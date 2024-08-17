package nobleidl.compiler

import nobleidl.compiler.format.{BackendMapping, BackendOptions, NobleIdlJarOptions, PackageMapping}
import esexpr.{ESExprCodec, Dictionary, ESExprBinaryEncoder}
import nobleidl.compiler.api.{java as _, *}
import scopt.{OEffect, OParser}
import zio.*
import zio.stream.*

import java.io.IOException
import java.nio.file.attribute.BasicFileAttributes
import java.nio.file.*
import java.util.Set as JSet
import scala.collection.mutable
import scala.jdk.CollectionConverters.*
import scala.util.Using

object ScalaNobleIDLCompiler extends ZIOAppDefault {

  private case class Config(
    generateScala: Boolean = false,
    generateScalaJS: Boolean = false,

    javaAdapters: Boolean = false,
    jsAdapters: Boolean = false,

    inputDirs: Seq[Path] = Seq(),
    outputDir: Option[String] = None,
    resourceOutputDir: Option[Path] = None,
    libraries: Seq[Path] = Seq(),
    packageMapping: Map[String, String] = Map(),
    javaPackageMapping: Map[String, String] = Map(),
    scalaJSPackageMapping: Map[String, String] = Map(),
  )

  override def run: ZIO[ZIOAppArgs & Scope, Any, Any] =
    runImpl
      .catchAll {
        case exitCode: ExitCode => ZIO.succeed(exitCode)
        case ex: Throwable => ZIO.fail(ex)
      }
      .flatMap(exit(_))

  private def runImpl: ZIO[ZIOAppArgs, ExitCode | Throwable, ExitCode] =
    for
      args <- ZIOAppArgs.getArgs

      parser = {
        val builder = OParser.builder[Config]
        import builder.*

        OParser.sequence(
          programName("noble-idl-compiler-scala"),

          opt[Unit]("scala")
            .action((_, c) => c.copy(generateScala = true)),
          opt[Unit]("scalajs")
            .action((_, c) => c.copy(generateScalaJS = true)),
          opt[Unit]("java-adapters")
            .action((_, c) => c.copy(javaAdapters = true)),
          opt[Unit]("js-adapters")
            .action((_, c) => c.copy(jsAdapters = true)),
          opt[Seq[Path]]('i', "input")
            .minOccurs(1)
            .unbounded()
            .action((x, c) => c.copy(inputDirs = x)),
          opt[String]('o', "output")
            .required()
            .action((x, c) => c.copy(outputDir = Some(x))),
          opt[Path]('r', "resource-output")
            .required()
            .action((x, c) => c.copy(resourceOutputDir = Some(x))),
          opt[Seq[Path]]('d', "library")
            .minOccurs(0)
            .unbounded()
            .action((x, c) => c.copy(libraries = c.libraries ++ x)),
          opt[Map[String, String]]('p', "package-mapping")
            .minOccurs(0)
            .unbounded()
            .action((x, c) => c.copy(packageMapping = c.packageMapping ++ x)),
          opt[Map[String, String]]("java-package-mapping")
            .minOccurs(0)
            .unbounded()
            .action((x, c) => c.copy(javaPackageMapping = c.javaPackageMapping ++ x)),
          opt[Map[String, String]]('p', "js-package-mapping")
            .minOccurs(0)
            .unbounded()
            .action((x, c) => c.copy(scalaJSPackageMapping = c.scalaJSPackageMapping ++ x)),
        )
      }

      (result, effects) = OParser.runParser(parser, args, Config())

      hasError <- Ref.make(false)

      _ <- ZIO.foreachDiscard(effects) {
        case OEffect.DisplayToOut(msg) => Console.printLine(msg).orDie
        case OEffect.DisplayToErr(msg) => Console.printLineError(msg).orDie
        case OEffect.ReportError(msg) => hasError.set(true) *> Console.printLineError("Error: " + msg).orDie
        case OEffect.ReportWarning(msg) => Console.printLineError("Warning: " + msg).orDie
        case OEffect.Terminate(exitState) => ZIO.fail(if exitState.isLeft then ExitCode.failure else ExitCode.success)
      }

      _ <- ZIO.fail(ExitCode.failure).whenZIODiscard(hasError.get)

      config = result.get
      resourceOutputDir = config.resourceOutputDir.get
      inputMap <- scanInputDirs(config.inputDirs, resourceOutputDir)

      backendMapping <- ZIO.foreach(platformRunners) { pr =>
        (
          for
            libRes <- LibraryAnalyzer.scan(config.libraries, pr.platform)
            _ <- pr.compile(ScalaIDLCompilerOptions(
              languageOptions = pr.languageOptions(config, libRes),
              inputFileData = inputMap.values.toSeq,
              libraryFileData = libRes.sourceFiles,
            ))
          yield Map(pr.platform -> pr.jarBackendOptions(config))

        )
          .when(pr.shouldEnable(config))
          .map(_.getOrElse(Map.empty))
      }

      jarOptions = NobleIdlJarOptions(
        idlFiles = inputMap.keys.toSeq,
        backends = BackendMapping(Dictionary(
          backendMapping.view.flatten.toMap
        )),
      )

      _ <- ZIO.attempt { Files.createDirectories(resourceOutputDir) }.refineToOrDie[IOException]
      _ <- ESExprBinaryEncoder.writeWithSymbolTable(summon[ESExprCodec[NobleIdlJarOptions]].encode(jarOptions))
        .run(ZSink.fromPath(resourceOutputDir.resolve("nobleidl-options.esxb").nn))


    yield ExitCode.success

  private def scanInputDirs(dirs: Seq[Path], resourceOutputDir: Path): IO[IOException, Map[String, String]] =
    ZIO.attempt {
      val inputFileMap = mutable.Map[String, String]()
      for dir <- dirs do
        val dirPath = dir.toAbsolutePath.nn
        Files.walkFileTree(dirPath, JSet.of(FileVisitOption.FOLLOW_LINKS), Integer.MAX_VALUE, new SimpleFileVisitor[Path]() {
          @throws[IOException]
          override def preVisitDirectory(dir: Path, attrs: BasicFileAttributes): FileVisitResult = {
            if (Files.isSymbolicLink(dir)) return FileVisitResult.SKIP_SUBTREE
            FileVisitResult.CONTINUE
          }

          @throws[IOException]
          override def visitFile(file: Path, attrs: BasicFileAttributes): FileVisitResult = {
            if (file.getFileName.toString.endsWith(".nidl")) {
              val content = Files.readString(file).nn
              val relPath = dirPath.relativize(file)
              val outFile = resourceOutputDir.resolve(relPath).nn
              val outDir = outFile.getParent
              Files.createDirectories(outDir)
              Files.copy(file, outFile, StandardCopyOption.REPLACE_EXISTING)

              inputFileMap.addOne(relPath.toString, content)
            }
            FileVisitResult.CONTINUE
          }
        })
      end for

      inputFileMap.toMap
    }.refineToOrDie[IOException]

  val platformRunners: Seq[PlatformRunner] = Seq(
    ScalaPlatformRunner,
    ScalaJSPlatformRunner,
  )

  trait PlatformRunner {
    val platform: String
    type LanguageOptions

    private[ScalaNobleIDLCompiler] def shouldEnable(config: Config): Boolean

    private[ScalaNobleIDLCompiler] def languageOptions(config: Config, libRes: LibraryAnalyzer.LibraryResults): LanguageOptions

    protected def createBackend(request: NobleIdlGenerationRequest[LanguageOptions]): ScalaBackendBase


    private[ScalaNobleIDLCompiler] def jarBackendOptions(config: Config): BackendOptions

    def compile(options: ScalaIDLCompilerOptions[LanguageOptions]): IO[IOException | NobleIDLCompileErrorException, NobleIdlGenerationResult] =
      val modelOptions = NobleIdlCompileModelOptions(
        libraryFiles = options.libraryFileData,
        files = options.inputFileData,
      )

      for
        result <- ZIO.scoped {
          NobleIDLCompiler.make
            .flatMap(_.loadModel(modelOptions))
        }

        model <-
          result match {
            case NobleIdlCompileModelResult.Success(model) => ZIO.succeed(model)
            case NobleIdlCompileModelResult.Failure(errors) => ZIO.fail(new NobleIDLCompileErrorException(errors.mkString("\n")))
          }

        backend = createBackend(
          NobleIdlGenerationRequest(
            languageOptions = options.languageOptions,
            model = model,
          ),
        )

        files <- backend.emit
          .mapZIOParUnordered(java.lang.Runtime.getRuntime.nn.availableProcessors()) { file =>
            ZIO.attempt {
              Files.createDirectories(file.path.getParent)
            }.refineToOrDie[IOException] *>
              file.content
                .via(ZPipeline.utf8Encode.orDie)
                .run[Any, IOException | NobleIDLCompileErrorException, Long](ZSink.fromPath(file.path).refineOrDie {
                  case ex: IOException => ex
                })
                .as(file.path.toString)
          }
          .runCollect

      yield NobleIdlGenerationResult(
        files
      )
    end compile

  }

  object ScalaPlatformRunner extends PlatformRunner {
    override val platform: String = "scala"
    override type LanguageOptions = ScalaLanguageOptions

    private[ScalaNobleIDLCompiler] override def shouldEnable(config: Config): Boolean =
      config.generateScala

    private[ScalaNobleIDLCompiler] override def languageOptions(config: Config, libRes: LibraryAnalyzer.LibraryResults): ScalaLanguageOptions =
      ScalaLanguageOptions(
        outputDir = config.outputDir.get,
        packageMapping = PackageMapping(Dictionary(
            libRes.scalaPackageMapping ++ config.packageMapping
        )),
        javaAdapters =
          if config.javaAdapters then
            Some(ScalaLanguageOptions.JavaAdapters(
              packageMapping = PackageMapping(Dictionary(
                libRes.javaPackageMapping ++ config.javaPackageMapping
              ))
            ))
          else
            None,
        jsAdapters =
          if config.jsAdapters then
            Some(ScalaLanguageOptions.JSAdapters(
              packageMapping = PackageMapping(Dictionary(
                libRes.scalaJSPackageMapping ++ config.scalaJSPackageMapping
              ))
            ))
          else
            None,
      )

    private[ScalaNobleIDLCompiler] override def jarBackendOptions(config: Config): BackendOptions =
      BackendOptions(
        packageMapping = PackageMapping(Dictionary(config.packageMapping)),
      )

    protected override def createBackend(request: NobleIdlGenerationRequest[ScalaLanguageOptions]): ScalaBackendBase =
      ScalaBackend(request)
  }

  object ScalaJSPlatformRunner extends PlatformRunner {
    override val platform: String = "scalajs"
    override type LanguageOptions = ScalaJSLanguageOptions

    private[ScalaNobleIDLCompiler] override def shouldEnable(config: Config): Boolean =
      config.generateScalaJS

    private[ScalaNobleIDLCompiler] override def languageOptions(config: Config, libRes: LibraryAnalyzer.LibraryResults): ScalaJSLanguageOptions =
      ScalaJSLanguageOptions(
        outputDir = config.outputDir.get,
        packageMapping = PackageMapping(
          mapping = Dictionary(
            (libRes.scalaJSPackageMapping ++ config.scalaJSPackageMapping)
          ),
        ),
      )

    private[ScalaNobleIDLCompiler] override def jarBackendOptions(config: Config): BackendOptions =
      BackendOptions(
        packageMapping = PackageMapping(Dictionary(config.scalaJSPackageMapping)),
      )

    protected override def createBackend(request: NobleIdlGenerationRequest[ScalaJSLanguageOptions]): ScalaBackendBase =
      ScalaJSBackend(request)
  }

}

package nobleidl.compiler

import nobleidl.compiler.format.{BackendMapping, BackendOptions, NobleIdlJarOptions, PackageMapping}
import esexpr.{ESExprCodec, Dictionary, ESExprBinaryEncoder}
import nobleidl.compiler.api.*
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

  def compile(options: ScalaIDLCompilerOptions): IO[IOException | NobleIDLCompileErrorException, NobleIdlGenerationResult] =
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

      backend = ScalaBackend(
        NobleIdlGenerationRequest[ScalaLanguageOptions](
          languageOptions = options.languageOptions,
          model = model,
        ),
      )

      files <- backend.emit
        .mapZIOParUnordered(java.lang.Runtime.getRuntime.nn.availableProcessors()) { file =>
          val fileName = file.path.foldLeft(Path.of(options.languageOptions.outputDir).nn)(_.resolve(_).nn)
          ZIO.attempt { Files.createDirectories(fileName.getParent) }.refineToOrDie[IOException] *>
            file.content
              .via(ZPipeline.utf8Encode.orDie)
              .run[Any, IOException | NobleIDLCompileErrorException, Long](ZSink.fromPath(fileName).refineOrDie {
                case ex: IOException => ex
              })
              .as(fileName.toString)
        }
        .runCollect
      
    yield NobleIdlGenerationResult(
      files
    )
  end compile

  private case class Config(
    inputDirs: Seq[Path] = Seq(),
    outputDir: Option[String] = None,
    resourceOutputDir: Option[Path] = None,
    libraries: Seq[Path] = Seq(),
    packageMapping: Map[String, String] = Map(),
  )

  override def run: ZIO[ZIOAppArgs & Scope, Any, Any] =
    ZIOAppArgs.getArgs.flatMap { args =>
      val parser =
        val builder = OParser.builder[Config]
        import builder.*
        
        OParser.sequence(
          programName("noble-idl-compiler-scala"),
          opt[Seq[Path]]('i', "input")
            .action((x, c) => c.copy(inputDirs = x)),
          opt[String]('o', "output")
            .required()
            .action((x, c) => c.copy(outputDir = Some(x))),
          opt[Path]('r', "resource-output")
            .required()
            .action((x, c) => c.copy(resourceOutputDir = Some(x))),
          opt[Seq[Path]]('d', "library")
            .action((x, c) => c.copy(libraries = x)),
          opt[Map[String, String]]('p', "package-mapping")
            .action((x, c) => c.copy(packageMapping = x)),
        )
      end parser
        
      val (result, effects) = OParser.runParser(parser, args, Config())
      
      
      ZIO.foreachDiscard(effects) {
        case OEffect.DisplayToOut(msg) => Console.printLine(msg).orDie
        case OEffect.DisplayToErr(msg) => Console.printLineError(msg).orDie
        case OEffect.ReportError(msg) => Console.printLineError("Error: " + msg).orDie
        case OEffect.ReportWarning(msg) => Console.printLineError("Warning: " + msg).orDie
        case OEffect.Terminate(exitState) => ZIO.fail(if exitState.isLeft then ExitCode.failure else ExitCode.success)
      }.foldZIO(
        failure = exitCode => ZIO.succeed(exitCode),
        success = _ => {
          val config = result.get

          val resourceOutputDir = config.resourceOutputDir.get

          for
            libRes <- LibraryAnalyzer.scan(config.libraries)
            inputMap <- scanInputDirs(config.inputDirs, resourceOutputDir)

            _ <- compile(ScalaIDLCompilerOptions(
              languageOptions = ScalaLanguageOptions(
                outputDir = config.outputDir.get,
                packageMapping = PackageMapping(
                  mapping = Dictionary(
                    (libRes.packageMapping ++ config.packageMapping)
                  ),
                ),
              ),
              inputFileData = inputMap.values.toSeq,
              libraryFileData = libRes.sourceFiles,
            ))

            jarOptions = NobleIdlJarOptions(
              idlFiles = inputMap.keys.toSeq,
              backends = BackendMapping(Dictionary(Map(
                "java" -> BackendOptions(
                  packageMapping = PackageMapping(
                    Dictionary(config.packageMapping),
                  )
                ),
              ))),
            )
            
            _ <- ZIO.attempt { Files.createDirectories(resourceOutputDir) }.refineToOrDie[IOException]
            _ <- ESExprBinaryEncoder.writeWithSymbolTable(summon[ESExprCodec[NobleIdlJarOptions]].encode(jarOptions))
              .run(ZSink.fromPath(resourceOutputDir.resolve("nobleidl-options.esxb").nn))

          yield ExitCode.success
        }
          
      )
      
    }.flatMap(exit)

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
              Files.copy(file, outFile)

              inputFileMap.addOne(relPath.toString, content)
            }
            FileVisitResult.CONTINUE
          }
        })
      end for

      inputFileMap.toMap
    }.refineToOrDie[IOException]
}

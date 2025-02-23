package nobleidl.compiler

import dev.argon.nobleidl.compiler.{JavaLanguageOptions, NobleIDLCompileErrorException}
import nobleidl.compiler.backend.{PackageMapping, Backend, ScalaJSLanguageOptions}
import esexpr.{Dictionary, ESExprBinaryEncoder, ESExprCodec}
import api.{NobleIdlCompileModelOptions, NobleIdlCompileModelResult, NobleIdlGenerationRequest, NobleIdlGenerationResult, java as _, *}
import scopt.{OEffect, OParser}
import zio.*
import zio.stream.*

import javax.annotation.processing.Processor
import javax.tools.*
import java.io.IOException
import java.nio.file.attribute.BasicFileAttributes
import java.nio.file.*
import java.nio.charset.StandardCharsets
import java.util.Set as JSet
import scala.collection.mutable
import scala.jdk.CollectionConverters.*
import scala.util.Using

object ScalaNobleIDLCompiler extends ZIOAppDefault {

  private[compiler] case class Config(
    generateScala: Boolean = false,
    generateJava: Boolean = false,
    generateScalaJS: Boolean = false,

    javaAdapters: Boolean = false,
    jsAdapters: Boolean = false,
    graaljsAdapters: Boolean = false,

    inputDirs: Seq[Path] = Seq(),
    javaSourceDirs: Seq[Path] = Seq(),
    outputDir: Option[Path] = None,
    resourceOutputDir: Option[Path] = None,
    libraries: Seq[Path] = Seq(),
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
          opt[Unit]("java")
            .action((_, c) => c.copy(generateJava = true)),
          opt[Unit]("scalajs")
            .action((_, c) => c.copy(generateScalaJS = true)),
          opt[Unit]("java-adapters")
            .action((_, c) => c.copy(javaAdapters = true)),
          opt[Unit]("js-adapters")
            .action((_, c) => c.copy(jsAdapters = true)),
          opt[Unit]("graal-js-adapters")
            .action((_, c) => c.copy(graaljsAdapters = true)),
          opt[Seq[Path]]('i', "input")
            .minOccurs(1)
            .unbounded()
            .action((x, c) => c.copy(inputDirs = c.inputDirs ++ x)),
          opt[Seq[Path]]('j', "java-source")
            .minOccurs(1)
            .unbounded()
            .action((x, c) => c.copy(javaSourceDirs = c.javaSourceDirs ++ x)),
          opt[Path]('o', "output")
            .required()
            .action((x, c) => c.copy(outputDir = Some(x))),
          opt[Path]('r', "resource-output")
            .required()
            .action((x, c) => c.copy(resourceOutputDir = Some(x))),
          opt[Seq[Path]]('d', "library")
            .minOccurs(0)
            .unbounded()
            .action((x, c) => c.copy(libraries = c.libraries ++ x)),
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

      + <- ZIO.foreach(platformRunners) { pr =>
        (
          for
            libRes <- LibraryAnalyzer.scan(config.libraries, pr.platform)
            currentLibRes <- getCurrentLibraryResult(config)
            _ <- pr.compile(ScalaIDLCompilerOptions(
              outputDir = config.outputDir.get,
              languageOptions = pr.languageOptions(config, libRes, currentLibRes),
              inputFileData = currentLibRes.sourceFiles,
              libraryFileData = libRes.sourceFiles,
            ))
          yield ()

        )
          .whenDiscard(pr.shouldEnable(config))
      }

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
              val outFile = resourceOutputDir.resolve("nobleidl").resolve(relPath).nn
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

  private def getCurrentLibraryResult(config: Config): IO[IOException, LibraryAnalyzer.LibraryResults] =
    for
      inputMap <- scanInputDirs(config.inputDirs, config.resourceOutputDir.get)
      processor <- ZIO.succeed(PackageMappingScannerProcessor())

      _ <- ZIO.attempt {
        runAnnotationProcessor(config)(processor)
      }.refineToOrDie[IOException]

      res <- ZIO.succeed(processor.getLibraryResults)
    yield res.copy(sourceFiles = inputMap.values.toSeq)


  private def runAnnotationProcessor(config: Config)(processor: Processor): Unit =
    val compiler = ToolProvider.getSystemJavaCompiler

    val diagnostics = new DiagnosticCollector[JavaFileObject]

    Using.resource(compiler.getStandardFileManager(diagnostics, null, StandardCharsets.UTF_8)) { fileManager =>
      val sourceFiles =
        config.javaSourceDirs
          .view
          .flatMap(dir => Files.walk(dir).iterator().asScala)
          .filter(Files.isRegularFile(_))
          .filter { p =>
            val fileName = p.getFileName
            fileName != null && fileName.toString.endsWith(".java")
          }
          .toArray

      if sourceFiles.isEmpty then
        throw new RuntimeException("No java source files were found")

      val compilationUnits = fileManager.getJavaFileObjects(sourceFiles*)

      val args = java.util.ArrayList[String]()
      args.add("-proc:only")

      val classpath = config.libraries.mkString(java.io.File.pathSeparator)

      if sourceFiles.exists(_.getFileName.toString == "module-info.java") then
        args.add("--module-path")
      else
        args.add("-cp")

      args.add(classpath)

      val task = compiler.getTask(
        java.io.Writer.nullWriter,
        fileManager,
        diagnostics,
        args,
        null,
        compilationUnits
      )
      task.setProcessors(Seq(processor).asJava)

      task.call() // Ignore errors, sometimes errors are due to the generated files not existing yet.
    }
  end runAnnotationProcessor


  val platformRunners: Seq[PlatformRunner] = Seq(
    ScalaPlatformRunner,
    ScalaJSPlatformRunner,
    JavaPlatformRunner,
  )










}

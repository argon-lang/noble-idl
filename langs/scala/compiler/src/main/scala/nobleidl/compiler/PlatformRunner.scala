package nobleidl.compiler

import dev.argon.nobleidl.compiler.NobleIDLCompileErrorException
import nobleidl.compiler.ScalaNobleIDLCompiler.Config
import nobleidl.compiler.api.{java as _, *}
import nobleidl.compiler.backend.Backend
import zio.{IO, ZIO}
import zio.stream.{ZPipeline, ZSink}

import java.io.IOException
import java.nio.file.Files


trait PlatformRunner {
  val platform: String
  type LanguageOptions

  private[compiler] def shouldEnable(config: Config): Boolean

  private[compiler] def languageOptions(config: Config, libRes: LibraryAnalyzer.LibraryResults, currentLibRes: LibraryAnalyzer.LibraryResults): LanguageOptions

  protected def createBackend(request: NobleIdlGenerationRequest[LanguageOptions]): Backend

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
        .mapZIOParUnordered(java.lang.Runtime.getRuntime.availableProcessors()) { file =>
          val filePath = options.outputDir.resolve(file.path)

          ZIO.attempt {
            Files.createDirectories(filePath.getParent)
          }.refineToOrDie[IOException] *>
            file.content
              .via(ZPipeline.utf8Encode.orDie)
              .run[Any, IOException | NobleIDLCompileErrorException, Long](ZSink.fromPath(filePath).refineOrDie {
                case ex: IOException => ex
              })
              .as(filePath.toString)
        }
        .runCollect

    yield NobleIdlGenerationResult(
      files
    )
  end compile

}

package nobleidl.compiler

import cats.Monoid
import cats.data.OptionT
import nobleidl.compiler.format.NobleIdlJarOptions
import dev.argon.util.async.ErrorWrapper
import esexpr.{ESExpr, ESExprCodec, ESExprBinaryDecoder, ESExprFormatException}
import zio.*
import zio.interop.catz.core.given
import zio.stream.*

import java.io.IOException
import java.nio.file.{FileSystems, Files, Path}
import scala.jdk.CollectionConverters.*
import scala.reflect.TypeTest

object LibraryAnalyzer {

  type Error = IOException | ESExprFormatException | ESExprCodec.DecodeError

  final case class LibraryResults(
    scalaPackageMapping: Map[String, String],
    javaPackageMapping: Map[String, String],
    scalaJSPackageMapping: Map[String, String],
    sourceFiles: Seq[String],
  )

  def scan(libraries: Seq[Path], platform: String): IO[Error, LibraryResults] =
    ZStream.fromIterable(libraries)
      .flatMap { lib =>
        ZStream.scoped(libFileSystemRoot(lib))
      }
      .mapZIO(scanLibrary(platform))
      .collectSome
      .runFold(
        LibraryResults(
          scalaPackageMapping = Map.empty,
          javaPackageMapping = Map.empty,
          scalaJSPackageMapping = Map.empty,
          sourceFiles = Seq.empty,
        )
      ) { (a, b) =>
        LibraryResults(
          scalaPackageMapping = a.scalaPackageMapping ++ b.scalaPackageMapping,
          javaPackageMapping = a.javaPackageMapping ++ b.javaPackageMapping,
          scalaJSPackageMapping = a.scalaJSPackageMapping ++ b.scalaJSPackageMapping,
          sourceFiles = a.sourceFiles ++ b.sourceFiles
        )
    }

  private class WrappedError(val cause: Cause[Error]) extends Exception

  private given ErrorWrapper[Error] with {
    override type EX = WrappedError

    override def exceptionTypeTest: TypeTest[Throwable, WrappedError] = summon

    override def wrap(error: Cause[Error]): WrappedError = WrappedError(error)
    override def unwrap(ex: WrappedError): Cause[Error] = ex.cause
  }

  private def scanLibrary(platform: String)(lib: Path): IO[Error, Option[LibraryResults]] =
    val optionsEsxFile = lib.resolve("nobleidl-options.esxb").nn

    OptionT(
      ESExprBinaryDecoder.readEmbeddedStringTable[Any, Error](
        ZStream.fromPath(optionsEsxFile).refineToOrDie[IOException]
      ).runHead
    )
      .flatMap { optionsExpr =>
        OptionT.liftF(ZIO.fromEither(
          summon[ESExprCodec[NobleIdlJarOptions]].decode(optionsExpr)
        ))
      }
      .filter(_.backends.mapping.dict.contains(platform))
      .flatMap { options =>
        OptionT.liftF[[A] =>> IO[Error, A], Seq[String]](ZIO.foreach(options.idlFiles) { idlFile =>
          val idlFilePath = lib.resolve(idlFile).nn
          ZIO.readFile(idlFilePath)
        })
          .map { fileData =>
            def getPackageMapping(platform: String): Map[String, String] =
              options.backends.mapping.dict
                .view
                .mapValues(_.packageMapping.mapping.dict)
                .getOrElse(platform, Map.empty)
            
            LibraryResults(
              scalaPackageMapping = getPackageMapping("scala"),
              javaPackageMapping = getPackageMapping("java"),
              scalaJSPackageMapping = getPackageMapping("scalajs"),

              sourceFiles = fileData,
            )
          }

      }
      .value
      .whenZIO(ZIO.attempt { Files.exists(optionsEsxFile) }.refineToOrDie[IOException])
      .map(_.flatten)
  end scanLibrary

  private def libFileSystemRoot(lib: Path): ZIO[Scope, IOException, Path] =
    ZIO.ifZIO(ZIO.attempt { Files.isDirectory(lib) })(
      onTrue = ZIO.succeed(lib),
      onFalse =
        if lib.getFileName.toString.endsWith(".jar") then
          ZIO.fromAutoCloseable(
            ZIO.attempt {
              FileSystems.newFileSystem(lib).nn
            }
          )
            .flatMap { zipFS => ZIO.attempt { zipFS.getRootDirectories.nn.iterator.nn.next.nn } }
        else
          ZIO.die(RuntimeException("Unknown library type. Expected directory or jar."))
    ).refineToOrDie[IOException]
}

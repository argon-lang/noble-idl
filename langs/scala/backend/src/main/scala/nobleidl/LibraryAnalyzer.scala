package nobleidl

import cats.Monoid
import cats.data.OptionT
import dev.argon.nobleidl.compiler.format.NobleIdlJarOptions
import dev.argon.util.async.ErrorWrapper
import esexpr.{ESExpr, ESExprBinaryDecoder, ESExprFormatException}
import zio.*
import zio.stream.*
import zio.interop.catz.core.given

import java.io.IOException
import java.nio.file.{FileSystems, Files, Path}
import scala.reflect.TypeTest
import scala.jdk.CollectionConverters.*

object LibraryAnalyzer {

  type Error = IOException | ESExprFormatException | dev.argon.esexpr.DecodeException

  final case class LibraryResults(
    packageMapping: Map[String, String],
    sourceFiles: Seq[String],
  )

  def scan(libraries: Seq[Path]): IO[Error, LibraryResults] =
    ZStream.fromIterable(libraries)
      .flatMap { lib =>
        ZStream.scoped(libFileSystemRoot(lib))
      }
      .mapZIO(scanLibrary)
      .collectSome
      .runFold(
        LibraryResults(
          packageMapping = Map.empty,
          sourceFiles = Seq.empty,
        )
      ) { (a, b) =>
        LibraryResults(
          packageMapping = a.packageMapping ++ b.packageMapping,
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

  private def scanLibrary(lib: Path): IO[Error, Option[LibraryResults]] =
    OptionT(
      ESExprBinaryDecoder.readEmbeddedStringTable[Any, Error](
        ZStream.fromPath(lib).refineToOrDie[IOException]
      ).runHead
    )
      .flatMap { optionsExpr =>
        OptionT.liftF(ZIO.attempt {
          NobleIdlJarOptions.codec().nn.decode(ESExpr.toJava(optionsExpr))
        }.refineToOrDie[dev.argon.esexpr.DecodeException])
      }
      .filter(_.backends.nn.mapping.nn.map.nn.containsKey("scala"))
      .map { options =>
        LibraryResults(
          packageMapping = options.nn.backends.nn.mapping.nn.map.nn.asScala
            .view
            .mapValues(_.packageMapping.nn.map.nn.asScala.toMap)
            .getOrElse("scala", Map.empty),
          
          sourceFiles = options.idlFiles.nn.asScala.toSeq,
        )
      }
      .value

  private def libFileSystemRoot(lib: Path): ZIO[Scope, IOException, Path] =
    ZIO.ifZIO(ZIO.attempt { Files.isDirectory(lib) })(
      onTrue = ZIO.succeed(lib),
      onFalse =
        if lib.getFileName.toString.endsWith(".jar") then
          ZIO.fromAutoCloseable(
            ZIO.attempt { FileSystems.newFileSystem(lib).nn }
          )
            .flatMap { zipFS => ZIO.attempt { zipFS.getRootDirectories.nn.iterator.nn.next.nn } }
        else
          ZIO.die(RuntimeException("Unknown library type. Expected directory or jar."))
    ).refineToOrDie[IOException]
}

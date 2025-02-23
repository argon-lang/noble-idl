package nobleidl.compiler.backend

import dev.argon.nobleidl.compiler.NobleIDLCompileErrorException
import dev.argon.nobleidl.compiler.Backend.FileGenerator
import dev.argon.util.async.ZStreamFromWriterCallbackZIO
import zio.*
import zio.stream.*

private[compiler] class WrappedJavaBackend(backend: dev.argon.nobleidl.compiler.Backend) extends Backend {
  import Backend.*

  override def emit: Stream[NobleIDLCompileErrorException, GeneratedFile] =
    ZStream.fromJavaStream(backend.emit())
      .refineToOrDie[NobleIDLCompileErrorException]
      .mapZIO { generator =>
        for
          path <- ZIO.attempt { generator.getPath }.refineToOrDie[NobleIDLCompileErrorException]
        yield GeneratedFile(path, generateStream(generator))
      }

  private def generateStream(generator: FileGenerator): Stream[NobleIDLCompileErrorException, String] =
    ZStreamFromWriterCallbackZIO { w =>
      ZIO.attempt { generator.generate(w) }
        .refineToOrDie[NobleIDLCompileErrorException]
    }



}

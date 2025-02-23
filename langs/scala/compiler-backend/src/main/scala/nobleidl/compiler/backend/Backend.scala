package nobleidl.compiler.backend

import zio.stream.*
import java.nio.file.Path

import dev.argon.nobleidl.compiler.NobleIDLCompileErrorException
import nobleidl.compiler.backend.Backend.GeneratedFile

trait Backend {
  def emit: Stream[NobleIDLCompileErrorException, GeneratedFile]
}

object Backend {
  final case class GeneratedFile(path: Path, content: Stream[NobleIDLCompileErrorException, String])
}

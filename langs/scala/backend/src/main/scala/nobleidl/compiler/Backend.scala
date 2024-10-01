package nobleidl.compiler

import zio.stream.*
import java.nio.file.Path

import dev.argon.nobleidl.compiler.NobleIDLCompileErrorException
import nobleidl.compiler.Backend.GeneratedFile

trait Backend {
  def emit: Stream[NobleIDLCompileErrorException, GeneratedFile]
}

object Backend {
  final case class GeneratedFile(path: Path, content: Stream[NobleIDLCompileErrorException, String])
}

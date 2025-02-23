package nobleidl.compiler

import dev.argon.jawawasm.engine.*
import dev.argon.jawawasm.format.binary.ModuleReader
import esexpr.{ESExpr, ESExprBinaryDecoder, ESExprBinaryEncoder, ESExprCodec}
import api.{NobleIdlCompileModelOptions, NobleIdlCompileModelResult}
import zio.stream.ZStream
import zio.*
import dev.argon.nobleidl.compiler.NobleIDLCompileErrorException

final class NobleIDLCompiler private(impl: dev.argon.nobleidl.compiler.NobleIDLCompiler) {
  def loadModel(options: NobleIdlCompileModelOptions): UIO[NobleIdlCompileModelResult] =
    ZIO.succeed {
      val javaOptions = NobleIdlCompileModelOptions.javaAdapter().toJava(options)
      val modelResult = impl.loadModel(javaOptions)
      NobleIdlCompileModelResult.javaAdapter().fromJava(modelResult)
    }
}

object NobleIDLCompiler {

  def make: ZIO[Scope, Nothing, NobleIDLCompiler] =
    for
      impl <- ZIO.fromAutoCloseable(ZIO.succeed { dev.argon.nobleidl.compiler.NobleIDLCompiler() })
    yield NobleIDLCompiler(impl)

}

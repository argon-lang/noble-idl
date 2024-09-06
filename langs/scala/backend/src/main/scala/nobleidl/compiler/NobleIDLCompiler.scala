package nobleidl.compiler

import dev.argon.jawawasm.engine.*
import dev.argon.jawawasm.format.binary.ModuleReader
import esexpr.{ESExpr, ESExprBinaryDecoder, ESExprBinaryEncoder, ESExprCodec}
import nobleidl.compiler.api.{NobleIdlCompileModelOptions, NobleIdlCompileModelResult}
import zio.stream.ZStream
import zio.*


final class NobleIDLCompiler(module: WasmModule) {
  import NobleIDLCompiler.Buffer


  private def alloc(size: Int): Int =
    val results = FunctionResult.resolveWith(() => module.getExport("nobleidl_alloc").asInstanceOf[WasmFunction].invoke(Array(size))).nn
    results(0).asInstanceOf[Int]
  end alloc

  private def free(b: Int, size: Int): Unit =
    FunctionResult.resolveWith(() => module.getExport("nobleidl_free").asInstanceOf[WasmFunction].invoke(Array(b, size)))

  private def freeBuffer(b: Buffer): UIO[Unit] =
    ZIO.succeed {
      free(b.ptr, b.size)
    }

  private def compileModule(options: Int, optionsSize: Int): UIO[Buffer] =
    ZIO.succeed {
      val resultSizePtr = alloc(4);
      try {
        val results = FunctionResult.resolveWith(() => module.getExport("nobleidl_compile_model").asInstanceOf[WasmFunction].invoke(Array(options, optionsSize, resultSizePtr))).nn
        val resultSize = memory.loadI32(resultSizePtr)
        Buffer(results(0).asInstanceOf[Int], resultSize)
      }
      finally free(resultSizePtr, 4)
    }

  private def memory = module.getExport("memory").asInstanceOf[WasmMemory]


  private def exprToBuffer(expr: ESExpr): UIO[Buffer] =
    ESExprBinaryEncoder.writeWithSymbolTable(expr)
      .runCollect
      .flatMap { data =>
        ZIO.succeed {
          val buffer = alloc(data.size)
          memory.copyFromArray(buffer, 0, data.size, data.toArray)
          Buffer(buffer, data.size)
        }
      }

  private def bufferToExpr(buffer: NobleIDLCompiler.Buffer): UIO[ESExpr] =
    ZIO.succeed {
      val data = new Array[Byte](buffer.size)
      memory.copyToArray(buffer.ptr, 0, data.length, data)
      data
    }
      .flatMap { data => ESExprBinaryDecoder.readEmbeddedStringTable(ZStream.fromChunk(Chunk.fromArray(data))).orDie.runHead }
      .flatMap {
        case Some(value) => ZIO.succeed(value)
        case None => ZIO.die(new NobleIDLCompileErrorException("Expected a single expression"))
      }


  def loadModel(options: NobleIdlCompileModelOptions): UIO[NobleIdlCompileModelResult] =
    ZIO.scoped {
      for
        optionsBuffer <- exprToBuffer(summon[ESExprCodec[NobleIdlCompileModelOptions]].encode(options)).withFinalizer(freeBuffer)
        resultBuffer <- compileModule(optionsBuffer.ptr, optionsBuffer.size).withFinalizer(freeBuffer)

        expr <- bufferToExpr(resultBuffer)
      yield expr
    }
      .flatMap { expr =>
        ZIO.fromEither(summon[ESExprCodec[NobleIdlCompileModelResult]].decode(expr))
      }
      .catchAll { error => ZIO.die(error) }
}

object NobleIDLCompiler {

  private final case class Buffer(ptr: Int, size: Int)


  def make: ZIO[Scope, Nothing, NobleIDLCompiler] =
    for
      engine <- ZIO.fromAutoCloseable(ZIO.succeed { Engine() })
      module <- ZIO.scoped {
        ZIO.fromAutoCloseable(ZIO.succeed { classOf[NobleIDLCompiler].getResourceAsStream("noble-idl-compiler.wasm").nn })
          .flatMap { wasmStream =>
            ZIO.succeed { new ModuleReader(wasmStream).readModule.nn }
          }
      }
      instModule <- ZIO.succeed { engine.instantiateModule(module, EmptyResolver()).nn }
    yield NobleIDLCompiler(instModule)


  private class EmptyResolver extends ModuleResolver {
    override def resolve(name: String): WasmModule =
      throw new ModuleResolutionException()
  }




}

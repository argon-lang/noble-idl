package nobleidl.compiler

import dev.argon.jvmwasm.engine.*
import dev.argon.jvmwasm.format.binary.ModuleReader
import esexpr.{ESExpr, ESExprBinaryDecoder, ESExprBinaryEncoder, ESExprCodec}
import nobleidl.compiler.api.{NobleIdlCompileModelOptions, NobleIdlCompileModelResult}
import zio.stream.ZStream
import zio.*

import java.io.{ByteArrayInputStream, ByteArrayOutputStream, IOException}
import java.util.concurrent.ExecutionException
import scala.util.Using

final class NobleIDLCompiler(engine: Engine, module: WasmModule) {
  import NobleIDLCompiler.Buffer


  private def alloc(size: Int): Buffer =
    val results = FunctionResult.resolveWith(() => module.getExport("nobleidl_alloc").asInstanceOf[WasmFunction].invoke(Array(size))).nn
    Buffer(results(0).asInstanceOf[Int], results(1).asInstanceOf[Int])
  end alloc

  private def free(b: Buffer): UIO[Unit] =
    ZIO.succeed {
      FunctionResult.resolveWith(() => module.getExport("nobleidl_free").asInstanceOf[WasmFunction].invoke(Array(b.size, b.data)))
    }

  private def compileModule(options: Buffer): UIO[Buffer] =
    ZIO.succeed {
      val results = FunctionResult.resolveWith(() => module.getExport("nobleidl_compile_model").asInstanceOf[WasmFunction].invoke(Array(options.size, options.data))).nn
      Buffer(results(0).asInstanceOf[Int], results(1).asInstanceOf[Int])
    }

  private def memory = module.getExport("memory").asInstanceOf[WasmMemory]


  private def exprToBuffer(expr: ESExpr): UIO[Buffer] =
    ESExprBinaryEncoder.writeWithSymbolTable(expr)
      .runCollect
      .flatMap { data =>
        ZIO.succeed {
          val buffer = alloc(data.size)
          memory.copyFromArray(buffer.data, 0, data.size, data.toArray)
          buffer
        }
      }

  private def bufferToExpr(buffer: NobleIDLCompiler.Buffer): UIO[ESExpr] =
    ZIO.succeed {
      val data = new Array[Byte](buffer.size)
      memory.copyToArray(buffer.data, 0, data.length, data)
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
        optionsBuffer <- exprToBuffer(summon[ESExprCodec[NobleIdlCompileModelOptions]].encode(options)).withFinalizer(free)
        resultBuffer <- compileModule(optionsBuffer).withFinalizer(free)

        expr <- bufferToExpr(resultBuffer)
      yield expr
    }
      .flatMap { expr =>
        ZIO.fromEither(summon[ESExprCodec[NobleIdlCompileModelResult]].decode(expr))
      }
      .catchAll { error => ZIO.die(error) }
}

object NobleIDLCompiler {

  private final case class Buffer(size: Int, data: Int)


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
    yield NobleIDLCompiler(engine, instModule)


  private class EmptyResolver extends ModuleResolver {
    override def resolve(name: String): WasmModule =
      throw new ModuleResolutionException()
  }




}

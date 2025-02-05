package dev.argon.nobleidl.compiler;

import dev.argon.esexpr.*;
import dev.argon.jawawasm.engine.*;
import dev.argon.jawawasm.format.ModuleFormatException;
import dev.argon.jawawasm.format.binary.ModuleReader;
import dev.argon.nobleidl.compiler.api.*;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;
import java.util.concurrent.ExecutionException;

public class NobleIDLCompiler implements AutoCloseable {

	public NobleIDLCompiler() {
		module = loadModule(engine);
	}

	private final Engine engine = new Engine();
	private final WasmModule module;

	private static WasmModule loadModule(Engine engine) {
		try {
			try(var wasmStream = NobleIDLCompiler.class.getResourceAsStream("noble-idl-compiler.wasm")) {
				var module = (new ModuleReader(wasmStream)).readModule();
				return engine.instantiateModule(module, new EmptyResolver());
			}
		}
		catch(IOException | ModuleFormatException | ExecutionException | ModuleLinkException ex) {
			throw new RuntimeException(ex);
		}
	}

	private final static class EmptyResolver implements ModuleResolver {
		@Override
		public WasmModule resolve(String s) throws ModuleResolutionException {
			throw new ModuleResolutionException();
		}
	}

	@Override
	public void close() {
		engine.close();
	}

	private record Buffer(int ptr, int size) {}

	private int alloc(int size) {
		try {
			Object[] results = FunctionResult.resolveWith(() -> ((WasmFunction)module.getExport("nobleidl_alloc")).invoke(new Object[] { size }));
			return (int)results[0];
		}
		catch(ExecutionException ex) {
			throw new RuntimeException(ex);
		}
	}

	private void free(int b, int size) {
		try {
			FunctionResult.resolveWith(() -> ((WasmFunction)module.getExport("nobleidl_free")).invoke(new Object[] { b, size }));
		}
		catch(ExecutionException ex) {
			throw new RuntimeException(ex);
		}
	}

	private Buffer compileModel(int options, int options_size) {
		try {
			int resultSizePtr = alloc(4);
			try {
				Object[] results = FunctionResult.resolveWith(() -> ((WasmFunction)module.getExport("nobleidl_compile_model")).invoke(new Object[] { options, options_size, resultSizePtr }));
				int size = getMemory().loadI32(resultSizePtr);
				return new Buffer((int)results[0], size);
			}
			finally {
				free(resultSizePtr, 4);
			}
		}
		catch(ExecutionException ex) {
			throw new RuntimeException(ex);
		}
	}

	private WasmMemory getMemory() {
		return (WasmMemory)module.getExport("memory");
	}

	private Buffer exprToBuffer(ESExpr expr) throws NobleIDLCompileErrorException {
		var os = new ByteArrayOutputStream();
		try {
			var writer = new ESExprBinaryWriter(os);
			writer.write(expr);
		}
		catch(IOException ex) {
			throw new NobleIDLCompileErrorException(ex);
		}

		var data = os.toByteArray();

		var buffer = alloc(data.length);
		var memory = getMemory();
		memory.copyFromArray(buffer, 0, data.length, data);

		return new Buffer(buffer, data.length);
	}

	private ESExpr bufferToExpr(Buffer buffer) throws NobleIDLCompileErrorException {
		var data = new byte[buffer.size()];

		var memory = getMemory();
		memory.copyToArray(buffer.ptr(), 0, data.length, data);

		var is = new ByteArrayInputStream(data);
		List<ESExpr> exprs;
		try {
			var reader = new ESExprBinaryReader(is);
			exprs = reader.readAll().toList();
		}
		catch(RuntimeException ex) {
			if(ex.getCause() instanceof SyntaxException ex2)
				throw new NobleIDLCompileErrorException(ex2);
			else if(ex.getCause() instanceof IOException ex2)
				throw new NobleIDLCompileErrorException(ex2);
			else {
				throw ex;
			}
		}

		if(exprs.size() != 1) {
			throw new NobleIDLCompileErrorException("Expected a single expression");
		}

		return exprs.getFirst();
	}

	public NobleIdlCompileModelResult loadModel(NobleIdlCompileModelOptions options) throws NobleIDLCompileErrorException {
		var optionsBuffer = exprToBuffer(NobleIdlCompileModelOptions.codec().encode(options));
		try {
			var resultBuffer = compileModel(optionsBuffer.ptr(), optionsBuffer.size());
			try {
				var expr = bufferToExpr(resultBuffer);
				try {
					return NobleIdlCompileModelResult.codec().decode(expr);
				}
				catch(DecodeException ex) {
					throw new NobleIDLCompileErrorException("Error decoding compiler result", ex);
				}
			}
			finally {
				free(resultBuffer.ptr(), resultBuffer.size());
			}
		}
		finally {
			free(optionsBuffer.ptr(), optionsBuffer.size());
		}
	}

}

package dev.argon.nobleidl.compiler;

import dev.argon.esexpr.*;
import dev.argon.jawawasm.engine.*;
import dev.argon.jawawasm.format.ModuleFormatException;
import dev.argon.jawawasm.format.binary.ModuleReader;
import dev.argon.jawawasm.format.modules.Data;
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

	private record Buffer(int size, int data) {}

	private Buffer alloc(int size) {
		try {
			Object[] results = FunctionResult.resolveWith(() -> ((WasmFunction)module.getExport("nobleidl_alloc")).invoke(new Object[] { size }));
			return new Buffer((int)results[0], (int)results[1]);
		}
		catch(ExecutionException ex) {
			throw new RuntimeException(ex);
		}
	}

	private void free(Buffer b) {
		try {
			FunctionResult.resolveWith(() -> ((WasmFunction)module.getExport("nobleidl_free")).invoke(new Object[] { b.size(), b.data() }));
		}
		catch(ExecutionException ex) {
			throw new RuntimeException(ex);
		}
	}

	private Buffer compileModule(Buffer options) {
		try {
			Object[] results = FunctionResult.resolveWith(() -> ((WasmFunction)module.getExport("nobleidl_compile_model")).invoke(new Object[] { options.size(), options.data() }));
			return new Buffer((int)results[0], (int)results[1]);
		}
		catch(ExecutionException ex) {
			throw new RuntimeException(ex);
		}
	}

	private WasmMemory getMemory() {
		return (WasmMemory)module.getExport("memory");
	}

	private Buffer exprToBuffer(ESExpr expr) throws NobleIDLCompileErrorException {
		var stringTable = ESExprBinaryWriter.buildSymbolTable(expr);

		var os = new ByteArrayOutputStream();
		try {
			(new ESExprBinaryWriter(List.of(), os)).write(StringTable.codec().encode(stringTable));
			(new ESExprBinaryWriter(stringTable.values(), os)).write(expr);
		}
		catch(IOException ex) {
			throw new NobleIDLCompileErrorException(ex);
		}

		var data = os.toByteArray();

		var memory = getMemory();

		var buffer = alloc(data.length);
		memory.copyFromArray(buffer.data(), 0, data.length, data);

		return buffer;
	}

	private ESExpr bufferToExpr(Buffer buffer) throws NobleIDLCompileErrorException {
		var data = new byte[buffer.size()];

		var memory = getMemory();
		memory.copyToArray(buffer.data(), 0, data.length, data);

		var is = new ByteArrayInputStream(data);
		List<ESExpr> exprs;
		try {
			exprs = ESExprBinaryReader.readEmbeddedStringTable(is).toList();
		}
		catch(IOException | SyntaxException ex) {
			throw new NobleIDLCompileErrorException(ex);
		}

		if(exprs.size() != 1) {
			throw new NobleIDLCompileErrorException("Expected a single expression");
		}

		return exprs.getFirst();
	}

	public NobleIdlCompileModelResult loadModel(NobleIdlCompileModelOptions options) throws NobleIDLCompileErrorException {
		var optionsBuffer = exprToBuffer(NobleIdlCompileModelOptions.codec().encode(options));
		try {
			var resultBuffer = compileModule(optionsBuffer);
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
				free(resultBuffer);
			}
		}
		finally {
			free(optionsBuffer);
		}
	}

}

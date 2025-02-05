using System.Collections.Immutable;
using System.Runtime.InteropServices;
using ESExpr.Runtime;
using NobleIDL.Backend.Api;

namespace NobleIDL.Backend;

public sealed class NobleIDLCompiler : IDisposable {

    public NobleIDLCompiler() {
        instance = instanceCreator(new WebAssembly.Runtime.ImportDictionary());
    }
    
    private readonly WebAssembly.Instance<WasmExports> instance;

    private static WebAssembly.Runtime.InstanceCreator<WasmExports> instanceCreator = CompileModel();

    private static WebAssembly.Runtime.InstanceCreator<WasmExports> CompileModel() {
        using var stream = typeof(NobleIDLCompiler).Assembly.GetManifestResourceStream(
            "NobleIDL.Backend.Wasm.noble_idl_compiler.wasm"
        );

        if(stream is null) {
            throw new Exception("Could not load Wasm module");
        }

        return WebAssembly.Runtime.Compile.FromBinary<WasmExports>(stream);
    }
    
    public abstract class WasmExports {
        public abstract WebAssembly.Runtime.UnmanagedMemory memory { get; }
        public abstract int nobleidl_alloc(int size);
        public abstract void nobleidl_free(int ptr, int size);
        public abstract int nobleidl_compile_model(int options, int options_size, int result_size);
    }

    private int Alloc(int size) =>
        instance.Exports.nobleidl_alloc(size);

    private void Free(int b, int size) =>
        instance.Exports.nobleidl_free(b, size);

    private int CompileModel(int options, int optionsSize, out int resultSize) {
        int resultSizePtr = Alloc(4);
        try {
            int result = instance.Exports.nobleidl_compile_model(options, optionsSize, resultSizePtr);

            if(result < 0) {
                throw new Exception("Result data too large");
            }
            
            resultSize = Marshal.ReadInt32(instance.Exports.memory.Start, resultSizePtr);
            return result;
        }
        finally {
            Free(resultSizePtr, 4);
        }
    }

    private int ExprToBuffer(Expr expr, out int resultSize) {
        var stream = new MemoryStream();

        var writer = new ESExprBinaryWriter(stream);
        writer.Write(expr).Wait();
        
        var data = stream.ToArray();
        
        var buffer = Alloc(data.Length);
        
        nint addr = checked((nint)(instance.Exports.memory.Start + unchecked((uint)buffer)));
        
        Marshal.Copy(data, 0, addr, data.Length);
        
        resultSize = data.Length;
        return buffer;
    }

    private Expr BufferToExpr(int ptr, int size) {
        var data = new byte[size];
        
        nint addr = checked((nint)(instance.Exports.memory.Start + unchecked((uint)ptr)));
        Marshal.Copy(addr, data, 0, size);
        
        var stream = new MemoryStream(data);
        var reader = new ESExprBinaryReader(stream);
        var exprs = reader.ReadAll().ToListAsync().AsTask().Result;

        if(exprs is [var expr]) {
            return expr;
        }

        throw new NobleIDLCompileErrorException("Expected a single expression");
    }

    public NobleIdlCompileModelResult LoadModel(NobleIdlCompileModelOptions options) {
        var optionsPtr = ExprToBuffer(new NobleIdlCompileModelOptions.Codec().Encode(options), out var optionsSize);
        try {
            var resultPtr = CompileModel(optionsPtr, optionsSize, out var resultSize);
            try {
                var expr = BufferToExpr(resultPtr, resultSize);
                try {
                    IESExprCodec<NobleIdlCompileModelResult> codec = new NobleIdlCompileModelResult.Codec();
                    return codec.Decode(expr);
                }
                catch(DecodeException ex) {
                    throw new NobleIDLCompileErrorException("Error decoding compiler result", ex);
                }
            }
            finally {
                Free(resultPtr, resultSize);
            }
        }
        finally {
            Free(optionsPtr, optionsSize);
        }
    }


    public void Dispose() {
        instance.Dispose();
    }
}
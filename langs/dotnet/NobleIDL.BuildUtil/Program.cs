namespace NobleIDL.BuildUtil;

using NobleIDL.Backend;

using WebAssembly;
using WebAssembly.Runtime;
using WebAssembly.Runtime.Compilation;

public class Program {
    public static void Main(string[] args) {
        var module = Module.ReadFromBinary("../../../target/wasm32-unknown-unknown/release/noble_idl_compiler.wasm");
        foreach(var e in module.Exports) {
            Console.WriteLine("Export: " + e);
        }

        var objCreator = WebAssembly.Runtime.Compile.FromBinary<NobleIDLCompiler>(
            "../../../target/wasm32-unknown-unknown/release/noble_idl_compiler.wasm");

        using var instance = objCreator(new Dictionary<string, IDictionary<string, RuntimeImport>>());

        Console.WriteLine(instance.Exports);
        
    }
}

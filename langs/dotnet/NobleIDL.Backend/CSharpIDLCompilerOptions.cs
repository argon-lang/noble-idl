using ESExpr.Runtime;

namespace NobleIDL.Backend;

[ESExprCodec]
public sealed partial record CSharpIDLCompilerOptions {
    [Keyword]
    public required CSharpLanguageOptions LanguageOptions { get; init; }
    
    [Keyword]
    public required VList<string> InputFileData { get; init; }
    
    [Keyword]
    public required VList<string> LibraryFileData { get; init; }
}
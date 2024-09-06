using ESExpr.Runtime;

namespace NobleIDL.Backend;

[ESExprCodec]
public sealed partial record CSharpLanguageOptions {
    [Keyword]
    public required string OutputFile { get; init; }
    
    [Keyword]
    public required NamespaceMapping NamespaceMapping { get; init; }
}
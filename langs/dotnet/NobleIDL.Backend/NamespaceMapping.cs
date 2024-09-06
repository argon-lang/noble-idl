using ESExpr.Runtime;

namespace NobleIDL.Backend;

[ESExprCodec]
public sealed partial record NamespaceMapping {
    public required VDict<string> Mapping { get; init; }
}
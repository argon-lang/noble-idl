using System.Numerics;
using ESExpr.Runtime;

namespace NobleIDL.Backend.Api;

[ESExprCodec]
public sealed partial record NobleIdlGenerationRequest<L> {
    [Keyword]
    public required L LanguageOptions { get; init; }
    
    [Keyword]
    public required NobleIdlModel Model { get; init; }
}

[ESExprCodec]
public sealed partial record NobleIdlGenerationResult {
    [Keyword]
    public required VList<string> GeneratedFiles { get; init; }
}

[ESExprCodec]
[Constructor("options")]
public sealed partial record NobleIdlCompileModelOptions {
    [Keyword]
    public VList<string> LibraryFiles { get; init; }
    
    [Keyword]
    public VList<string> Files { get; init; }
}

[ESExprCodec]
public abstract partial record NobleIdlCompileModelResult {
    private NobleIdlCompileModelResult() { }

    public sealed record Success : NobleIdlCompileModelResult {
        public required NobleIdlModel Model { get; init; }
    }

    public sealed record Failure : NobleIdlCompileModelResult {
        [Vararg]
        public required VList<string> Errors { get; init; }
    }
}

[ESExprCodec]
public sealed partial record NobleIdlModel {
    [Keyword]
    public required VList<DefinitionInfo> Definitions { get; init; }
}

[ESExprCodec]
public sealed partial record DefinitionInfo {
    [Keyword]
    public required QualifiedName Name { get; init; }
    
    [Keyword]
    public required VList<TypeParameter> TypeParameters { get; init; }
    
    [Keyword]
    public required Definition Definition { get; init; }
    
    [Keyword]
    public required VList<Annotation> Annotations { get; init; }
    
    [Keyword]
    public required bool IsLibrary { get; init; }
}

[ESExprCodec]
public sealed partial record PackageName {
    [Vararg]
    public required VList<string> Parts { get; init; }
}

[ESExprCodec]
public sealed partial record QualifiedName {
    public required PackageName Package { get; init; }
    public required string Name { get; init; }
}

[ESExprCodec]
public abstract partial record Definition {
    private Definition() { }

    [InlineValue]
    public sealed record Record : Definition {
        public required RecordDefinition R { get; init; }
    }

    [InlineValue]
    public sealed record Enum : Definition {
        public required EnumDefinition E { get; init; }
    }

    [InlineValue]
    public sealed record SimpleEnum : Definition {
        public required SimpleEnumDefinition E { get; init; }
    }

    [InlineValue]
    public sealed record ExternType : Definition {
        public required ExternTypeDefinition Et { get; init; }
    }

    [InlineValue]
    public sealed record Interface : Definition {
        public required InterfaceDefinition Iface { get; init; }
    }

    [InlineValue]
    public sealed record ExceptionType : Definition {
        public required ExceptionTypeDefinition Ex { get; init; }
    }
}

[ESExprCodec]
public sealed partial record RecordDefinition {
    [Vararg]
    public required VList<RecordField> Fields { get; init; }
    
    [Keyword]
    [Optional]
    public required Option<EsexprRecordOptions> EsexprOptions { get; init; }
}

[ESExprCodec]
public sealed partial record RecordField {
    public required string Name { get; init; }
    public required TypeExpr FieldType { get; init; }
    
    [Keyword]
    public required VList<Annotation> Annotations { get; init; }
    
    [Keyword]
    [Optional]
    public required Option<EsexprRecordFieldOptions> EsexprOptions { get; init; }
}

[ESExprCodec]
public sealed partial record EnumDefinition {
    [Vararg]
    public required VList<EnumCase> Cases { get; init; }
    
    [Keyword]
    [Optional]
    public required Option<EsexprEnumOptions> EsexprOptions { get; init; }
}

[ESExprCodec]
public sealed partial record EnumCase {
    public required string Name { get; init; }
    
    [Vararg]
    public required VList<RecordField> Fields { get; init; }
    
    [Keyword]
    [Optional]
    public required Option<EsexprEnumCaseOptions> EsexprOptions { get; init; }
    
    [Keyword]
    public required VList<Annotation> Annotations { get; init; }
}

[ESExprCodec]
public sealed partial record SimpleEnumDefinition {
    [Vararg]
    public required VList<SimpleEnumCase> Cases { get; init; }
    
    [Keyword]
    [Optional]
    public required Option<EsexprSimpleEnumOptions> EsexprOptions { get; init; }
}

[ESExprCodec]
public sealed partial record SimpleEnumCase {
    public required string Name { get; init; }
    
    [Keyword]
    [Optional]
    public required Option<EsexprSimpleEnumCaseOptions> EsexprOptions { get; init; }
    
    [Keyword]
    public required VList<Annotation> Annotations { get; init; }
}

[ESExprCodec]
public sealed partial record ExternTypeDefinition {
    [Keyword]
    [Optional]
    public required Option<EsexprExternTypeOptions> EsexprOptions { get; init; }
}

[ESExprCodec]
public sealed partial record InterfaceDefinition {
    [Vararg]
    public required VList<InterfaceMethod> Methods { get; init; }
}

[ESExprCodec]
public sealed partial record InterfaceMethod {
    [Keyword]
    public required string Name { get; init; }
    
    [Keyword]
    public required VList<TypeParameter> TypeParameters { get; init; }
    
    [Keyword]
    public required VList<InterfaceMethodParameter> Parameters { get; init; }
    
    [Keyword]
    public required TypeExpr ReturnType { get; init; }
    
    [Keyword]
    [Optional]
    public required Option<TypeExpr> Throws { get; init; }
    
    [Keyword]
    public required VList<Annotation> Annotations { get; init; }
}

[ESExprCodec]
public sealed partial record InterfaceMethodParameter {
    public required string Name { get; init; }
    public required TypeExpr ParameterType { get; init; }
    
    [Keyword]
    public required VList<Annotation> Annotations { get; init; }
}

[ESExprCodec]
public sealed partial record ExceptionTypeDefinition {
    public required TypeExpr Information { get; init; }
}

[ESExprCodec]
public sealed partial record Annotation {
    public required string Scope { get; init; }
    public required Expr Value { get; init; }
}

[ESExprCodec]
public abstract partial record TypeExpr {
    private TypeExpr() { }

    public sealed record DefinedType : TypeExpr {
        public required QualifiedName Name { get; init; }
        
        [Vararg]
        public required VList<TypeExpr> Args { get; init; }
    }

    public sealed record TypeParameter : TypeExpr {
        public required string Name { get; init; }
        
        [Keyword]
        public required TypeParameterOwner Owner { get; init; }
    }
}

public enum TypeParameterOwner {
    ByType,
    ByMethod,
}

[ESExprCodec]
public abstract partial record TypeParameter {
    private TypeParameter() { }

    public sealed record Type : TypeParameter {
        public required string Name { get; init; }

        [Keyword]
        public VList<TypeParameterTypeConstraint> Constraints { get; init; } =
            VList<TypeParameterTypeConstraint>.Empty;
        
        [Keyword]
        public required VList<Annotation> Annotations { get; init; }
    }
}

[ESExprCodec]
public abstract partial record TypeParameterTypeConstraint {
    private TypeParameterTypeConstraint() { }

    public sealed record Exception : TypeParameterTypeConstraint {
    }
}



[ESExprCodec]
[Constructor("record-options")]
public sealed partial record EsexprRecordOptions {
    [Keyword]
    public required string Constructor { get; init; }
}

[ESExprCodec]
[Constructor("enum-options")]
public sealed partial record EsexprEnumOptions {
}

[ESExprCodec]
[Constructor("enum-case-options")]
public sealed partial record EsexprEnumCaseOptions {
    public required EsexprEnumCaseType CaseType { get; init; }
}

[ESExprCodec]
public abstract partial record EsexprEnumCaseType {
    private EsexprEnumCaseType() { }

    public sealed record Constructor : EsexprEnumCaseType {
        public required string Name { get; init; }
    }

    public sealed record InlineValue : EsexprEnumCaseType {
    }
}

[ESExprCodec]
[Constructor("simple-enum-options")]
public sealed partial record EsexprSimpleEnumOptions {
}

[ESExprCodec]
[Constructor("simple-enum-case-options")]
public sealed partial record EsexprSimpleEnumCaseOptions {
    public required string Name { get; init; }
}

[ESExprCodec]
[Constructor("extern-type-options")]
public sealed partial record EsexprExternTypeOptions {
    [Keyword]
    public bool AllowValue { get; init; } = false;
    
    [Keyword]
    [Optional]
    public required Option<TypeExpr> AllowOptional { get; init; }
    
    [Keyword]
    [Optional]
    public required Option<TypeExpr> AllowVararg { get; init; }
    
    [Keyword]
    [Optional]
    public required Option<TypeExpr> AllowDict { get; init; }
    
    [Keyword]
    public required EsexprExternTypeLiterals Literals { get; init; }
}

[ESExprCodec]
[Constructor("literals")]
public sealed partial record EsexprExternTypeLiterals {
    [Keyword]
    public bool AllowBool { get; init; } = false;
    
    [Keyword]
    public bool AllowInt { get; init; } = false;
    
    [Keyword]
    [Optional]
    public required Option<BigInteger> MinInt { get; init; }
    
    [Keyword]
    [Optional]
    public required Option<BigInteger> MaxInt { get; init; }
    
    [Keyword]
    public bool AllowStr { get; init; } = false;
    
    [Keyword]
    public bool AllowBinary { get; init; } = false;
    
    [Keyword]
    public bool AllowFloat32 { get; init; } = false;
    
    [Keyword]
    public bool AllowFloat64 { get; init; } = false;
    
    [Keyword]
    public bool AllowNull { get; init; } = false;
    
    [Keyword]
    [Optional]
    public Option<TypeExpr> BuildLiteralFrom { get; init; }
}

[ESExprCodec]
[Constructor("field-options")]
public sealed partial record EsexprRecordFieldOptions {
    public required EsexprRecordFieldKind Kind { get; init; }
}

[ESExprCodec]
public abstract partial record EsexprRecordFieldKind {
    private EsexprRecordFieldKind() { }

    public sealed record Positional : EsexprRecordFieldKind {
        public required EsexprRecordPositionalMode Mode { get; init; }
    }

    public sealed record Keyword : EsexprRecordFieldKind {
        public required string Name { get; init; }
        public required EsexprRecordKeywordMode Mode { get; init; }
    }

    public sealed record Dict : EsexprRecordFieldKind {
        public required TypeExpr ElementType { get; init; }
    }

    public sealed record Vararg : EsexprRecordFieldKind {
        public required TypeExpr ElementType { get; init; }
    }
}

[ESExprCodec]
public abstract partial record EsexprRecordPositionalMode {
    private EsexprRecordPositionalMode() { }

    public sealed record Required : EsexprRecordPositionalMode {
    }

    public sealed record Optional : EsexprRecordPositionalMode {
        public required TypeExpr ElementType { get; init; }
    }
}

[ESExprCodec]
public abstract partial record EsexprRecordKeywordMode {
    private EsexprRecordKeywordMode() { }

    public sealed record Required : EsexprRecordKeywordMode {
    }

    public sealed record Optional : EsexprRecordKeywordMode {
        public required TypeExpr ElementType { get; init; }
    }

    public sealed record DefaultValue : EsexprRecordKeywordMode {
        public required EsexprDecodedValue Value { get; init; }
    }
}

[ESExprCodec]
public abstract partial record EsexprDecodedValue {
    private EsexprDecodedValue() { }

    public sealed record Record : EsexprDecodedValue {
        public required TypeExpr T { get; init; }
        
        [Vararg]
        public required VList<EsexprDecodedFieldValue> Fields { get; init; }
    }
    
    public sealed record Enum : EsexprDecodedValue {
        public required TypeExpr T { get; init; }
        public required string CaseName { get; init; }
        
        [Vararg]
        public required VList<EsexprDecodedFieldValue> Fields { get; init; }
    }
    
    public sealed record Optional : EsexprDecodedValue {
        public required TypeExpr T { get; init; }
        public required TypeExpr ElementType { get; init; }
        
        [Optional]
        public required Option<EsexprDecodedValue> Value { get; init; }
    }
    
    public sealed record Vararg : EsexprDecodedValue {
        public required TypeExpr T { get; init; }
        public required TypeExpr ElementType { get; init; }
        
        [Vararg]
        public required VList<EsexprDecodedValue> Values { get; init; }
    }
    
    public sealed record Dict : EsexprDecodedValue {
        public required TypeExpr T { get; init; }
        public required TypeExpr ElementType { get; init; }
        
        [Dict]
        public required VDict<EsexprDecodedValue> Values { get; init; }
    }
    
    public sealed record BuildFrom : EsexprDecodedValue {
        public required TypeExpr T { get; init; }
        public required TypeExpr FromType { get; init; }
        public required EsexprDecodedValue FromValue { get; init; }
    }
    
    public sealed record FromBool : EsexprDecodedValue {
        public required TypeExpr T { get; init; }
        public required bool B { get; init; }
    }
    
    public sealed record FromInt : EsexprDecodedValue {
        public required TypeExpr T { get; init; }
        public required BigInteger I { get; init; }
        
        [Keyword]
        [Optional]
        public required Option<BigInteger> MinInt { get; init; }
        
        [Keyword]
        [Optional]
        public required Option<BigInteger> MaxInt { get; init; }
    }
    
    public sealed record FromStr : EsexprDecodedValue {
        public required TypeExpr T { get; init; }
        public required string S { get; init; }
    }
    
    public sealed record FromBinary : EsexprDecodedValue {
        public required TypeExpr T { get; init; }
        public required Binary B { get; init; }
    }
    
    public sealed record FromFloat32 : EsexprDecodedValue {
        public required TypeExpr T { get; init; }
        public required float F { get; init; }
    }
    
    public sealed record FromFloat64 : EsexprDecodedValue {
        public required TypeExpr T { get; init; }
        public required double F { get; init; }
    }
    
    public sealed record FromNull : EsexprDecodedValue {
        public required TypeExpr T { get; init; }
    }
}

[ESExprCodec]
[Constructor("field-value")]
public sealed partial record EsexprDecodedFieldValue {
    public required string Name { get; init; }
    public required EsexprDecodedValue Value { get; init; }
}


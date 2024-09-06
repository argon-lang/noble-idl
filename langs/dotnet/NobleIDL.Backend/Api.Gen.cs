namespace NobleIDL.Backend.Api
{
    [global::ESExpr.Runtime.ESExprCodec, global::ESExpr.Runtime.Constructor("annotation")]
    public sealed partial record Annotation
    {
        public required global::System.String Scope { get; init; }
        public required global::ESExpr.Runtime.Expr Value { get; init; }
    }

    [global::ESExpr.Runtime.ESExprCodec]
    public abstract partial record Definition
    {
        private Definition()
        {
        }

        [global::ESExpr.Runtime.InlineValue]
        public sealed record Record : Definition
        {
            public required global::NobleIDL.Backend.Api.RecordDefinition R { get; init; }
        }

        [global::ESExpr.Runtime.InlineValue]
        public sealed record Enum : Definition
        {
            public required global::NobleIDL.Backend.Api.EnumDefinition E { get; init; }
        }

        [global::ESExpr.Runtime.InlineValue]
        public sealed record SimpleEnum : Definition
        {
            public required global::NobleIDL.Backend.Api.SimpleEnumDefinition E { get; init; }
        }

        [global::ESExpr.Runtime.InlineValue]
        public sealed record ExternType : Definition
        {
            public required global::NobleIDL.Backend.Api.ExternTypeDefinition Et { get; init; }
        }

        [global::ESExpr.Runtime.InlineValue]
        public sealed record Interface : Definition
        {
            public required global::NobleIDL.Backend.Api.InterfaceDefinition Iface { get; init; }
        }

        [global::ESExpr.Runtime.InlineValue]
        public sealed record ExceptionType : Definition
        {
            public required global::NobleIDL.Backend.Api.ExceptionTypeDefinition Ex { get; init; }
        }
    }

    [global::ESExpr.Runtime.ESExprCodec, global::ESExpr.Runtime.Constructor("definition-info")]
    public sealed partial record DefinitionInfo
    {
        [global::ESExpr.Runtime.Keyword("name")]
        public required global::NobleIDL.Backend.Api.QualifiedName Name { get; init; }

        [global::ESExpr.Runtime.Keyword("type-parameters")]
        public required global::ESExpr.Runtime.VList<global::NobleIDL.Backend.Api.TypeParameter> TypeParameters { get; init; }

        [global::ESExpr.Runtime.Keyword("definition")]
        public required global::NobleIDL.Backend.Api.Definition Definition { get; init; }

        [global::ESExpr.Runtime.Keyword("annotations")]
        public required global::ESExpr.Runtime.VList<global::NobleIDL.Backend.Api.Annotation> Annotations { get; init; }

        [global::ESExpr.Runtime.Keyword("is-library")]
        public required global::System.Boolean IsLibrary { get; init; }
    }

    [global::ESExpr.Runtime.ESExprCodec, global::ESExpr.Runtime.Constructor("enum-case")]
    public sealed partial record EnumCase
    {
        public required global::System.String Name { get; init; }

        [global::ESExpr.Runtime.Vararg]
        public required global::ESExpr.Runtime.VList<global::NobleIDL.Backend.Api.RecordField> Fields { get; init; }

        [global::ESExpr.Runtime.Keyword("esexpr-options")]
        [global::ESExpr.Runtime.Optional]
        public required global::ESExpr.Runtime.Option<global::NobleIDL.Backend.Api.EsexprEnumCaseOptions> EsexprOptions { get; init; }

        [global::ESExpr.Runtime.Keyword("annotations")]
        public required global::ESExpr.Runtime.VList<global::NobleIDL.Backend.Api.Annotation> Annotations { get; init; }
    }

    [global::ESExpr.Runtime.ESExprCodec, global::ESExpr.Runtime.Constructor("enum-definition")]
    public sealed partial record EnumDefinition
    {
        [global::ESExpr.Runtime.Vararg]
        public required global::ESExpr.Runtime.VList<global::NobleIDL.Backend.Api.EnumCase> Cases { get; init; }

        [global::ESExpr.Runtime.Keyword("esexpr-options")]
        [global::ESExpr.Runtime.Optional]
        public required global::ESExpr.Runtime.Option<global::NobleIDL.Backend.Api.EsexprEnumOptions> EsexprOptions { get; init; }
    }

    [global::ESExpr.Runtime.ESExprCodec, global::ESExpr.Runtime.Constructor("field-value")]
    public sealed partial record EsexprDecodedFieldValue
    {
        public required global::System.String Name { get; init; }
        public required global::NobleIDL.Backend.Api.EsexprDecodedValue Value { get; init; }
    }

    [global::ESExpr.Runtime.ESExprCodec]
    public abstract partial record EsexprDecodedValue
    {
        private EsexprDecodedValue()
        {
        }

        [global::ESExpr.Runtime.Constructor("record")]
        public sealed record Record : EsexprDecodedValue
        {
            public required global::NobleIDL.Backend.Api.TypeExpr T { get; init; }

            [global::ESExpr.Runtime.Vararg]
            public required global::ESExpr.Runtime.VList<global::NobleIDL.Backend.Api.EsexprDecodedFieldValue> Fields { get; init; }
        }

        [global::ESExpr.Runtime.Constructor("enum")]
        public sealed record Enum : EsexprDecodedValue
        {
            public required global::NobleIDL.Backend.Api.TypeExpr T { get; init; }
            public required global::System.String CaseName { get; init; }

            [global::ESExpr.Runtime.Vararg]
            public required global::ESExpr.Runtime.VList<global::NobleIDL.Backend.Api.EsexprDecodedFieldValue> Fields { get; init; }
        }

        [global::ESExpr.Runtime.Constructor("optional")]
        public sealed record Optional : EsexprDecodedValue
        {
            public required global::NobleIDL.Backend.Api.TypeExpr T { get; init; }
            public required global::NobleIDL.Backend.Api.TypeExpr ElementType { get; init; }

            [global::ESExpr.Runtime.Optional]
            public required global::ESExpr.Runtime.Option<global::NobleIDL.Backend.Api.EsexprDecodedValue> Value { get; init; }
        }

        [global::ESExpr.Runtime.Constructor("vararg")]
        public sealed record Vararg : EsexprDecodedValue
        {
            public required global::NobleIDL.Backend.Api.TypeExpr T { get; init; }
            public required global::NobleIDL.Backend.Api.TypeExpr ElementType { get; init; }

            [global::ESExpr.Runtime.Vararg]
            public required global::ESExpr.Runtime.VList<global::NobleIDL.Backend.Api.EsexprDecodedValue> Values { get; init; }
        }

        [global::ESExpr.Runtime.Constructor("dict")]
        public sealed record Dict : EsexprDecodedValue
        {
            public required global::NobleIDL.Backend.Api.TypeExpr T { get; init; }
            public required global::NobleIDL.Backend.Api.TypeExpr ElementType { get; init; }

            [global::ESExpr.Runtime.Dict]
            public required global::ESExpr.Runtime.VDict<global::NobleIDL.Backend.Api.EsexprDecodedValue> Values { get; init; }
        }

        [global::ESExpr.Runtime.Constructor("build-from")]
        public sealed record BuildFrom : EsexprDecodedValue
        {
            public required global::NobleIDL.Backend.Api.TypeExpr T { get; init; }
            public required global::NobleIDL.Backend.Api.TypeExpr FromType { get; init; }
            public required global::NobleIDL.Backend.Api.EsexprDecodedValue FromValue { get; init; }
        }

        [global::ESExpr.Runtime.Constructor("from-bool")]
        public sealed record FromBool : EsexprDecodedValue
        {
            public required global::NobleIDL.Backend.Api.TypeExpr T { get; init; }
            public required global::System.Boolean B { get; init; }
        }

        [global::ESExpr.Runtime.Constructor("from-int")]
        public sealed record FromInt : EsexprDecodedValue
        {
            public required global::NobleIDL.Backend.Api.TypeExpr T { get; init; }
            public required global::System.Numerics.BigInteger I { get; init; }

            [global::ESExpr.Runtime.Keyword("min-int")]
            [global::ESExpr.Runtime.Optional]
            public required global::ESExpr.Runtime.Option<global::System.Numerics.BigInteger> MinInt { get; init; }

            [global::ESExpr.Runtime.Keyword("max-int")]
            [global::ESExpr.Runtime.Optional]
            public required global::ESExpr.Runtime.Option<global::System.Numerics.BigInteger> MaxInt { get; init; }
        }

        [global::ESExpr.Runtime.Constructor("from-str")]
        public sealed record FromStr : EsexprDecodedValue
        {
            public required global::NobleIDL.Backend.Api.TypeExpr T { get; init; }
            public required global::System.String S { get; init; }
        }

        [global::ESExpr.Runtime.Constructor("from-binary")]
        public sealed record FromBinary : EsexprDecodedValue
        {
            public required global::NobleIDL.Backend.Api.TypeExpr T { get; init; }
            public required global::ESExpr.Runtime.Binary B { get; init; }
        }

        [global::ESExpr.Runtime.Constructor("from-float32")]
        public sealed record FromFloat32 : EsexprDecodedValue
        {
            public required global::NobleIDL.Backend.Api.TypeExpr T { get; init; }
            public required global::System.Single F { get; init; }
        }

        [global::ESExpr.Runtime.Constructor("from-float64")]
        public sealed record FromFloat64 : EsexprDecodedValue
        {
            public required global::NobleIDL.Backend.Api.TypeExpr T { get; init; }
            public required global::System.Double F { get; init; }
        }

        [global::ESExpr.Runtime.Constructor("from-null")]
        public sealed record FromNull : EsexprDecodedValue
        {
            public required global::NobleIDL.Backend.Api.TypeExpr T { get; init; }
        }
    }

    [global::ESExpr.Runtime.ESExprCodec, global::ESExpr.Runtime.Constructor("enum-case-options")]
    public sealed partial record EsexprEnumCaseOptions
    {
        public required global::NobleIDL.Backend.Api.EsexprEnumCaseType CaseType { get; init; }
    }

    [global::ESExpr.Runtime.ESExprCodec]
    public abstract partial record EsexprEnumCaseType
    {
        private EsexprEnumCaseType()
        {
        }

        [global::ESExpr.Runtime.Constructor("constructor")]
        public sealed record Constructor : EsexprEnumCaseType
        {
            public required global::System.String Name { get; init; }
        }

        [global::ESExpr.Runtime.Constructor("inline-value")]
        public sealed record InlineValue : EsexprEnumCaseType
        {
        }
    }

    [global::ESExpr.Runtime.ESExprCodec, global::ESExpr.Runtime.Constructor("enum-options")]
    public sealed partial record EsexprEnumOptions
    {
    }

    [global::ESExpr.Runtime.ESExprCodec, global::ESExpr.Runtime.Constructor("literals")]
    public sealed partial record EsexprExternTypeLiterals
    {
        [global::ESExpr.Runtime.Keyword("allow-bool")]
        [global::ESExpr.Runtime.DefaultValue("global::NobleIDL.Runtime.Bool.FromBool(false)")]
        public required global::System.Boolean AllowBool { get; init; }

        [global::ESExpr.Runtime.Keyword("allow-int")]
        [global::ESExpr.Runtime.DefaultValue("global::NobleIDL.Runtime.Bool.FromBool(false)")]
        public required global::System.Boolean AllowInt { get; init; }

        [global::ESExpr.Runtime.Keyword("min-int")]
        [global::ESExpr.Runtime.Optional]
        public required global::ESExpr.Runtime.Option<global::System.Numerics.BigInteger> MinInt { get; init; }

        [global::ESExpr.Runtime.Keyword("max-int")]
        [global::ESExpr.Runtime.Optional]
        public required global::ESExpr.Runtime.Option<global::System.Numerics.BigInteger> MaxInt { get; init; }

        [global::ESExpr.Runtime.Keyword("allow-str")]
        [global::ESExpr.Runtime.DefaultValue("global::NobleIDL.Runtime.Bool.FromBool(false)")]
        public required global::System.Boolean AllowStr { get; init; }

        [global::ESExpr.Runtime.Keyword("allow-binary")]
        [global::ESExpr.Runtime.DefaultValue("global::NobleIDL.Runtime.Bool.FromBool(false)")]
        public required global::System.Boolean AllowBinary { get; init; }

        [global::ESExpr.Runtime.Keyword("allow-float32")]
        [global::ESExpr.Runtime.DefaultValue("global::NobleIDL.Runtime.Bool.FromBool(false)")]
        public required global::System.Boolean AllowFloat32 { get; init; }

        [global::ESExpr.Runtime.Keyword("allow-float64")]
        [global::ESExpr.Runtime.DefaultValue("global::NobleIDL.Runtime.Bool.FromBool(false)")]
        public required global::System.Boolean AllowFloat64 { get; init; }

        [global::ESExpr.Runtime.Keyword("allow-null")]
        [global::ESExpr.Runtime.DefaultValue("global::NobleIDL.Runtime.Bool.FromBool(false)")]
        public required global::System.Boolean AllowNull { get; init; }

        [global::ESExpr.Runtime.Keyword("build-literal-from")]
        [global::ESExpr.Runtime.Optional]
        public required global::ESExpr.Runtime.Option<global::NobleIDL.Backend.Api.TypeExpr> BuildLiteralFrom { get; init; }
    }

    [global::ESExpr.Runtime.ESExprCodec, global::ESExpr.Runtime.Constructor("extern-type-options")]
    public sealed partial record EsexprExternTypeOptions
    {
        [global::ESExpr.Runtime.Keyword("allow-value")]
        [global::ESExpr.Runtime.DefaultValue("global::NobleIDL.Runtime.Bool.FromBool(false)")]
        public required global::System.Boolean AllowValue { get; init; }

        [global::ESExpr.Runtime.Keyword("allow-optional")]
        [global::ESExpr.Runtime.Optional]
        public required global::ESExpr.Runtime.Option<global::NobleIDL.Backend.Api.TypeExpr> AllowOptional { get; init; }

        [global::ESExpr.Runtime.Keyword("allow-vararg")]
        [global::ESExpr.Runtime.Optional]
        public required global::ESExpr.Runtime.Option<global::NobleIDL.Backend.Api.TypeExpr> AllowVararg { get; init; }

        [global::ESExpr.Runtime.Keyword("allow-dict")]
        [global::ESExpr.Runtime.Optional]
        public required global::ESExpr.Runtime.Option<global::NobleIDL.Backend.Api.TypeExpr> AllowDict { get; init; }

        [global::ESExpr.Runtime.Keyword("literals")]
        public required global::NobleIDL.Backend.Api.EsexprExternTypeLiterals Literals { get; init; }
    }

    [global::ESExpr.Runtime.ESExprCodec]
    public abstract partial record EsexprRecordFieldKind
    {
        private EsexprRecordFieldKind()
        {
        }

        [global::ESExpr.Runtime.Constructor("positional")]
        public sealed record Positional : EsexprRecordFieldKind
        {
            public required global::NobleIDL.Backend.Api.EsexprRecordPositionalMode Mode { get; init; }
        }

        [global::ESExpr.Runtime.Constructor("keyword")]
        public sealed record Keyword : EsexprRecordFieldKind
        {
            public required global::System.String Name { get; init; }
            public required global::NobleIDL.Backend.Api.EsexprRecordKeywordMode Mode { get; init; }
        }

        [global::ESExpr.Runtime.Constructor("dict")]
        public sealed record Dict : EsexprRecordFieldKind
        {
            public required global::NobleIDL.Backend.Api.TypeExpr ElementType { get; init; }
        }

        [global::ESExpr.Runtime.Constructor("vararg")]
        public sealed record Vararg : EsexprRecordFieldKind
        {
            public required global::NobleIDL.Backend.Api.TypeExpr ElementType { get; init; }
        }
    }

    [global::ESExpr.Runtime.ESExprCodec, global::ESExpr.Runtime.Constructor("field-options")]
    public sealed partial record EsexprRecordFieldOptions
    {
        public required global::NobleIDL.Backend.Api.EsexprRecordFieldKind Kind { get; init; }
    }

    [global::ESExpr.Runtime.ESExprCodec]
    public abstract partial record EsexprRecordKeywordMode
    {
        private EsexprRecordKeywordMode()
        {
        }

        [global::ESExpr.Runtime.Constructor("required")]
        public sealed record Required : EsexprRecordKeywordMode
        {
        }

        [global::ESExpr.Runtime.Constructor("optional")]
        public sealed record Optional : EsexprRecordKeywordMode
        {
            public required global::NobleIDL.Backend.Api.TypeExpr ElementType { get; init; }
        }

        [global::ESExpr.Runtime.Constructor("default-value")]
        public sealed record DefaultValue : EsexprRecordKeywordMode
        {
            public required global::NobleIDL.Backend.Api.EsexprDecodedValue Value { get; init; }
        }
    }

    [global::ESExpr.Runtime.ESExprCodec, global::ESExpr.Runtime.Constructor("record-options")]
    public sealed partial record EsexprRecordOptions
    {
        [global::ESExpr.Runtime.Keyword("constructor")]
        public required global::System.String Constructor { get; init; }
    }

    [global::ESExpr.Runtime.ESExprCodec]
    public abstract partial record EsexprRecordPositionalMode
    {
        private EsexprRecordPositionalMode()
        {
        }

        [global::ESExpr.Runtime.Constructor("required")]
        public sealed record Required : EsexprRecordPositionalMode
        {
        }

        [global::ESExpr.Runtime.Constructor("optional")]
        public sealed record Optional : EsexprRecordPositionalMode
        {
            public required global::NobleIDL.Backend.Api.TypeExpr ElementType { get; init; }
        }
    }

    [global::ESExpr.Runtime.ESExprCodec, global::ESExpr.Runtime.Constructor("simple-enum-case-options")]
    public sealed partial record EsexprSimpleEnumCaseOptions
    {
        public required global::System.String Name { get; init; }
    }

    [global::ESExpr.Runtime.ESExprCodec, global::ESExpr.Runtime.Constructor("simple-enum-options")]
    public sealed partial record EsexprSimpleEnumOptions
    {
    }

    [global::ESExpr.Runtime.ESExprCodec, global::ESExpr.Runtime.Constructor("exception-type-definition")]
    public sealed partial record ExceptionTypeDefinition
    {
        public required global::NobleIDL.Backend.Api.TypeExpr Information { get; init; }
    }

    [global::ESExpr.Runtime.ESExprCodec, global::ESExpr.Runtime.Constructor("extern-type-definition")]
    public sealed partial record ExternTypeDefinition
    {
        [global::ESExpr.Runtime.Keyword("esexpr-options")]
        [global::ESExpr.Runtime.Optional]
        public required global::ESExpr.Runtime.Option<global::NobleIDL.Backend.Api.EsexprExternTypeOptions> EsexprOptions { get; init; }
    }

    [global::ESExpr.Runtime.ESExprCodec, global::ESExpr.Runtime.Constructor("interface-definition")]
    public sealed partial record InterfaceDefinition
    {
        [global::ESExpr.Runtime.Vararg]
        public required global::ESExpr.Runtime.VList<global::NobleIDL.Backend.Api.InterfaceMethod> Methods { get; init; }
    }

    [global::ESExpr.Runtime.ESExprCodec, global::ESExpr.Runtime.Constructor("interface-method")]
    public sealed partial record InterfaceMethod
    {
        [global::ESExpr.Runtime.Keyword("name")]
        public required global::System.String Name { get; init; }

        [global::ESExpr.Runtime.Keyword("type-parameters")]
        public required global::ESExpr.Runtime.VList<global::NobleIDL.Backend.Api.TypeParameter> TypeParameters { get; init; }

        [global::ESExpr.Runtime.Keyword("parameters")]
        public required global::ESExpr.Runtime.VList<global::NobleIDL.Backend.Api.InterfaceMethodParameter> Parameters { get; init; }

        [global::ESExpr.Runtime.Keyword("return-type")]
        public required global::NobleIDL.Backend.Api.TypeExpr ReturnType { get; init; }

        [global::ESExpr.Runtime.Keyword("throws")]
        [global::ESExpr.Runtime.Optional]
        public required global::ESExpr.Runtime.Option<global::NobleIDL.Backend.Api.TypeExpr> Throws { get; init; }

        [global::ESExpr.Runtime.Keyword("annotations")]
        public required global::ESExpr.Runtime.VList<global::NobleIDL.Backend.Api.Annotation> Annotations { get; init; }
    }

    [global::ESExpr.Runtime.ESExprCodec, global::ESExpr.Runtime.Constructor("interface-method-parameter")]
    public sealed partial record InterfaceMethodParameter
    {
        public required global::System.String Name { get; init; }
        public required global::NobleIDL.Backend.Api.TypeExpr ParameterType { get; init; }

        [global::ESExpr.Runtime.Keyword("annotations")]
        public required global::ESExpr.Runtime.VList<global::NobleIDL.Backend.Api.Annotation> Annotations { get; init; }
    }

    [global::ESExpr.Runtime.ESExprCodec, global::ESExpr.Runtime.Constructor("options")]
    public sealed partial record NobleIdlCompileModelOptions
    {
        [global::ESExpr.Runtime.Keyword("library-files")]
        public required global::ESExpr.Runtime.VList<global::System.String> LibraryFiles { get; init; }

        [global::ESExpr.Runtime.Keyword("files")]
        public required global::ESExpr.Runtime.VList<global::System.String> Files { get; init; }
    }

    [global::ESExpr.Runtime.ESExprCodec]
    public abstract partial record NobleIdlCompileModelResult
    {
        private NobleIdlCompileModelResult()
        {
        }

        [global::ESExpr.Runtime.Constructor("success")]
        public sealed record Success : NobleIdlCompileModelResult
        {
            public required global::NobleIDL.Backend.Api.NobleIdlModel Model { get; init; }
        }

        [global::ESExpr.Runtime.Constructor("failure")]
        public sealed record Failure : NobleIdlCompileModelResult
        {
            [global::ESExpr.Runtime.Vararg]
            public required global::ESExpr.Runtime.VList<global::System.String> Errors { get; init; }
        }
    }

    [global::ESExpr.Runtime.ESExprCodec, global::ESExpr.Runtime.Constructor("noble-idl-generation-request")]
    public sealed partial record NobleIdlGenerationRequest<L>
    {
        [global::ESExpr.Runtime.Keyword("language-options")]
        public required L LanguageOptions { get; init; }

        [global::ESExpr.Runtime.Keyword("model")]
        public required global::NobleIDL.Backend.Api.NobleIdlModel Model { get; init; }
    }

    [global::ESExpr.Runtime.ESExprCodec, global::ESExpr.Runtime.Constructor("noble-idl-generation-result")]
    public sealed partial record NobleIdlGenerationResult
    {
        [global::ESExpr.Runtime.Keyword("generated-files")]
        public required global::ESExpr.Runtime.VList<global::System.String> GeneratedFiles { get; init; }
    }

    [global::ESExpr.Runtime.ESExprCodec, global::ESExpr.Runtime.Constructor("noble-idl-model")]
    public sealed partial record NobleIdlModel
    {
        [global::ESExpr.Runtime.Keyword("definitions")]
        public required global::ESExpr.Runtime.VList<global::NobleIDL.Backend.Api.DefinitionInfo> Definitions { get; init; }
    }

    [global::ESExpr.Runtime.ESExprCodec, global::ESExpr.Runtime.Constructor("package-name")]
    public sealed partial record PackageName
    {
        [global::ESExpr.Runtime.Vararg]
        public required global::ESExpr.Runtime.VList<global::System.String> Parts { get; init; }
    }

    [global::ESExpr.Runtime.ESExprCodec, global::ESExpr.Runtime.Constructor("qualified-name")]
    public sealed partial record QualifiedName
    {
        public required global::NobleIDL.Backend.Api.PackageName Package { get; init; }
        public required global::System.String Name { get; init; }
    }

    [global::ESExpr.Runtime.ESExprCodec, global::ESExpr.Runtime.Constructor("record-definition")]
    public sealed partial record RecordDefinition
    {
        [global::ESExpr.Runtime.Vararg]
        public required global::ESExpr.Runtime.VList<global::NobleIDL.Backend.Api.RecordField> Fields { get; init; }

        [global::ESExpr.Runtime.Keyword("esexpr-options")]
        [global::ESExpr.Runtime.Optional]
        public required global::ESExpr.Runtime.Option<global::NobleIDL.Backend.Api.EsexprRecordOptions> EsexprOptions { get; init; }
    }

    [global::ESExpr.Runtime.ESExprCodec, global::ESExpr.Runtime.Constructor("record-field")]
    public sealed partial record RecordField
    {
        public required global::System.String Name { get; init; }
        public required global::NobleIDL.Backend.Api.TypeExpr FieldType { get; init; }

        [global::ESExpr.Runtime.Keyword("annotations")]
        public required global::ESExpr.Runtime.VList<global::NobleIDL.Backend.Api.Annotation> Annotations { get; init; }

        [global::ESExpr.Runtime.Keyword("esexpr-options")]
        [global::ESExpr.Runtime.Optional]
        public required global::ESExpr.Runtime.Option<global::NobleIDL.Backend.Api.EsexprRecordFieldOptions> EsexprOptions { get; init; }
    }

    [global::ESExpr.Runtime.ESExprCodec, global::ESExpr.Runtime.Constructor("simple-enum-case")]
    public sealed partial record SimpleEnumCase
    {
        public required global::System.String Name { get; init; }

        [global::ESExpr.Runtime.Keyword("esexpr-options")]
        [global::ESExpr.Runtime.Optional]
        public required global::ESExpr.Runtime.Option<global::NobleIDL.Backend.Api.EsexprSimpleEnumCaseOptions> EsexprOptions { get; init; }

        [global::ESExpr.Runtime.Keyword("annotations")]
        public required global::ESExpr.Runtime.VList<global::NobleIDL.Backend.Api.Annotation> Annotations { get; init; }
    }

    [global::ESExpr.Runtime.ESExprCodec, global::ESExpr.Runtime.Constructor("simple-enum-definition")]
    public sealed partial record SimpleEnumDefinition
    {
        [global::ESExpr.Runtime.Vararg]
        public required global::ESExpr.Runtime.VList<global::NobleIDL.Backend.Api.SimpleEnumCase> Cases { get; init; }

        [global::ESExpr.Runtime.Keyword("esexpr-options")]
        [global::ESExpr.Runtime.Optional]
        public required global::ESExpr.Runtime.Option<global::NobleIDL.Backend.Api.EsexprSimpleEnumOptions> EsexprOptions { get; init; }
    }

    [global::ESExpr.Runtime.ESExprCodec]
    public abstract partial record TypeExpr
    {
        private TypeExpr()
        {
        }

        [global::ESExpr.Runtime.Constructor("defined-type")]
        public sealed record DefinedType : TypeExpr
        {
            public required global::NobleIDL.Backend.Api.QualifiedName Name { get; init; }

            [global::ESExpr.Runtime.Vararg]
            public required global::ESExpr.Runtime.VList<global::NobleIDL.Backend.Api.TypeExpr> Args { get; init; }
        }

        [global::ESExpr.Runtime.Constructor("type-parameter")]
        public sealed record TypeParameter : TypeExpr
        {
            public required global::System.String Name { get; init; }

            [global::ESExpr.Runtime.Keyword("owner")]
            public required global::NobleIDL.Backend.Api.TypeParameterOwner Owner { get; init; }
        }
    }

    [global::ESExpr.Runtime.ESExprCodec]
    public abstract partial record TypeParameter
    {
        private TypeParameter()
        {
        }

        [global::ESExpr.Runtime.Constructor("type")]
        public sealed record Type : TypeParameter
        {
            public required global::System.String Name { get; init; }

            [global::ESExpr.Runtime.Keyword("constraints")]
            [global::ESExpr.Runtime.DefaultValue("global::NobleIDL.Runtime.List<global::NobleIDL.Backend.Api.TypeParameterTypeConstraint>.BuildFrom(new global::NobleIDL.Runtime.ListRepr<global::NobleIDL.Backend.Api.TypeParameterTypeConstraint> { Values = global::NobleIDL.Runtime.List<global::NobleIDL.Backend.Api.TypeParameterTypeConstraint>.FromValues([]) })")]
            public required global::ESExpr.Runtime.VList<global::NobleIDL.Backend.Api.TypeParameterTypeConstraint> Constraints { get; init; }

            [global::ESExpr.Runtime.Keyword("annotations")]
            public required global::ESExpr.Runtime.VList<global::NobleIDL.Backend.Api.Annotation> Annotations { get; init; }
        }
    }

    public enum TypeParameterOwner
    {
        [global::ESExpr.Runtime.Constructor("by-type")]
        ByType,
        [global::ESExpr.Runtime.Constructor("by-method")]
        ByMethod
    }

    [global::ESExpr.Runtime.ESExprCodec]
    public abstract partial record TypeParameterTypeConstraint
    {
        private TypeParameterTypeConstraint()
        {
        }

        [global::ESExpr.Runtime.Constructor("exception")]
        public sealed record Exception : TypeParameterTypeConstraint
        {
        }
    }
}
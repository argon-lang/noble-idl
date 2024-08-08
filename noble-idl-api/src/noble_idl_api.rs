#[derive(
    ::std::fmt::Debug,
    ::std::clone::Clone,
    ::std::cmp::PartialEq,
    ::esexpr::ESExprCodec
)]
#[constructor = "annotation"]
pub struct Annotation {
    pub scope: ::noble_idl_runtime::String,
    pub value: ::noble_idl_runtime::Esexpr,
}
#[derive(
    ::std::fmt::Debug,
    ::std::clone::Clone,
    ::std::cmp::PartialEq,
    ::esexpr::ESExprCodec
)]
pub enum Definition {
    #[inline_value]
    Record(crate::RecordDefinition),
    #[inline_value]
    Enum(crate::EnumDefinition),
    #[inline_value]
    SimpleEnum(crate::SimpleEnumDefinition),
    #[inline_value]
    ExternType(crate::ExternTypeDefinition),
    #[inline_value]
    Interface(crate::InterfaceDefinition),
}
#[derive(
    ::std::fmt::Debug,
    ::std::clone::Clone,
    ::std::cmp::PartialEq,
    ::esexpr::ESExprCodec
)]
#[constructor = "definition-info"]
pub struct DefinitionInfo {
    #[keyword = "name"]
    pub name: crate::QualifiedName,
    #[keyword = "type-parameters"]
    pub type_parameters: ::noble_idl_runtime::List<crate::TypeParameter>,
    #[keyword = "definition"]
    pub definition: crate::Definition,
    #[keyword = "annotations"]
    pub annotations: ::noble_idl_runtime::List<crate::Annotation>,
    #[keyword = "is-library"]
    pub is_library: ::noble_idl_runtime::Bool,
}
#[derive(
    ::std::fmt::Debug,
    ::std::clone::Clone,
    ::std::cmp::PartialEq,
    ::esexpr::ESExprCodec
)]
#[constructor = "enum-case"]
pub struct EnumCase {
    pub name: ::noble_idl_runtime::String,
    #[vararg]
    pub fields: ::noble_idl_runtime::List<crate::RecordField>,
    #[keyword = "esexpr-options"]
    #[optional]
    pub esexpr_options: ::noble_idl_runtime::OptionalField<crate::EsexprEnumCaseOptions>,
    #[keyword = "annotations"]
    pub annotations: ::noble_idl_runtime::List<crate::Annotation>,
}
#[derive(
    ::std::fmt::Debug,
    ::std::clone::Clone,
    ::std::cmp::PartialEq,
    ::esexpr::ESExprCodec
)]
#[constructor = "enum-definition"]
pub struct EnumDefinition {
    #[vararg]
    pub cases: ::noble_idl_runtime::List<crate::EnumCase>,
    #[keyword = "esexpr-options"]
    #[optional]
    pub esexpr_options: ::noble_idl_runtime::OptionalField<crate::EsexprEnumOptions>,
}
#[derive(
    ::std::fmt::Debug,
    ::std::clone::Clone,
    ::std::cmp::PartialEq,
    ::esexpr::ESExprCodec
)]
#[constructor = "field-value"]
pub struct EsexprDecodedFieldValue {
    pub name: ::noble_idl_runtime::String,
    pub value: crate::EsexprDecodedValue,
}
#[derive(
    ::std::fmt::Debug,
    ::std::clone::Clone,
    ::std::cmp::PartialEq,
    ::esexpr::ESExprCodec
)]
pub enum EsexprDecodedValue {
    #[constructor = "record"]
    Record {
        t: crate::TypeExpr,
        #[vararg]
        fields: ::noble_idl_runtime::List<crate::EsexprDecodedFieldValue>,
    },
    #[constructor = "enum"]
    Enum {
        t: crate::TypeExpr,
        case_name: ::noble_idl_runtime::String,
        #[vararg]
        fields: ::noble_idl_runtime::List<crate::EsexprDecodedFieldValue>,
    },
    #[constructor = "optional"]
    Optional {
        t: crate::TypeExpr,
        element_type: crate::TypeExpr,
        #[optional]
        value: ::std::boxed::Box<
            ::noble_idl_runtime::OptionalField<crate::EsexprDecodedValue>,
        >,
    },
    #[constructor = "vararg"]
    Vararg {
        t: crate::TypeExpr,
        element_type: crate::TypeExpr,
        #[vararg]
        values: ::noble_idl_runtime::List<crate::EsexprDecodedValue>,
    },
    #[constructor = "dict"]
    Dict {
        t: crate::TypeExpr,
        element_type: crate::TypeExpr,
        #[dict]
        values: ::noble_idl_runtime::Dict<crate::EsexprDecodedValue>,
    },
    #[constructor = "build-from"]
    BuildFrom {
        t: crate::TypeExpr,
        from_type: crate::TypeExpr,
        from_value: ::std::boxed::Box<crate::EsexprDecodedValue>,
    },
    #[constructor = "from-bool"]
    FromBool { t: crate::TypeExpr, b: ::noble_idl_runtime::Bool },
    #[constructor = "from-int"]
    FromInt {
        t: crate::TypeExpr,
        i: ::noble_idl_runtime::Int,
        #[keyword = "min-int"]
        #[optional]
        min_int: ::noble_idl_runtime::OptionalField<::noble_idl_runtime::Int>,
        #[keyword = "max-int"]
        #[optional]
        max_int: ::noble_idl_runtime::OptionalField<::noble_idl_runtime::Int>,
    },
    #[constructor = "from-str"]
    FromStr { t: crate::TypeExpr, s: ::noble_idl_runtime::String },
    #[constructor = "from-binary"]
    FromBinary { t: crate::TypeExpr, b: ::noble_idl_runtime::Binary },
    #[constructor = "from-float32"]
    FromFloat32 { t: crate::TypeExpr, f: ::noble_idl_runtime::F32 },
    #[constructor = "from-float64"]
    FromFloat64 { t: crate::TypeExpr, f: ::noble_idl_runtime::F64 },
    #[constructor = "from-null"]
    FromNull { t: crate::TypeExpr },
}
#[derive(
    ::std::fmt::Debug,
    ::std::clone::Clone,
    ::std::cmp::PartialEq,
    ::esexpr::ESExprCodec
)]
#[constructor = "enum-case-options"]
pub struct EsexprEnumCaseOptions {
    pub case_type: crate::EsexprEnumCaseType,
}
#[derive(
    ::std::fmt::Debug,
    ::std::clone::Clone,
    ::std::cmp::PartialEq,
    ::esexpr::ESExprCodec
)]
pub enum EsexprEnumCaseType {
    #[constructor = "constructor"]
    Constructor(::noble_idl_runtime::String),
    #[constructor = "inline-value"]
    InlineValue,
}
#[derive(
    ::std::fmt::Debug,
    ::std::clone::Clone,
    ::std::cmp::PartialEq,
    ::esexpr::ESExprCodec
)]
#[constructor = "enum-options"]
pub struct EsexprEnumOptions {}
#[derive(
    ::std::fmt::Debug,
    ::std::clone::Clone,
    ::std::cmp::PartialEq,
    ::esexpr::ESExprCodec
)]
#[constructor = "literals"]
pub struct EsexprExternTypeLiterals {
    #[keyword = "allow-bool"]
    #[default_value = "< :: noble_idl_runtime :: Bool as :: std :: convert :: From < :: std :: primitive :: bool > > :: from (false)"]
    pub allow_bool: ::noble_idl_runtime::Bool,
    #[keyword = "allow-int"]
    #[default_value = "< :: noble_idl_runtime :: Bool as :: std :: convert :: From < :: std :: primitive :: bool > > :: from (false)"]
    pub allow_int: ::noble_idl_runtime::Bool,
    #[keyword = "min-int"]
    #[optional]
    pub min_int: ::noble_idl_runtime::OptionalField<::noble_idl_runtime::Int>,
    #[keyword = "max-int"]
    #[optional]
    pub max_int: ::noble_idl_runtime::OptionalField<::noble_idl_runtime::Int>,
    #[keyword = "allow-str"]
    #[default_value = "< :: noble_idl_runtime :: Bool as :: std :: convert :: From < :: std :: primitive :: bool > > :: from (false)"]
    pub allow_str: ::noble_idl_runtime::Bool,
    #[keyword = "allow-binary"]
    #[default_value = "< :: noble_idl_runtime :: Bool as :: std :: convert :: From < :: std :: primitive :: bool > > :: from (false)"]
    pub allow_binary: ::noble_idl_runtime::Bool,
    #[keyword = "allow-float32"]
    #[default_value = "< :: noble_idl_runtime :: Bool as :: std :: convert :: From < :: std :: primitive :: bool > > :: from (false)"]
    pub allow_float32: ::noble_idl_runtime::Bool,
    #[keyword = "allow-float64"]
    #[default_value = "< :: noble_idl_runtime :: Bool as :: std :: convert :: From < :: std :: primitive :: bool > > :: from (false)"]
    pub allow_float64: ::noble_idl_runtime::Bool,
    #[keyword = "allow-null"]
    #[default_value = "< :: noble_idl_runtime :: Bool as :: std :: convert :: From < :: std :: primitive :: bool > > :: from (false)"]
    pub allow_null: ::noble_idl_runtime::Bool,
    #[keyword = "build-literal-from"]
    #[optional]
    pub build_literal_from: ::noble_idl_runtime::OptionalField<crate::TypeExpr>,
}
#[derive(
    ::std::fmt::Debug,
    ::std::clone::Clone,
    ::std::cmp::PartialEq,
    ::esexpr::ESExprCodec
)]
#[constructor = "extern-type-options"]
pub struct EsexprExternTypeOptions {
    #[keyword = "allow-value"]
    #[default_value = "< :: noble_idl_runtime :: Bool as :: std :: convert :: From < :: std :: primitive :: bool > > :: from (false)"]
    pub allow_value: ::noble_idl_runtime::Bool,
    #[keyword = "allow-optional"]
    #[optional]
    pub allow_optional: ::noble_idl_runtime::OptionalField<crate::TypeExpr>,
    #[keyword = "allow-vararg"]
    #[optional]
    pub allow_vararg: ::noble_idl_runtime::OptionalField<crate::TypeExpr>,
    #[keyword = "allow-dict"]
    #[optional]
    pub allow_dict: ::noble_idl_runtime::OptionalField<crate::TypeExpr>,
    #[keyword = "literals"]
    pub literals: crate::EsexprExternTypeLiterals,
}
#[derive(
    ::std::fmt::Debug,
    ::std::clone::Clone,
    ::std::cmp::PartialEq,
    ::esexpr::ESExprCodec
)]
pub enum EsexprRecordFieldKind {
    #[constructor = "positional"]
    Positional(crate::EsexprRecordPositionalMode),
    #[constructor = "keyword"]
    Keyword(::noble_idl_runtime::String, crate::EsexprRecordKeywordMode),
    #[constructor = "dict"]
    Dict(crate::TypeExpr),
    #[constructor = "vararg"]
    Vararg(crate::TypeExpr),
}
#[derive(
    ::std::fmt::Debug,
    ::std::clone::Clone,
    ::std::cmp::PartialEq,
    ::esexpr::ESExprCodec
)]
#[constructor = "field-options"]
pub struct EsexprRecordFieldOptions {
    pub kind: crate::EsexprRecordFieldKind,
}
#[derive(
    ::std::fmt::Debug,
    ::std::clone::Clone,
    ::std::cmp::PartialEq,
    ::esexpr::ESExprCodec
)]
pub enum EsexprRecordKeywordMode {
    #[constructor = "required"]
    Required,
    #[constructor = "optional"]
    Optional(crate::TypeExpr),
    #[constructor = "default-value"]
    DefaultValue(crate::EsexprDecodedValue),
}
#[derive(
    ::std::fmt::Debug,
    ::std::clone::Clone,
    ::std::cmp::PartialEq,
    ::esexpr::ESExprCodec
)]
#[constructor = "record-options"]
pub struct EsexprRecordOptions {
    #[keyword = "constructor"]
    pub constructor: ::noble_idl_runtime::String,
}
#[derive(
    ::std::fmt::Debug,
    ::std::clone::Clone,
    ::std::cmp::PartialEq,
    ::esexpr::ESExprCodec
)]
pub enum EsexprRecordPositionalMode {
    #[constructor = "required"]
    Required,
    #[constructor = "optional"]
    Optional(crate::TypeExpr),
}
#[derive(
    ::std::fmt::Debug,
    ::std::clone::Clone,
    ::std::cmp::PartialEq,
    ::esexpr::ESExprCodec
)]
#[constructor = "simple-enum-case-options"]
pub struct EsexprSimpleEnumCaseOptions {
    pub name: ::noble_idl_runtime::String,
}
#[derive(
    ::std::fmt::Debug,
    ::std::clone::Clone,
    ::std::cmp::PartialEq,
    ::esexpr::ESExprCodec
)]
#[constructor = "simple-enum-options"]
pub struct EsexprSimpleEnumOptions {}
#[derive(
    ::std::fmt::Debug,
    ::std::clone::Clone,
    ::std::cmp::PartialEq,
    ::esexpr::ESExprCodec
)]
#[constructor = "extern-type-definition"]
pub struct ExternTypeDefinition {
    #[keyword = "esexpr-options"]
    #[optional]
    pub esexpr_options: ::noble_idl_runtime::OptionalField<
        crate::EsexprExternTypeOptions,
    >,
}
#[derive(
    ::std::fmt::Debug,
    ::std::clone::Clone,
    ::std::cmp::PartialEq,
    ::esexpr::ESExprCodec
)]
#[constructor = "interface-definition"]
pub struct InterfaceDefinition {
    #[vararg]
    pub methods: ::noble_idl_runtime::List<crate::InterfaceMethod>,
}
#[derive(
    ::std::fmt::Debug,
    ::std::clone::Clone,
    ::std::cmp::PartialEq,
    ::esexpr::ESExprCodec
)]
#[constructor = "interface-method"]
pub struct InterfaceMethod {
    #[keyword = "name"]
    pub name: ::noble_idl_runtime::String,
    #[keyword = "type-parameters"]
    pub type_parameters: ::noble_idl_runtime::List<crate::TypeParameter>,
    #[keyword = "parameters"]
    pub parameters: ::noble_idl_runtime::List<crate::InterfaceMethodParameter>,
    #[keyword = "return-type"]
    pub return_type: crate::TypeExpr,
    #[keyword = "annotations"]
    pub annotations: ::noble_idl_runtime::List<crate::Annotation>,
}
#[derive(
    ::std::fmt::Debug,
    ::std::clone::Clone,
    ::std::cmp::PartialEq,
    ::esexpr::ESExprCodec
)]
#[constructor = "interface-method-parameter"]
pub struct InterfaceMethodParameter {
    pub name: ::noble_idl_runtime::String,
    pub parameter_type: crate::TypeExpr,
    #[keyword = "annotations"]
    pub annotations: ::noble_idl_runtime::List<crate::Annotation>,
}
#[derive(
    ::std::fmt::Debug,
    ::std::clone::Clone,
    ::std::cmp::PartialEq,
    ::esexpr::ESExprCodec
)]
#[constructor = "options"]
pub struct NobleIdlCompileModelOptions {
    #[keyword = "library-files"]
    pub library_files: ::noble_idl_runtime::List<::noble_idl_runtime::String>,
    #[keyword = "files"]
    pub files: ::noble_idl_runtime::List<::noble_idl_runtime::String>,
}
#[derive(
    ::std::fmt::Debug,
    ::std::clone::Clone,
    ::std::cmp::PartialEq,
    ::esexpr::ESExprCodec
)]
pub enum NobleIdlCompileModelResult {
    #[constructor = "success"]
    Success(crate::NobleIdlModel),
    #[constructor = "failure"]
    Failure { #[vararg] errors: ::noble_idl_runtime::List<::noble_idl_runtime::String> },
}
#[derive(
    ::std::fmt::Debug,
    ::std::clone::Clone,
    ::std::cmp::PartialEq,
    ::esexpr::ESExprCodec
)]
#[constructor = "noble-idl-generation-request"]
pub struct NobleIdlGenerationRequest<L> {
    #[keyword = "language-options"]
    pub language_options: L,
    #[keyword = "model"]
    pub model: crate::NobleIdlModel,
}
#[derive(
    ::std::fmt::Debug,
    ::std::clone::Clone,
    ::std::cmp::PartialEq,
    ::esexpr::ESExprCodec
)]
#[constructor = "noble-idl-generation-result"]
pub struct NobleIdlGenerationResult {
    #[keyword = "generated-files"]
    pub generated_files: ::noble_idl_runtime::List<::noble_idl_runtime::String>,
}
#[derive(
    ::std::fmt::Debug,
    ::std::clone::Clone,
    ::std::cmp::PartialEq,
    ::esexpr::ESExprCodec
)]
#[constructor = "noble-idl-model"]
pub struct NobleIdlModel {
    #[keyword = "definitions"]
    pub definitions: ::noble_idl_runtime::List<crate::DefinitionInfo>,
}
#[derive(
    ::std::fmt::Debug,
    ::std::clone::Clone,
    ::std::cmp::PartialEq,
    std::hash::Hash,
    std::cmp::Eq,
    std::cmp::PartialOrd,
    std::cmp::Ord,
    ::esexpr::ESExprCodec
)]
#[constructor = "package-name"]
pub struct PackageName(
    #[vararg]
    pub ::noble_idl_runtime::List<::noble_idl_runtime::String>,
);
#[derive(
    ::std::fmt::Debug,
    ::std::clone::Clone,
    ::std::cmp::PartialEq,
    std::hash::Hash,
    std::cmp::Eq,
    std::cmp::PartialOrd,
    std::cmp::Ord,
    ::esexpr::ESExprCodec
)]
#[constructor = "qualified-name"]
pub struct QualifiedName(pub crate::PackageName, pub ::noble_idl_runtime::String);
#[derive(
    ::std::fmt::Debug,
    ::std::clone::Clone,
    ::std::cmp::PartialEq,
    ::esexpr::ESExprCodec
)]
#[constructor = "record-definition"]
pub struct RecordDefinition {
    #[vararg]
    pub fields: ::noble_idl_runtime::List<crate::RecordField>,
    #[keyword = "esexpr-options"]
    #[optional]
    pub esexpr_options: ::noble_idl_runtime::OptionalField<crate::EsexprRecordOptions>,
}
#[derive(
    ::std::fmt::Debug,
    ::std::clone::Clone,
    ::std::cmp::PartialEq,
    ::esexpr::ESExprCodec
)]
#[constructor = "record-field"]
pub struct RecordField {
    pub name: ::noble_idl_runtime::String,
    pub field_type: crate::TypeExpr,
    #[keyword = "annotations"]
    pub annotations: ::noble_idl_runtime::List<crate::Annotation>,
    #[keyword = "esexpr-options"]
    #[optional]
    pub esexpr_options: ::noble_idl_runtime::OptionalField<
        crate::EsexprRecordFieldOptions,
    >,
}
#[derive(
    ::std::fmt::Debug,
    ::std::clone::Clone,
    ::std::cmp::PartialEq,
    ::esexpr::ESExprCodec
)]
#[constructor = "simple-enum-case"]
pub struct SimpleEnumCase {
    pub name: ::noble_idl_runtime::String,
    #[keyword = "esexpr-options"]
    #[optional]
    pub esexpr_options: ::noble_idl_runtime::OptionalField<
        crate::EsexprSimpleEnumCaseOptions,
    >,
    #[keyword = "annotations"]
    pub annotations: ::noble_idl_runtime::List<crate::Annotation>,
}
#[derive(
    ::std::fmt::Debug,
    ::std::clone::Clone,
    ::std::cmp::PartialEq,
    ::esexpr::ESExprCodec
)]
#[constructor = "simple-enum-definition"]
pub struct SimpleEnumDefinition {
    #[vararg]
    pub cases: ::noble_idl_runtime::List<crate::SimpleEnumCase>,
    #[keyword = "esexpr-options"]
    #[optional]
    pub esexpr_options: ::noble_idl_runtime::OptionalField<
        crate::EsexprSimpleEnumOptions,
    >,
}
#[derive(
    ::std::fmt::Debug,
    ::std::clone::Clone,
    ::std::cmp::PartialEq,
    ::esexpr::ESExprCodec
)]
pub enum TypeExpr {
    #[constructor = "defined-type"]
    DefinedType(
        crate::QualifiedName,
        #[vararg]
        ::noble_idl_runtime::List<crate::TypeExpr>,
    ),
    #[constructor = "type-parameter"]
    TypeParameter(::noble_idl_runtime::String),
}
#[derive(
    ::std::fmt::Debug,
    ::std::clone::Clone,
    ::std::cmp::PartialEq,
    ::esexpr::ESExprCodec
)]
pub enum TypeParameter {
    #[constructor = "type"]
    Type {
        name: ::noble_idl_runtime::String,
        #[keyword = "annotations"]
        annotations: ::noble_idl_runtime::List<crate::Annotation>,
    },
}

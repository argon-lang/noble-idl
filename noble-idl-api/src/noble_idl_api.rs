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
#[allow(non_camel_case_types)]
#[derive(
    ::std::fmt::Debug,
    ::std::clone::Clone,
    ::std::cmp::PartialEq,
    ::esexpr::ESExprCodec
)]
pub enum Definition {
    #[inline_value]
    Record(::std::boxed::Box<crate::RecordDefinition>),
    #[inline_value]
    Enum(::std::boxed::Box<crate::EnumDefinition>),
    #[inline_value]
    SimpleEnum(::std::boxed::Box<crate::SimpleEnumDefinition>),
    #[inline_value]
    ExternType(::std::boxed::Box<crate::ExternTypeDefinition>),
    #[inline_value]
    Interface(::std::boxed::Box<crate::InterfaceDefinition>),
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
    pub name: ::std::boxed::Box<crate::QualifiedName>,
    #[keyword = "type-parameters"]
    pub type_parameters: ::noble_idl_runtime::List<
        ::std::boxed::Box<crate::TypeParameter>,
    >,
    #[keyword = "definition"]
    pub definition: ::std::boxed::Box<crate::Definition>,
    #[keyword = "annotations"]
    pub annotations: ::noble_idl_runtime::List<::std::boxed::Box<crate::Annotation>>,
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
    pub fields: ::noble_idl_runtime::List<::std::boxed::Box<crate::RecordField>>,
    #[keyword = "esexpr-options"]
    #[optional]
    pub esexpr_options: ::noble_idl_runtime::OptionalField<
        ::std::boxed::Box<crate::EsexprEnumCaseOptions>,
    >,
    #[keyword = "annotations"]
    pub annotations: ::noble_idl_runtime::List<::std::boxed::Box<crate::Annotation>>,
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
    pub cases: ::noble_idl_runtime::List<::std::boxed::Box<crate::EnumCase>>,
    #[keyword = "esexpr-options"]
    #[optional]
    pub esexpr_options: ::noble_idl_runtime::OptionalField<
        ::std::boxed::Box<crate::EsexprEnumOptions>,
    >,
}
#[allow(non_camel_case_types)]
#[derive(
    ::std::fmt::Debug,
    ::std::clone::Clone,
    ::std::cmp::PartialEq,
    ::esexpr::ESExprCodec
)]
pub enum EsexprAnnEnum {
    #[constructor = "derive-codec"]
    DeriveCodec,
}
#[allow(non_camel_case_types)]
#[derive(
    ::std::fmt::Debug,
    ::std::clone::Clone,
    ::std::cmp::PartialEq,
    ::esexpr::ESExprCodec
)]
pub enum EsexprAnnEnumCase {
    #[constructor = "constructor"]
    Constructor(::noble_idl_runtime::String),
    #[constructor = "inline-value"]
    InlineValue,
}
#[allow(non_camel_case_types)]
#[derive(
    ::std::fmt::Debug,
    ::std::clone::Clone,
    ::std::cmp::PartialEq,
    ::esexpr::ESExprCodec
)]
pub enum EsexprAnnExternType {
    #[constructor = "derive-codec"]
    DeriveCodec,
    #[constructor = "allow-optional"]
    AllowOptional(::std::boxed::Box<crate::TypeExpr>),
    #[constructor = "allow-vararg"]
    AllowVararg(::std::boxed::Box<crate::TypeExpr>),
    #[constructor = "allow-dict"]
    AllowDict(::std::boxed::Box<crate::TypeExpr>),
    #[inline_value]
    Literals(::std::boxed::Box<crate::EsexprExternTypeLiterals>),
}
#[allow(non_camel_case_types)]
#[derive(
    ::std::fmt::Debug,
    ::std::clone::Clone,
    ::std::cmp::PartialEq,
    ::esexpr::ESExprCodec
)]
pub enum EsexprAnnRecord {
    #[constructor = "derive-codec"]
    DeriveCodec,
    #[constructor = "constructor"]
    Constructor(::noble_idl_runtime::String),
}
#[allow(non_camel_case_types)]
#[derive(
    ::std::fmt::Debug,
    ::std::clone::Clone,
    ::std::cmp::PartialEq,
    ::esexpr::ESExprCodec
)]
pub enum EsexprAnnRecordField {
    #[constructor = "keyword"]
    Keyword(#[optional] ::noble_idl_runtime::OptionalField<::noble_idl_runtime::String>),
    #[constructor = "dict"]
    Dict,
    #[constructor = "vararg"]
    Vararg,
    #[constructor = "optional"]
    Optional,
    #[constructor = "default-value"]
    DefaultValue(::noble_idl_runtime::Esexpr),
}
#[allow(non_camel_case_types)]
#[derive(
    ::std::fmt::Debug,
    ::std::clone::Clone,
    ::std::cmp::PartialEq,
    ::esexpr::ESExprCodec
)]
pub enum EsexprAnnSimpleEnum {
    #[constructor = "derive-codec"]
    DeriveCodec,
}
#[allow(non_camel_case_types)]
#[derive(
    ::std::fmt::Debug,
    ::std::clone::Clone,
    ::std::cmp::PartialEq,
    ::esexpr::ESExprCodec
)]
pub enum EsexprAnnSimpleEnumCase {
    #[constructor = "constructor"]
    Constructor(::noble_idl_runtime::String),
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
    pub value: ::std::boxed::Box<crate::EsexprDecodedValue>,
}
#[allow(non_camel_case_types)]
#[derive(
    ::std::fmt::Debug,
    ::std::clone::Clone,
    ::std::cmp::PartialEq,
    ::esexpr::ESExprCodec
)]
pub enum EsexprDecodedValue {
    #[constructor = "record"]
    Record {
        t: ::std::boxed::Box<crate::TypeExpr>,
        #[vararg]
        fields: ::noble_idl_runtime::List<
            ::std::boxed::Box<crate::EsexprDecodedFieldValue>,
        >,
    },
    #[constructor = "enum"]
    Enum {
        t: ::std::boxed::Box<crate::TypeExpr>,
        case_name: ::noble_idl_runtime::String,
        #[vararg]
        fields: ::noble_idl_runtime::List<
            ::std::boxed::Box<crate::EsexprDecodedFieldValue>,
        >,
    },
    #[constructor = "optional"]
    Optional {
        t: ::std::boxed::Box<crate::TypeExpr>,
        element_type: ::std::boxed::Box<crate::TypeExpr>,
        #[optional]
        value: ::noble_idl_runtime::OptionalField<
            ::std::boxed::Box<crate::EsexprDecodedValue>,
        >,
    },
    #[constructor = "vararg"]
    Vararg {
        t: ::std::boxed::Box<crate::TypeExpr>,
        element_type: ::std::boxed::Box<crate::TypeExpr>,
        #[vararg]
        values: ::noble_idl_runtime::List<::std::boxed::Box<crate::EsexprDecodedValue>>,
    },
    #[constructor = "dict"]
    Dict {
        t: ::std::boxed::Box<crate::TypeExpr>,
        element_type: ::std::boxed::Box<crate::TypeExpr>,
        #[dict]
        values: ::noble_idl_runtime::Dict<::std::boxed::Box<crate::EsexprDecodedValue>>,
    },
    #[constructor = "build-from"]
    BuildFrom {
        t: ::std::boxed::Box<crate::TypeExpr>,
        from_type: ::std::boxed::Box<crate::TypeExpr>,
        from_value: ::std::boxed::Box<crate::EsexprDecodedValue>,
    },
    #[constructor = "from-bool"]
    FromBool { t: ::std::boxed::Box<crate::TypeExpr>, b: ::noble_idl_runtime::Bool },
    #[constructor = "from-int"]
    FromInt {
        t: ::std::boxed::Box<crate::TypeExpr>,
        i: ::noble_idl_runtime::Int,
        #[keyword = "min-int"]
        #[optional]
        min_int: ::noble_idl_runtime::OptionalField<::noble_idl_runtime::Int>,
        #[keyword = "max-int"]
        #[optional]
        max_int: ::noble_idl_runtime::OptionalField<::noble_idl_runtime::Int>,
    },
    #[constructor = "from-str"]
    FromStr { t: ::std::boxed::Box<crate::TypeExpr>, s: ::noble_idl_runtime::String },
    #[constructor = "from-binary"]
    FromBinary { t: ::std::boxed::Box<crate::TypeExpr>, b: ::noble_idl_runtime::Binary },
    #[constructor = "from-float32"]
    FromFloat32 { t: ::std::boxed::Box<crate::TypeExpr>, f: ::noble_idl_runtime::F32 },
    #[constructor = "from-float64"]
    FromFloat64 { t: ::std::boxed::Box<crate::TypeExpr>, f: ::noble_idl_runtime::F64 },
    #[constructor = "from-null"]
    FromNull { t: ::std::boxed::Box<crate::TypeExpr> },
}
#[derive(
    ::std::fmt::Debug,
    ::std::clone::Clone,
    ::std::cmp::PartialEq,
    ::esexpr::ESExprCodec
)]
#[constructor = "enum-case-options"]
pub struct EsexprEnumCaseOptions {
    pub case_type: ::std::boxed::Box<crate::EsexprEnumCaseType>,
}
#[allow(non_camel_case_types)]
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
    pub build_literal_from: ::noble_idl_runtime::OptionalField<
        ::std::boxed::Box<crate::TypeExpr>,
    >,
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
    pub allow_optional: ::noble_idl_runtime::OptionalField<
        ::std::boxed::Box<crate::TypeExpr>,
    >,
    #[keyword = "allow-vararg"]
    #[optional]
    pub allow_vararg: ::noble_idl_runtime::OptionalField<
        ::std::boxed::Box<crate::TypeExpr>,
    >,
    #[keyword = "allow-dict"]
    #[optional]
    pub allow_dict: ::noble_idl_runtime::OptionalField<
        ::std::boxed::Box<crate::TypeExpr>,
    >,
    #[keyword = "literals"]
    pub literals: ::std::boxed::Box<crate::EsexprExternTypeLiterals>,
}
#[allow(non_camel_case_types)]
#[derive(
    ::std::fmt::Debug,
    ::std::clone::Clone,
    ::std::cmp::PartialEq,
    ::esexpr::ESExprCodec
)]
pub enum EsexprRecordFieldKind {
    #[constructor = "positional"]
    Positional(::std::boxed::Box<crate::EsexprRecordPositionalMode>),
    #[constructor = "keyword"]
    Keyword(
        ::noble_idl_runtime::String,
        ::std::boxed::Box<crate::EsexprRecordKeywordMode>,
    ),
    #[constructor = "dict"]
    Dict(::std::boxed::Box<crate::TypeExpr>),
    #[constructor = "vararg"]
    Vararg(::std::boxed::Box<crate::TypeExpr>),
}
#[derive(
    ::std::fmt::Debug,
    ::std::clone::Clone,
    ::std::cmp::PartialEq,
    ::esexpr::ESExprCodec
)]
#[constructor = "field-options"]
pub struct EsexprRecordFieldOptions {
    pub kind: ::std::boxed::Box<crate::EsexprRecordFieldKind>,
}
#[allow(non_camel_case_types)]
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
    Optional(::std::boxed::Box<crate::TypeExpr>),
    #[constructor = "default-value"]
    DefaultValue(::std::boxed::Box<crate::EsexprDecodedValue>),
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
#[allow(non_camel_case_types)]
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
    Optional(::std::boxed::Box<crate::TypeExpr>),
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
        ::std::boxed::Box<crate::EsexprExternTypeOptions>,
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
    pub methods: ::noble_idl_runtime::List<::std::boxed::Box<crate::InterfaceMethod>>,
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
    pub type_parameters: ::noble_idl_runtime::List<
        ::std::boxed::Box<crate::TypeParameter>,
    >,
    #[keyword = "parameters"]
    pub parameters: ::noble_idl_runtime::List<
        ::std::boxed::Box<crate::InterfaceMethodParameter>,
    >,
    #[keyword = "return-type"]
    pub return_type: ::std::boxed::Box<crate::TypeExpr>,
    #[keyword = "annotations"]
    pub annotations: ::noble_idl_runtime::List<::std::boxed::Box<crate::Annotation>>,
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
    pub parameter_type: ::std::boxed::Box<crate::TypeExpr>,
    #[keyword = "annotations"]
    pub annotations: ::noble_idl_runtime::List<::std::boxed::Box<crate::Annotation>>,
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
#[allow(non_camel_case_types)]
#[derive(
    ::std::fmt::Debug,
    ::std::clone::Clone,
    ::std::cmp::PartialEq,
    ::esexpr::ESExprCodec
)]
pub enum NobleIdlCompileModelResult {
    #[constructor = "success"]
    Success(::std::boxed::Box<crate::NobleIdlModel>),
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
    pub model: ::std::boxed::Box<crate::NobleIdlModel>,
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
    pub definitions: ::noble_idl_runtime::List<::std::boxed::Box<crate::DefinitionInfo>>,
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
pub struct QualifiedName(
    pub ::std::boxed::Box<crate::PackageName>,
    pub ::noble_idl_runtime::String,
);
#[derive(
    ::std::fmt::Debug,
    ::std::clone::Clone,
    ::std::cmp::PartialEq,
    ::esexpr::ESExprCodec
)]
#[constructor = "record-definition"]
pub struct RecordDefinition {
    #[vararg]
    pub fields: ::noble_idl_runtime::List<::std::boxed::Box<crate::RecordField>>,
    #[keyword = "esexpr-options"]
    #[optional]
    pub esexpr_options: ::noble_idl_runtime::OptionalField<
        ::std::boxed::Box<crate::EsexprRecordOptions>,
    >,
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
    pub field_type: ::std::boxed::Box<crate::TypeExpr>,
    #[keyword = "annotations"]
    pub annotations: ::noble_idl_runtime::List<::std::boxed::Box<crate::Annotation>>,
    #[keyword = "esexpr-options"]
    #[optional]
    pub esexpr_options: ::noble_idl_runtime::OptionalField<
        ::std::boxed::Box<crate::EsexprRecordFieldOptions>,
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
        ::std::boxed::Box<crate::EsexprSimpleEnumCaseOptions>,
    >,
    #[keyword = "annotations"]
    pub annotations: ::noble_idl_runtime::List<::std::boxed::Box<crate::Annotation>>,
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
    pub cases: ::noble_idl_runtime::List<::std::boxed::Box<crate::SimpleEnumCase>>,
    #[keyword = "esexpr-options"]
    #[optional]
    pub esexpr_options: ::noble_idl_runtime::OptionalField<
        ::std::boxed::Box<crate::EsexprSimpleEnumOptions>,
    >,
}
#[allow(non_camel_case_types)]
#[derive(
    ::std::fmt::Debug,
    ::std::clone::Clone,
    ::std::cmp::PartialEq,
    ::esexpr::ESExprCodec
)]
pub enum TypeExpr {
    #[constructor = "defined-type"]
    DefinedType(
        ::std::boxed::Box<crate::QualifiedName>,
        #[vararg]
        ::noble_idl_runtime::List<::std::boxed::Box<crate::TypeExpr>>,
    ),
    #[constructor = "type-parameter"]
    TypeParameter(::noble_idl_runtime::String),
}
#[allow(non_camel_case_types)]
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
        annotations: ::noble_idl_runtime::List<::std::boxed::Box<crate::Annotation>>,
    },
}

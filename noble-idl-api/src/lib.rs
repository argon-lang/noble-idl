use esexpr::*;
use num_bigint::BigInt;

use std::borrow::Borrow;
use std::fmt::Debug;
use std::hash::Hash;
use std::collections::HashMap;

use noble_idl_runtime::Binary;

pub trait NobleIDLPluginExecutor {
    type LanguageOptions: Clone + Debug;
    type Error: Debug;

    fn generate(&self, request: NobleIDLGenerationRequest<Self::LanguageOptions>) -> Result<NobleIDLGenerationResult, Self::Error>;
}


#[derive(ESExprCodec, Clone, Debug, PartialEq)]
pub struct NobleIDLGenerationRequest<L> {
    #[keyword]
    pub language_options: L,

    #[keyword]
    pub model: NobleIDLModel,
}



#[derive(ESExprCodec, Debug, Clone, PartialEq)]
pub struct NobleIDLGenerationResult {
    #[keyword]
    pub generated_files: Vec<String>,
}


#[derive(ESExprCodec, Clone, Debug, PartialEq)]
#[constructor = "options"]
pub struct NobleIDLCompileModelOptions {
    #[keyword]
    pub library_files: Vec<String>,

    #[keyword]
    pub files: Vec<String>,
}

#[derive(ESExprCodec, Clone, Debug, PartialEq)]
pub enum NobleIDLCompileModelResult {
    Success(NobleIDLModel),
    Failure {
        #[vararg]
        errors: Vec<String>,
    },
}




#[derive(ESExprCodec, Clone, Debug, PartialEq)]
pub struct NobleIDLModel {
    #[keyword]
    pub definitions: Vec<DefinitionInfo>,
}


#[derive(ESExprCodec, Clone, Debug, PartialEq)]
pub struct DefinitionInfo {
    #[keyword]
    pub name: QualifiedName,

    #[keyword]
    pub type_parameters: Vec<TypeParameter>,

    #[keyword]
    pub definition: Definition,

    #[keyword]
    pub annotations: Vec<Annotation>,

	#[keyword]
	pub is_library: bool,
}

#[derive(ESExprCodec, Debug, PartialEq, Eq, Hash, Clone)]
pub struct PackageName(#[vararg] pub Vec<String>);

impl PackageName {
    pub fn from_str(s: &str) -> Self {
        if s.is_empty() {
            Self(Vec::new())
        }
        else {
            Self(s.split(".").map(str::to_owned).collect())
        }
    }
}

#[derive(ESExprCodec, Debug, PartialEq, Eq, Hash, Clone)]
pub struct QualifiedName(pub PackageName, pub String);

impl QualifiedName {
    pub fn package_name(&self) -> &PackageName {
        &self.0
    }

    pub fn name(&self) -> &str {
        &self.1
    }
}

#[derive(ESExprCodec, Clone, Debug, PartialEq)]
pub enum Definition {
    #[inline_value]
    Record(RecordDefinition),

    #[inline_value]
    Enum(EnumDefinition),

	#[inline_value]
    ExternType(ExternTypeDefinition),

    #[inline_value]
    Interface(InterfaceDefinition),
}

#[derive(ESExprCodec, Clone, Debug, PartialEq)]
pub struct RecordDefinition {
    #[vararg]
    pub fields: Vec<RecordField>,

	#[keyword]
	#[optional]
	pub esexpr_options: Option<ESExprRecordOptions>,
}

#[derive(ESExprCodec, Clone, Debug, PartialEq)]
pub struct RecordField {
    pub name: String,
    pub field_type: TypeExpr,

    #[keyword]
    pub annotations: Vec<Annotation>,

	#[keyword]
	#[optional]
	pub esexpr_options: Option<ESExprRecordFieldOptions>,
}


#[derive(ESExprCodec, Clone, Debug, PartialEq)]
pub struct EnumDefinition {
    #[vararg]
    pub cases: Vec<EnumCase>,

	#[keyword]
	#[optional]
	pub esexpr_options: Option<ESExprEnumOptions>,
}

#[derive(ESExprCodec, Clone, Debug, PartialEq)]
pub struct EnumCase {
    pub name: String,

    #[vararg]
    pub fields: Vec<RecordField>,

    #[keyword]
    pub annotations: Vec<Annotation>,

	#[keyword]
	#[optional]
	pub esexpr_options: Option<ESExprEnumCaseOptions>,
}

#[derive(ESExprCodec, Clone, Debug, PartialEq)]
pub struct ExternTypeDefinition {
	#[keyword]
	#[optional]
	pub esexpr_options: Option<ESExprExternTypeOptions>,
}

#[derive(ESExprCodec, Clone, Debug, PartialEq)]
pub struct InterfaceDefinition {
    #[vararg]
    pub methods: Vec<InterfaceMethod>,
}

#[derive(ESExprCodec, Clone, Debug, PartialEq)]
pub struct InterfaceMethod {
    #[keyword]
    pub name: String,

    #[keyword]
    pub type_parameters: Vec<TypeParameter>,

    #[keyword]
    pub parameters: Vec<InterfaceMethodParameter>,

    #[keyword]
    pub return_type: TypeExpr,

    #[keyword]
    pub annotations: Vec<Annotation>,
}

#[derive(ESExprCodec, Clone, Debug, PartialEq)]
pub struct InterfaceMethodParameter {
    pub name: String,
    pub parameter_type: TypeExpr,

    #[keyword]
    pub annotations: Vec<Annotation>,
}

#[derive(ESExprCodec, Debug, PartialEq, Clone)]
pub struct Annotation {
    pub scope: String,
    pub value: ESExpr,
}




#[derive(ESExprCodec, Debug, PartialEq, Clone)]
pub enum TypeExpr {
    DefinedType(QualifiedName, Vec<TypeExpr>),
    TypeParameter(String),
}

impl TypeExpr {
	pub fn substitute<S: AsRef<str> + Borrow<str> + Hash + Eq, TE: Borrow<TypeExpr>>(&mut self, mapping: &HashMap<S, TE>) -> bool
	{
		match self {
			TypeExpr::DefinedType(_, args) => args.iter_mut().all(|arg| arg.substitute(mapping)),
			TypeExpr::TypeParameter(name) => {
				let Some(mapped_type) = mapping.get(name) else { return false; };
				*self = mapped_type.borrow().clone();
				true
			},
		}
	}
}

#[derive(ESExprCodec, Debug, Clone, PartialEq)]
pub enum TypeParameter {
    Type(String),
}

impl TypeParameter {
    pub fn name(&self) -> &str {
        match self {
            TypeParameter::Type(name) => name,
        }
    }
}


// ESExpr options
#[derive(ESExprCodec, Debug, PartialEq, Clone)]
#[constructor = "record-options"]
pub struct ESExprRecordOptions {
	pub constructor: String,
}


#[derive(ESExprCodec, Debug, PartialEq, Clone)]
#[constructor = "enum-options"]
pub struct ESExprEnumOptions {
	#[keyword]
	#[default_value = false]
	pub simple_enum: bool,
}

#[derive(ESExprCodec, Debug, PartialEq, Clone)]
#[constructor = "enum-case-options"]
pub struct ESExprEnumCaseOptions {
	pub case_type: ESExprEnumCaseType,
}

#[derive(ESExprCodec, Debug, PartialEq, Clone)]
pub enum ESExprEnumCaseType {
	Constructor(String),
	InlineValue,
}

#[derive(ESExprCodec, Debug, PartialEq, Clone)]
#[constructor = "extern-type-options"]
pub struct ESExprExternTypeOptions {
	#[keyword]
	#[default_value = false]
	pub allow_value: bool,

	#[keyword]
	#[optional]
	pub allow_optional: Option<TypeExpr>,

	#[keyword]
	#[optional]
	pub allow_vararg: Option<TypeExpr>,

	#[keyword]
	#[optional]
	pub allow_dict: Option<TypeExpr>,

	#[keyword]
	pub literals: ESExprAnnExternTypeLiterals,
}

#[derive(ESExprCodec, Debug, PartialEq, Clone)]
#[constructor = "field-options"]
pub struct ESExprRecordFieldOptions {
	pub kind: ESExprRecordFieldKind,
}

#[derive(ESExprCodec, Debug, PartialEq, Clone)]
pub enum ESExprRecordFieldKind {
	Positional(ESExprRecordPositionalMode),
	Keyword(String, ESExprRecordKeywordMode),
	Dict(TypeExpr),
	Vararg(TypeExpr),
}

#[derive(ESExprCodec, Debug, PartialEq, Clone)]
pub enum ESExprRecordPositionalMode {
	Required,
	Optional(TypeExpr),
}

#[derive(ESExprCodec, Debug, PartialEq, Clone)]
pub enum ESExprRecordKeywordMode {
	Required,
	Optional(TypeExpr),
	DefaultValue(ESExprDecodedValue),
}

#[derive(ESExprCodec, Debug, PartialEq, Clone)]
pub enum ESExprDecodedValue {
	Record(TypeExpr, #[dict] HashMap<String, ESExprDecodedValue>),
	Enum(TypeExpr, String, #[dict] HashMap<String, ESExprDecodedValue>),

	Optional {
		optional_type: TypeExpr,

		#[optional]
		value: Option<Box<ESExprDecodedValue>>,
	},

	Vararg {
		vararg_type: TypeExpr,

		#[vararg]
		values: Vec<ESExprDecodedValue>,
	},

	Dict {
		dict_type: TypeExpr,

		#[dict]
		values: HashMap<String, ESExprDecodedValue>,
	},

	BuildFrom(TypeExpr, Box<ESExprDecodedValue>),

	FromBool(TypeExpr, bool),
	FromInt {
		t: TypeExpr,
		i: BigInt,

		#[keyword]
		#[optional]
		min_int: Option<BigInt>,

		#[keyword]
		#[optional]
		max_int: Option<BigInt>,
	},
	FromStr(TypeExpr, String),
	FromBinary(TypeExpr, Binary),
	FromFloat32(TypeExpr, f32),
	FromFloat64(TypeExpr, f64),
	FromNull(TypeExpr),
}



// ESExpr annotations
#[derive(ESExprCodec, Debug, PartialEq, Clone)]
pub enum ESExprAnnRecord {
    DeriveCodec,
    Constructor(String),
}

#[derive(ESExprCodec, Debug, PartialEq, Clone)]
pub enum ESExprAnnEnum {
    DeriveCodec,
    SimpleEnum,
}

#[derive(ESExprCodec, Debug, PartialEq, Clone)]
pub enum ESExprAnnEnumCase {
    Constructor(String),
    InlineValue,
}

#[derive(ESExprCodec, Debug, PartialEq, Clone)]
pub enum ESExprAnnRecordField {
    Keyword(#[optional] Option<String>),

    Dict,
    Vararg,

	Optional,
	DefaultValue(ESExpr),
}

#[derive(ESExprCodec, Debug, PartialEq, Clone)]
pub enum ESExprAnnExternType {
    DeriveCodec,
	AllowOptional(TypeExpr),
	AllowVararg(TypeExpr),
	AllowDict(TypeExpr),
	#[inline_value]
	Literals(ESExprAnnExternTypeLiterals),
}

#[derive(ESExprCodec, Debug, PartialEq, Clone)]
#[constructor = "literals"]
pub struct ESExprAnnExternTypeLiterals {
	#[keyword]
	#[default_value = false]
	pub allow_bool: bool,

	#[keyword]
	#[default_value = false]
	pub allow_int: bool,

	#[keyword]
	#[optional]
	pub min_int: Option<BigInt>,

	#[keyword]
	#[optional]
	pub max_int: Option<BigInt>,

	#[keyword]
	#[default_value = false]
	pub allow_str: bool,

	#[keyword]
	#[default_value = false]
	pub allow_binary: bool,

	#[keyword]
	#[default_value = false]
	pub allow_float32: bool,

	#[keyword]
	#[default_value = false]
	pub allow_float64: bool,

	#[keyword]
	#[default_value = false]
	pub allow_null: bool,

	#[keyword]
	#[optional]
	pub build_literal_from: Option<TypeExpr>,
}


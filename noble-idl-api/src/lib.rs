use esexpr::*;

use core::fmt::Debug;

pub trait NobleIDLPluginExecutor {
    type LanguageOptions: Clone + Debug;
    type Error: Debug;

    fn generate(&self, model: NobleIDLDefinitions<Self::LanguageOptions>) -> Result<NobleIDLGenerationResult, Self::Error>;
}


#[derive(ESExprCodec, Debug, PartialEq)]
pub struct NobleIDLDefinitions<L> {
    #[keyword]
    pub language_options: L,

    #[keyword]
    pub definitions: Vec<DefinitionInfo>,
}

#[derive(ESExprCodec, Debug, PartialEq)]
pub struct NobleIDLGenerationResult {
    #[keyword]
    pub generated_files: Vec<String>,
}



#[derive(ESExprCodec, Debug, PartialEq)]
pub struct DefinitionInfo {
    #[keyword]
    pub name: QualifiedName,

    #[keyword]
    pub type_parameters: Vec<TypeParameter>,

    #[keyword]
    pub definition: Definition,
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

#[derive(ESExprCodec, Debug, PartialEq)]
pub enum Definition {
    Record(RecordDefinition),
    Enum(EnumDefinition),
    ExternType,
    Interface(InterfaceDefinition),
}

#[derive(ESExprCodec, Debug, PartialEq)]
pub struct RecordDefinition {
    #[vararg]
    pub fields: Vec<RecordField>,
}

#[derive(ESExprCodec, Debug, PartialEq)]
pub struct RecordField {
    pub name: String,
    pub field_type: TypeExpr,
}


#[derive(ESExprCodec, Debug, PartialEq)]
pub struct EnumDefinition {
    pub cases: Vec<EnumCase>,
}

#[derive(ESExprCodec, Debug, PartialEq)]
pub struct EnumCase {
    pub name: String,

    #[vararg]
    pub fields: Vec<RecordField>,
}

#[derive(ESExprCodec, Debug, PartialEq)]
pub struct InterfaceDefinition {
    #[vararg]
    pub methods: Vec<InterfaceMethod>,
}

#[derive(ESExprCodec, Debug, PartialEq)]
pub struct InterfaceMethod {
    #[keyword]
    pub name: String,

    #[keyword]
    pub type_parameters: Vec<TypeParameter>,

    #[keyword]
    pub parameters: Vec<InterfaceMethodParameter>,

    #[keyword]
    pub return_type: TypeExpr,
}

#[derive(ESExprCodec, Debug, PartialEq)]
pub struct InterfaceMethodParameter {
    pub name: String,
    pub parameter_type: TypeExpr,
}






#[derive(ESExprCodec, Debug, PartialEq, Clone)]
pub enum TypeExpr {
    DefinedType(QualifiedName),
    TypeParameter(String),
    Apply(Box<TypeExpr>, Vec<TypeExpr>)
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




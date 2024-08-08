use esexpr::*;

use std::{borrow::Borrow, collections::HashMap, fmt::Debug, hash::Hash};

pub trait NobleIDLPluginExecutor {
    type LanguageOptions: Clone + Debug;
    type Error: Debug;

    fn generate(&self, request: NobleIdlGenerationRequest<Self::LanguageOptions>) -> Result<NobleIdlGenerationResult, Self::Error>;
}


include!("noble_idl_api.rs");

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

impl QualifiedName {
    pub fn package_name(&self) -> &PackageName {
        &self.0
    }

    pub fn name(&self) -> &str {
        &self.1
    }
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

impl TypeParameter {
    pub fn name(&self) -> &str {
        match self {
            TypeParameter::Type { name, .. } => name,
        }
    }

    pub fn annotations(&self) -> &Vec<Annotation> {
        match self {
            TypeParameter::Type { annotations, .. } => annotations,
        }
    }
}




// ESExpr annotations
#[derive(ESExprCodec, Debug, PartialEq, Clone)]
pub enum EsexprAnnRecord {
    DeriveCodec,
    Constructor(String),
}

#[derive(ESExprCodec, Debug, PartialEq, Clone)]
pub enum EsexprAnnEnum {
    DeriveCodec,
}

#[derive(ESExprCodec, Debug, PartialEq, Clone)]
pub enum EsexprAnnEnumCase {
    Constructor(String),
    InlineValue,
}

#[derive(ESExprCodec, Debug, PartialEq, Clone)]
pub enum EsexprAnnSimpleEnum {
    DeriveCodec,
}

#[derive(ESExprCodec, Debug, PartialEq, Clone)]
pub enum EsexprAnnSimpleEnumCase {
    Constructor(String),
}

#[derive(ESExprCodec, Debug, PartialEq, Clone)]
pub enum EsexprAnnRecordField {
    Keyword(#[optional] Option<String>),

    Dict,
    Vararg,

	Optional,
	DefaultValue(ESExpr),
}

#[derive(ESExprCodec, Debug, PartialEq, Clone)]
pub enum EsexprAnnExternType {
    DeriveCodec,
	AllowOptional(TypeExpr),
	AllowVararg(TypeExpr),
	AllowDict(TypeExpr),
	#[inline_value]
	Literals(EsexprExternTypeLiterals),
}

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


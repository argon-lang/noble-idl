use noble_idl_api::TypeParameterOwner;
pub use noble_idl_api::{PackageName, QualifiedName, TypeParameter, Annotation, TypeParameterTypeConstraint};

#[derive(Debug, PartialEq)]
pub struct DefinitionFile {
    pub package: PackageName,
    pub imports: Vec<PackageName>,
    pub definitions: Vec<Definition>,
}

#[derive(Debug, PartialEq)]
pub enum Definition {
    Record(RecordDefinition),
    Enum(EnumDefinition),
    SimpleEnum(SimpleEnumDefinition),
    ExternType(ExternTypeDefinition),
    Interface(InterfaceDefinition),
	ExceptionType(ExceptionTypeDefinition),
}

impl Definition {
    pub fn name(&self) -> &str {
        match self {
            Definition::Record(rec) => &rec.name,
            Definition::Enum(e) => &e.name,
            Definition::SimpleEnum(e) => &e.name,
            Definition::ExternType(et) => &et.name,
            Definition::Interface(iface) => &iface.name,
            Definition::ExceptionType(ex) => &ex.name,
        }
    }
}

#[derive(Debug, PartialEq)]
pub struct RecordDefinition {
    pub name: String,
    pub type_parameters: Vec<TypeParameter>,
    pub fields: Vec<RecordField>,
    pub annotations: Vec<Annotation>,
}

impl RecordDefinition {
    pub fn into_api(self, package: PackageName, is_library: bool) -> noble_idl_api::DefinitionInfo {
        noble_idl_api::DefinitionInfo {
            name: Box::new(QualifiedName(Box::new(package), self.name)),
            type_parameters: self.type_parameters.into_iter().map(Box::new).collect(),
            definition: Box::new(noble_idl_api::Definition::Record(Box::new(noble_idl_api::RecordDefinition {
                fields: self.fields.into_iter().map(RecordField::into_api).map(Box::new).collect(),
				esexpr_options: None,
            }))),
            annotations: self.annotations.into_iter().map(Box::new).collect(),
			is_library,
        }
    }
}

#[derive(Debug, PartialEq, Clone)]
pub struct RecordField {
    pub name: String,
    pub field_type: TypeExpr,
    pub annotations: Vec<Annotation>,
}

impl RecordField {
    pub fn into_api(self) -> noble_idl_api::RecordField {
        noble_idl_api::RecordField {
            name: self.name,
            field_type: Box::new(self.field_type.into_api()),
            annotations: self.annotations.into_iter().map(Box::new).collect(),
			esexpr_options: None,
        }
    }
}


#[derive(Debug, PartialEq)]
pub struct EnumDefinition {
    pub name: String,
    pub type_parameters: Vec<TypeParameter>,
    pub cases: Vec<EnumCase>,
    pub annotations: Vec<Annotation>,
}

impl EnumDefinition {
    pub fn into_api(self, package: PackageName, is_library: bool) -> noble_idl_api::DefinitionInfo {
        noble_idl_api::DefinitionInfo {
            name: Box::new(QualifiedName(Box::new(package), self.name)),
            type_parameters: self.type_parameters.into_iter().map(Box::new).collect(),
            definition: Box::new(noble_idl_api::Definition::Enum(Box::new(noble_idl_api::EnumDefinition {
                cases: self.cases.into_iter().map(EnumCase::into_api).map(Box::new).collect(),
				esexpr_options: None,
            }))),
            annotations: self.annotations.into_iter().map(Box::new).collect(),
			is_library,
        }
    }
}

#[derive(Debug, PartialEq)]
pub struct EnumCase {
    pub name: String,
    pub fields: Vec<RecordField>,
    pub annotations: Vec<Annotation>,
}

impl EnumCase {
    pub fn into_api(self) -> noble_idl_api::EnumCase {
        noble_idl_api::EnumCase {
            name: self.name,
            fields: self.fields.into_iter().map(RecordField::into_api).map(Box::new).collect(),
            annotations: self.annotations.into_iter().map(Box::new).collect(),
			esexpr_options: None,
        }
    }
}


#[derive(Debug, PartialEq)]
pub struct SimpleEnumDefinition {
    pub name: String,
    pub cases: Vec<SimpleEnumCase>,
    pub annotations: Vec<Annotation>,
}

impl SimpleEnumDefinition {
    pub fn into_api(self, package: PackageName, is_library: bool) -> noble_idl_api::DefinitionInfo {
        noble_idl_api::DefinitionInfo {
            name: Box::new(QualifiedName(Box::new(package), self.name)),
            type_parameters: Vec::new(),
            definition: Box::new(noble_idl_api::Definition::SimpleEnum(Box::new(noble_idl_api::SimpleEnumDefinition {
                cases: self.cases.into_iter().map(SimpleEnumCase::into_api).map(Box::new).collect(),
				esexpr_options: None,
            }))),
            annotations: self.annotations.into_iter().map(Box::new).collect(),
			is_library,
        }
    }
}

#[derive(Debug, PartialEq)]
pub struct SimpleEnumCase {
    pub name: String,
    pub annotations: Vec<Annotation>,
}

impl SimpleEnumCase {
    pub fn into_api(self) -> noble_idl_api::SimpleEnumCase {
        noble_idl_api::SimpleEnumCase {
            name: self.name,
            annotations: self.annotations.into_iter().map(Box::new).collect(),
			esexpr_options: None,
        }
    }
}

#[derive(Debug, PartialEq)]
pub struct ExternTypeDefinition {
    pub name: String,
    pub type_parameters: Vec<TypeParameter>,
    pub annotations: Vec<Annotation>,
}

impl ExternTypeDefinition {
    pub fn into_api(self, package: PackageName, is_library: bool) -> noble_idl_api::DefinitionInfo {
        noble_idl_api::DefinitionInfo {
            name: Box::new(QualifiedName(Box::new(package), self.name)),
            type_parameters: self.type_parameters.into_iter().map(Box::new).collect(),
            definition: Box::new(noble_idl_api::Definition::ExternType(Box::new(noble_idl_api::ExternTypeDefinition {
				esexpr_options: None,
			}))),
            annotations: self.annotations.into_iter().map(Box::new).collect(),
			is_library,
        }
    }
}


#[derive(Debug, PartialEq)]
pub struct InterfaceDefinition {
    pub name: String,
    pub type_parameters: Vec<TypeParameter>,
    pub methods: Vec<InterfaceMethod>,
    pub annotations: Vec<Annotation>,
}

impl InterfaceDefinition {
    pub fn into_api(self, package: PackageName, is_library: bool) -> noble_idl_api::DefinitionInfo {
        noble_idl_api::DefinitionInfo {
            name: Box::new(QualifiedName(Box::new(package), self.name)),
            type_parameters: self.type_parameters.into_iter().map(Box::new).collect(),
            definition: Box::new(noble_idl_api::Definition::Interface(Box::new(noble_idl_api::InterfaceDefinition {
                methods: self.methods.into_iter().map(InterfaceMethod::into_api).map(Box::new).collect(),
            }))),
            annotations: self.annotations.into_iter().map(Box::new).collect(),
			is_library,
        }
    }
}

#[derive(Debug, PartialEq)]
pub struct InterfaceMethod {
    pub name: String,
    pub type_parameters: Vec<TypeParameter>,
    pub parameters: Vec<InterfaceMethodParameter>,
    pub return_type: TypeExpr,
	pub throws: Option<TypeExpr>,
    pub annotations: Vec<Annotation>,
}

impl InterfaceMethod {
    pub fn into_api(self) -> noble_idl_api::InterfaceMethod {
        noble_idl_api::InterfaceMethod {
            name: self.name,
            type_parameters: self.type_parameters.into_iter().map(Box::new).collect(),
            parameters: self.parameters.into_iter().map(InterfaceMethodParameter::into_api).map(Box::new).collect(),
            return_type: Box::new(self.return_type.into_api()),
			throws: self.throws.map(TypeExpr::into_api).map(Box::new),
            annotations: self.annotations.into_iter().map(Box::new).collect(),
        }
    }
}

#[derive(Debug, PartialEq)]
pub struct InterfaceMethodParameter {
    pub name: String,
    pub parameter_type: TypeExpr,
    pub annotations: Vec<Annotation>,
}

impl InterfaceMethodParameter {
    pub fn into_api(self) -> noble_idl_api::InterfaceMethodParameter {
        noble_idl_api::InterfaceMethodParameter {
            name: self.name,
            parameter_type: Box::new(self.parameter_type.into_api()),
            annotations: self.annotations.into_iter().map(Box::new).collect(),
        }
    }
}


#[derive(Debug, PartialEq)]
pub struct ExceptionTypeDefinition {
    pub name: String,
	pub information: TypeExpr,
    pub annotations: Vec<Annotation>,
}

impl ExceptionTypeDefinition {
    pub fn into_api(self, package: PackageName, is_library: bool) -> noble_idl_api::DefinitionInfo {
        noble_idl_api::DefinitionInfo {
            name: Box::new(QualifiedName(Box::new(package), self.name)),
            type_parameters: vec![],
            definition: Box::new(noble_idl_api::Definition::ExceptionType(Box::new(noble_idl_api::ExceptionTypeDefinition {
                information: Box::new(self.information.into_api()),
            }))),
            annotations: self.annotations.into_iter().map(Box::new).collect(),
			is_library,
        }
    }
}

#[derive(Debug, PartialEq, Clone)]
pub enum TypeExpr {
    InvalidType,
    UnresolvedName(QualifiedName, Vec<TypeExpr>),

    DefinedType(QualifiedName, Vec<TypeExpr>),
    TypeParameter { name: String, owner: TypeParameterOwner },
}

impl TypeExpr {
    pub fn into_api(self) -> noble_idl_api::TypeExpr {
        match self {
            TypeExpr::InvalidType => panic!("An invalid type should have been replaced."),
            TypeExpr::UnresolvedName(..) => panic!("An unresolved name should have been replaced."),
            TypeExpr::DefinedType(name, args) => noble_idl_api::TypeExpr::DefinedType(Box::new(name), args.into_iter().map(TypeExpr::into_api).map(Box::new).collect()),
            TypeExpr::TypeParameter { name, owner } => noble_idl_api::TypeExpr::TypeParameter { name, owner },
        }
    }
}


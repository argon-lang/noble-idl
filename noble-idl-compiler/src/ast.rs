pub use noble_idl_api::{PackageName, QualifiedName, TypeParameter, Annotation};

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
    ExternType(ExternTypeDefinition),
    Interface(InterfaceDefinition),
}

impl Definition {
    pub fn name(&self) -> &str {
        match self {
            Definition::Record(rec) => &rec.name,
            Definition::Enum(e) => &e.name,
            Definition::ExternType(et) => &et.name,
            Definition::Interface(iface) => &iface.name,
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
    pub fn into_api(self, package: PackageName) -> noble_idl_api::DefinitionInfo {
        noble_idl_api::DefinitionInfo {
            name: QualifiedName(package, self.name),
            type_parameters: self.type_parameters,
            definition: noble_idl_api::Definition::Record(noble_idl_api::RecordDefinition {
                fields: self.fields.into_iter().map(RecordField::into_api).collect(),
            }),
            annotations: self.annotations,
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
            field_type: self.field_type.into_api(),
            annotations: self.annotations,
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
    pub fn into_api(self, package: PackageName) -> noble_idl_api::DefinitionInfo {
        noble_idl_api::DefinitionInfo {
            name: QualifiedName(package, self.name),
            type_parameters: self.type_parameters,
            definition: noble_idl_api::Definition::Enum(noble_idl_api::EnumDefinition {
                cases: self.cases.into_iter().map(EnumCase::into_api).collect(),
            }),
            annotations: self.annotations,
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
            fields: self.fields.into_iter().map(RecordField::into_api).collect(),
            annotations: self.annotations,
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
    pub fn into_api(self, package: PackageName) -> noble_idl_api::DefinitionInfo {
        noble_idl_api::DefinitionInfo {
            name: QualifiedName(package, self.name),
            type_parameters: self.type_parameters,
            definition: noble_idl_api::Definition::ExternType,
            annotations: self.annotations,
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
    pub fn into_api(self, package: PackageName) -> noble_idl_api::DefinitionInfo {
        noble_idl_api::DefinitionInfo {
            name: QualifiedName(package, self.name),
            type_parameters: self.type_parameters,
            definition: noble_idl_api::Definition::Interface(noble_idl_api::InterfaceDefinition {
                methods: self.methods.into_iter().map(InterfaceMethod::into_api).collect(),
            }),
            annotations: self.annotations,
        }
    }
}

#[derive(Debug, PartialEq)]
pub struct InterfaceMethod {
    pub name: String,
    pub type_parameters: Vec<TypeParameter>,
    pub parameters: Vec<InterfaceMethodParameter>,
    pub return_type: TypeExpr,
    pub annotations: Vec<Annotation>,
}

impl InterfaceMethod {
    pub fn into_api(self) -> noble_idl_api::InterfaceMethod {
        noble_idl_api::InterfaceMethod {
            name: self.name,
            type_parameters: self.type_parameters,
            parameters: self.parameters.into_iter().map(InterfaceMethodParameter::into_api).collect(),
            return_type: self.return_type.into_api(),
            annotations: self.annotations,
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
            parameter_type: self.parameter_type.into_api(),
            annotations: self.annotations,
        }
    }
}

#[derive(Debug, PartialEq, Clone)]
pub enum TypeExpr {
    InvalidType,
    UnresolvedName(QualifiedName),

    DefinedType(QualifiedName),
    TypeParameter(String),
    Apply(Box<TypeExpr>, Vec<TypeExpr>)
}

impl TypeExpr {
    pub fn into_api(self) -> noble_idl_api::TypeExpr {
        match self {
            TypeExpr::InvalidType => panic!("An invalid type should have been replaced."),
            TypeExpr::UnresolvedName(_) => panic!("An unresolved name should have been replaced."),
            TypeExpr::DefinedType(name) => noble_idl_api::TypeExpr::DefinedType(name),
            TypeExpr::TypeParameter(name) => noble_idl_api::TypeExpr::TypeParameter(name),
            TypeExpr::Apply(f, args) => noble_idl_api::TypeExpr::Apply(Box::new(f.into_api()), args.into_iter().map(TypeExpr::into_api).collect()),
        }
    }
}


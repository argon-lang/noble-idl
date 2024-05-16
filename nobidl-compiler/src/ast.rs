
#[derive(Debug, PartialEq, Eq, Clone, Hash)]
pub struct PackageName(pub Vec<String>);

#[derive(Debug, PartialEq, Eq, Clone, Hash)]
pub struct QualifiedName(pub PackageName, pub String);

#[derive(Debug, PartialEq, Eq)]
pub struct DefinitionFile {
    pub package: PackageName,
    pub imports: Vec<PackageName>,
    pub definitions: Vec<Definition>,
}

#[derive(Debug, PartialEq, Eq)]
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

#[derive(Debug, PartialEq, Eq)]
pub struct RecordDefinition {
    pub name: String,
    pub type_parameters: Vec<TypeParameter>,
    pub fields: Vec<RecordField>,
}

#[derive(Debug, PartialEq, Eq)]
pub struct RecordField {
    pub name: String,
    pub field_type: TypeExpr,
}


#[derive(Debug, PartialEq, Eq)]
pub struct EnumDefinition {
    pub name: String,
    pub type_parameters: Vec<TypeParameter>,
    pub cases: Vec<EnumCase>,
}

#[derive(Debug, PartialEq, Eq)]
pub struct EnumCase {
    pub name: String,
    pub fields: Vec<RecordField>,
}

#[derive(Debug, PartialEq, Eq)]
pub struct ExternTypeDefinition {
    pub name: String,
    pub type_parameters: Vec<TypeParameter>,
}


#[derive(Debug, PartialEq, Eq)]
pub struct InterfaceDefinition {
    pub name: String,
    pub type_parameters: Vec<TypeParameter>,
    pub methods: Vec<InterfaceMethod>,
}

#[derive(Debug, PartialEq, Eq)]
pub struct InterfaceMethod {
    pub name: String,
    pub type_parameters: Vec<TypeParameter>,
    pub parameters: Vec<InterfaceMethodParameter>,
    pub return_type: TypeExpr,
}

#[derive(Debug, PartialEq, Eq)]
pub struct InterfaceMethodParameter {
    pub name: String,
    pub parameter_type: TypeExpr,
}






#[derive(Debug, PartialEq, Eq)]
pub enum TypeExpr {
    Name(QualifiedName),
    Apply(Box<TypeExpr>, Vec<TypeExpr>)
}

#[derive(Debug, Clone, PartialEq, Eq)]
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

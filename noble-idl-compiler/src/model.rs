use std::collections::{hash_map, HashMap, HashSet};

use noble_idl_api::NobleIDLDefinitions;

use crate::ast::*;

#[derive(Debug)]
pub enum CheckError {
    UnknownType(QualifiedName),
    DuplicateRecordField(QualifiedName, String),
    DuplicateEnumCase(QualifiedName, String),
    DuplicateEnumCaseField(QualifiedName, String, String),
    DuplicateMethod(QualifiedName, String),
    DuplicateMethodParameter(QualifiedName, String, String),
    DuplicateTypeParameter(QualifiedName, Option<String>, String),
    DuplicateDefinition(QualifiedName),
    TypeInMultiplePackages(String, Vec<PackageName>),
    
    UnexpectedTypeKind {
        expected: TypeKind,
        actual: TypeKind,
    },

    TypeParameterMismatch { expected: Vec<TypeKind>, actual: Vec<TypeKind>, },
}


pub(crate) struct DefinitionInfo {
    pub package: PackageName,
    pub imports: Vec<PackageName>,
    pub def: Definition,
    pub is_library: bool,
}

impl DefinitionInfo {
    fn qualified_name(&self) -> QualifiedName {
        QualifiedName(self.package.clone(), self.def.name().to_owned())
    }

    fn check(&mut self, model: &ModelTypes) -> Result<(), CheckError> {
        if self.is_library {
            return Ok(());
        }

        let scope = GlobalScope {
            package: &self.package,
            imports: &self.imports,
            types: model,
        };

        let full_name = self.qualified_name();

        match &mut self.def {
            Definition::Record(rec) => check_record(&scope, full_name, rec),
            Definition::Enum(e) => check_enum(&scope, full_name, e),
            Definition::ExternType(et) => check_extern_type(&scope, full_name, et),
            Definition::Interface(iface) => check_interface(&scope, full_name, iface),
        }
    }

    fn into_api(self) -> noble_idl_api::DefinitionInfo {
        match self.def {
            Definition::Record(r) => r.into_api(self.package),
            Definition::Enum(e) => e.into_api(self.package),
            Definition::ExternType(ext) => ext.into_api(self.package),
            Definition::Interface(iface) => iface.into_api(self.package),
        }
    }
}


trait TypeScope {
    fn resolve_type(&self, name: QualifiedName) -> Result<TypeExpr, CheckError>;
    fn check_defined_type(&self, name: &QualifiedName) -> Result<TypeKind, CheckError>;
    fn check_type_parameter(&self, name: &str) -> Result<TypeKind, CheckError>;
}


#[derive(Debug, Clone, PartialEq, Eq)]
pub enum TypeKind {
    Star,
    Parameterized(Vec<TypeKind>, Box<TypeKind>),
}

struct DefinitionEntry {
    def: DefinitionInfo,
    kind: TypeKind,
}

struct ModelTypes {
    definitions: HashMap<QualifiedName, TypeKind>,
}

struct GlobalScope<'a> {
    package: &'a PackageName,
    imports: &'a [PackageName],
    types: &'a ModelTypes,
}

impl <'a> TypeScope for GlobalScope<'a> {    
    fn resolve_type(&self, mut full_name: QualifiedName) -> Result<TypeExpr, CheckError> {
        if full_name.0.0.is_empty() {
            full_name.0 = self.package.clone();

            if self.types.definitions.contains_key(&full_name) {
                return Ok(TypeExpr::DefinedType(full_name));
            }

            let mut check_import_package = |mut package| {
                std::mem::swap(&mut package, &mut full_name.0);
                let res = self.types.definitions.contains_key(&full_name);
                std::mem::swap(&mut package, &mut full_name.0);

                if res {
                    Some(package)
                }
                else {
                    None
                }
            };

            if let Some(package) = check_import_package(PackageName(vec!())) {
                full_name.0 = package;
                return Ok(TypeExpr::DefinedType(full_name));
            }


            let mut matching_defs: Vec<_> = self.imports.iter()
                .filter_map(move |package| check_import_package(package.clone()))
                .collect();

            if matching_defs.len() > 0 {
                if matching_defs.len() == 1 {
                    let package = matching_defs.swap_remove(0);
                    full_name.0 = package;
                    Ok(TypeExpr::DefinedType(full_name))
                }
                else {
                    Err(CheckError::TypeInMultiplePackages(
                        full_name.1.clone(),
                        matching_defs.into_iter()
                            .map(|package| package)
                            .collect(),
                    ))
                }
            }
            else {
                full_name.0.0.clear();
                Err(CheckError::UnknownType(full_name.clone()))
            }
        }
        else {
            if self.types.definitions.contains_key(&full_name) {
                Ok(TypeExpr::DefinedType(full_name))
            }
            else {
                Err(CheckError::UnknownType(full_name))
            }
        }
    }
    
    fn check_defined_type(&self, name: &QualifiedName) -> Result<TypeKind, CheckError> {
        if let Some(k) = self.types.definitions.get(name) {
            return Ok(k.clone());
        }
        else {
            Err(CheckError::UnknownType(name.clone()))
        }
    }
    
    fn check_type_parameter(&self, name: &str) -> Result<TypeKind, CheckError> {
        Err(CheckError::UnknownType(QualifiedName(PackageName(Vec::new()), name.to_owned())))
    }
}

struct TypeParameterScope<'a, ParentScope> {
    parent_scope: &'a ParentScope,
    type_parameters: &'a [TypeParameter],
}

impl <'a, ParentScope: TypeScope> TypeScope for TypeParameterScope<'a, ParentScope> {    
    fn resolve_type(&self, name: QualifiedName) -> Result<TypeExpr, CheckError> {
        if name.0.0.is_empty() {
            if self.type_parameters.iter().any(|p| p.name() == name.1) {
                return Ok(TypeExpr::TypeParameter(name.1));
            }
        }

        self.parent_scope.resolve_type(name)
    }
    
    fn check_defined_type(&self, name: &QualifiedName) -> Result<TypeKind, CheckError> {
        self.parent_scope.check_defined_type(name)
    }
    
    fn check_type_parameter(&self, name: &str) -> Result<TypeKind, CheckError> {
        self.type_parameters.iter()
            .find(|p| p.name() == name)
            .map(get_type_parameter_kind)
            .map(Ok)
            .unwrap_or_else(|| self.parent_scope.check_type_parameter(name))
    }
}


pub(crate) struct ModelBuilder {
    definitions: HashMap<QualifiedName, DefinitionEntry>,
}

impl ModelBuilder {
    pub fn new() -> Self {
        ModelBuilder {
            definitions: HashMap::new(),
        }
    }

    pub(crate) fn add_definition(&mut self, def: DefinitionInfo) -> Result<(), CheckError> {
        let name = def.qualified_name();
        
        match self.definitions.entry(name) {
            hash_map::Entry::Occupied(_) => return Err(CheckError::DuplicateDefinition(def.qualified_name())),
            hash_map::Entry::Vacant(ve) => {
                let kind = get_type_def_kind(&def.def);
                ve.insert(DefinitionEntry { def, kind })
            },
        };

        Ok(())
    }

    pub(crate) fn check<L>(self, language_options: L) -> Result<NobleIDLDefinitions<L>, CheckError> {
        let mut types = HashMap::new();
        let mut definitions = HashMap::new();

        for (qual_name, entry) in self.definitions {
            types.insert(qual_name.clone(), entry.kind);
            definitions.insert(qual_name, entry.def);
        }

        let model_types = ModelTypes { 
            definitions: types,
        };

        for def in definitions.values_mut() {
            def.check(&model_types)?;
        }

        let model_definitions = definitions.into_values()
            .filter(|dfn| !dfn.is_library)
            .map(DefinitionInfo::into_api)
            .collect();

        Ok(NobleIDLDefinitions {
            language_options,
            definitions: model_definitions,
        })
    }
}

fn get_type_def_kind(def: &Definition) -> TypeKind {
    match def {
        Definition::Record(rec) => get_params_type_kind(&rec.type_parameters),
        Definition::Enum(e) => get_params_type_kind(&e.type_parameters),
        Definition::ExternType(et) => get_params_type_kind(&et.type_parameters),
        Definition::Interface(iface) => get_params_type_kind(&iface.type_parameters),
    }
}

fn get_params_type_kind(type_parameters: &[TypeParameter]) -> TypeKind {
    if type_parameters.is_empty() {
        TypeKind::Star
    }
    else {
        TypeKind::Parameterized(
            type_parameters.iter().map(get_type_parameter_kind).collect(),
            Box::new(TypeKind::Star),
        )
    }
}

fn get_type_parameter_kind(p: &TypeParameter) -> TypeKind {
    match p {
        TypeParameter::Type(_) => TypeKind::Star,
    }
}






    
fn check_record<Scope: TypeScope>(scope: &Scope, full_name: QualifiedName, rec: &mut RecordDefinition) -> Result<(), CheckError> {
    check_type_parameters(scope, &full_name, None, &rec.type_parameters)?;

    let scope = TypeParameterScope {
        parent_scope: scope,
        type_parameters: &rec.type_parameters,
    };

    let mut field_names = HashSet::new();

    for field in &mut rec.fields {
        if let Some(name) = field_names.replace(field.name.clone()) {
            return Err(CheckError::DuplicateRecordField(full_name, name));
        }

        check_type(&scope, &mut field.field_type, TypeKind::Star)?;
    }

    Ok(())
}

fn check_enum<Scope: TypeScope>(scope: &Scope, full_name: QualifiedName, e: &mut EnumDefinition) -> Result<(), CheckError> {
    check_type_parameters(scope, &full_name, None, &e.type_parameters)?;

    let scope = TypeParameterScope {
        parent_scope: scope,
        type_parameters: &e.type_parameters,
    };

    let mut case_names = HashSet::new();
    for c in &mut e.cases {
        if let Some(name) = case_names.replace(c.name.clone()) {
            return Err(CheckError::DuplicateEnumCase(full_name, name));
        }

        let mut field_names = HashSet::new();
    
        for field in &mut c.fields {
            if let Some(name) = field_names.replace(field.name.clone()) {
                return Err(CheckError::DuplicateEnumCaseField(full_name, c.name.clone(), name));
            }
    
            check_type(&scope, &mut field.field_type, TypeKind::Star)?;
        }
    }

    Ok(())
}

fn check_extern_type<Scope: TypeScope>(scope: &Scope, full_name: QualifiedName, et: &mut ExternTypeDefinition) -> Result<(), CheckError> {
    check_type_parameters(scope, &full_name, None, &et.type_parameters)?;

    Ok(())
}

fn check_interface<Scope: TypeScope>(scope: &Scope, full_name: QualifiedName, iface: &mut InterfaceDefinition) -> Result<(), CheckError> {
    check_type_parameters(scope, &full_name, None, &iface.type_parameters)?;

    let scope = TypeParameterScope {
        parent_scope: scope,
        type_parameters: &iface.type_parameters,
    };

    let mut method_names = HashSet::new();
    for method in &mut iface.methods {
        if let Some(name) = method_names.replace(method.name.clone()) {
            return Err(CheckError::DuplicateMethod(full_name, name));
        }

        if !method.type_parameters.is_empty() {
            let type_parameters =
                iface.type_parameters.iter()
                    .chain(method.type_parameters.iter())
                    .map(|p| p.clone())
                    .collect::<Vec<_>>();

            check_type_parameters(&scope, &full_name, Some(&method.name), &type_parameters)?;
        }

        let scope = TypeParameterScope {
            parent_scope: &scope,
            type_parameters: &method.type_parameters,
        };

        let mut param_names = HashSet::new();
    
        for param in &mut method.parameters {
            if let Some(name) = param_names.replace(param.name.clone()) {
                return Err(CheckError::DuplicateMethodParameter(full_name, method.name.clone(), name));
            }
    
            check_type(&scope, &mut param.parameter_type, TypeKind::Star)?;
        }

        check_type(&scope, &mut method.return_type, TypeKind::Star)?;
    }

    Ok(())
}

fn check_type_parameters<Scope: TypeScope>(_scope: &Scope, full_name: &QualifiedName, method_name: Option<&str>, params: &[TypeParameter]) -> Result<(), CheckError> {
    let mut names = HashSet::new();
    for param in params {
        if let Some(name) = names.replace(param.name().to_owned()) {
            return Err(CheckError::DuplicateTypeParameter(full_name.clone(), method_name.map(|n| n.to_owned()), name));
        }
    }

    Ok(())
}

fn check_type<Scope: TypeScope>(scope: &Scope, t: &mut TypeExpr, expected_kind: TypeKind) -> Result<(), CheckError> {
    let actual = get_type_expr_kind(scope, t)?;
    if actual != expected_kind {
        return Err(CheckError::UnexpectedTypeKind { expected: expected_kind, actual })
    }

    Ok(())
}


fn get_type_expr_kind<Scope: TypeScope>(scope: &Scope, t: &mut TypeExpr) -> Result<TypeKind, CheckError> {
    let mut t2 = TypeExpr::InvalidType;
    std::mem::swap(&mut t2, t);
    match get_type_expr_kind_impl(scope, t2) {
        TypeKindResult::Success(t2, k) => {
            *t = t2;
            Ok(k)
        },
        TypeKindResult::Failure(t2, e) => {
            *t = t2;
            Err(e)
        },
    }
}


enum TypeKindResult {
    Success(TypeExpr, TypeKind),
    Failure(TypeExpr, CheckError),
}

fn get_type_expr_kind_impl<Scope: TypeScope>(scope: &Scope, mut t: TypeExpr) -> TypeKindResult {
    match t {
        TypeExpr::InvalidType => panic!("Unexpected invalid type"),
        
        TypeExpr::UnresolvedName(name) => match scope.resolve_type(name) {
            Ok(t) => get_type_expr_kind_impl(scope, t),
            Err(e) => TypeKindResult::Failure(TypeExpr::InvalidType, e)
        },

        TypeExpr::DefinedType(ref mut name) => match scope.check_defined_type(name) {
            Ok(k) => TypeKindResult::Success(t, k),
            Err(e) => TypeKindResult::Failure(t, e),
        },

        TypeExpr::TypeParameter(ref mut name) => match scope.check_type_parameter(name) {
            Ok(k) => TypeKindResult::Success(t, k),
            Err(e) => TypeKindResult::Failure(t, e),
        },

        TypeExpr::Apply(ref mut f, ref mut args) => {
            let arg_kinds = match args.iter_mut().map(|arg| get_type_expr_kind(scope, arg)).collect::<Result<Vec<_>, _>>() {
                Ok(arg_kinds) => arg_kinds,
                Err(err) => return TypeKindResult::Failure(t, err),
            };

            let f_kind = match get_type_expr_kind(scope, f) {
                Ok(f_kind) => f_kind,
                Err(err) => return TypeKindResult::Failure(t, err),
            };
            match f_kind {
                TypeKind::Star => return TypeKindResult::Failure(t, CheckError::TypeParameterMismatch { expected: vec!(), actual: arg_kinds }),
                TypeKind::Parameterized(params, res) => {
                    if params.len() != arg_kinds.len() {
                        return TypeKindResult::Failure(t, CheckError::TypeParameterMismatch { expected: params, actual: arg_kinds });
                    }

                    for (param, arg) in params.into_iter().zip(arg_kinds.into_iter()) {
                        if param != arg {
                            return TypeKindResult::Failure(t, CheckError::UnexpectedTypeKind { expected: param, actual: arg });
                        }
                    }

                    TypeKindResult::Success(t, *res)
                },
            }
        },
    }
}



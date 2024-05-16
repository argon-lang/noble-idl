use std::collections::{hash_map, HashMap, HashSet};

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
}


trait TypeScope {
    fn lookup_type<'a>(&'a self, full_name: &mut QualifiedName) -> Result<TypeKind, CheckError>;
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
    fn lookup_type<'b>(&'b self, full_name: &mut QualifiedName) -> Result<TypeKind, CheckError> {
        if full_name.0.0.is_empty() {
            let name = &full_name.1;

            let mut qual_name = QualifiedName(self.package.clone(), name.clone());

            if let Some(k) = self.types.definitions.get(&qual_name) {
                *full_name = qual_name;
                return Ok(k.clone());
            }


            let mut check_import_package = |mut package| {
                std::mem::swap(&mut package, &mut qual_name.0);
                let res = self.types.definitions.get(&qual_name)?;
                std::mem::swap(&mut package, &mut qual_name.0);

                Some((package, res))
            };

            if let Some((_, res)) = check_import_package(PackageName(vec!())) {
                return Ok(res.clone());
            }


            let mut matching_defs: Vec<_> = self.imports.iter()
                .filter_map(move |package| check_import_package(package.clone()))
                .collect();

            if matching_defs.len() > 0 {
                if matching_defs.len() == 1 {
                    let (package, def) = matching_defs.remove(0);
                    qual_name.0 = package;
                    *full_name = qual_name;
                    return Ok(def.clone());
                }
                else {
                    return Err(CheckError::TypeInMultiplePackages(
                        name.clone(),
                        matching_defs.into_iter()
                            .map(|(package, _)| package)
                            .collect(),
                    ));
                }
            }
        }
        else {
            if let Some(k) = self.types.definitions.get(full_name) {
                return Ok(k.clone());
            }
        };

        Err(CheckError::UnknownType(full_name.clone()))
    }
}

struct TypeParameterScope<'a, ParentScope> {
    parent_scope: &'a ParentScope,
    type_parameters: &'a [TypeParameter],
}

impl <'a, ParentScope: TypeScope> TypeScope for TypeParameterScope<'a, ParentScope> {
    fn lookup_type<'b>(&'b self, full_name: &mut QualifiedName) -> Result<TypeKind, CheckError> {
        (
            if full_name.0.0.is_empty() {
                let name = &full_name.1;
                self.type_parameters.iter()
                    .find(|p| p.name() == name)
                    .map(get_type_parameter_kind)
                    .map(Ok)
            }
            else {
                None
            }
        )
            .unwrap_or_else(|| self.parent_scope.lookup_type(full_name))        
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

    pub(crate) fn check(self) -> Result<Model, CheckError> {
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

        let mut model_definitions: HashMap<PackageName, Vec<DefinitionInfo>> = HashMap::new();
        for (name, def) in definitions {
            if def.is_library {
                continue;
            }

            let items = model_definitions.entry(name.0).or_default();
            items.push(def);
        }

        Ok(Model {
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
    match t {
        TypeExpr::Name(name) => scope.lookup_type(name),

        TypeExpr::Apply(f, args) => {
            let arg_kinds = args.iter_mut().map(|arg| get_type_expr_kind(scope, arg)).collect::<Result<Vec<_>, _>>()?;

            let f_kind = get_type_expr_kind(scope, f)?;
            match f_kind {
                TypeKind::Star => return Err(CheckError::TypeParameterMismatch { expected: vec!(), actual: arg_kinds }),
                TypeKind::Parameterized(params, res) => {
                    if params.len() != arg_kinds.len() {
                        return Err(CheckError::TypeParameterMismatch { expected: params, actual: arg_kinds });
                    }

                    for (param, arg) in params.into_iter().zip(arg_kinds.into_iter()) {
                        if param != arg {
                            return Err(CheckError::UnexpectedTypeKind { expected: param, actual: arg });
                        }
                    }

                    Ok(*res)
                },
            }
        },
    }
}



pub(crate) struct Model {
    pub definitions: HashMap<PackageName, Vec<DefinitionInfo>>,
}


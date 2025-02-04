use std::collections::{HashMap, HashSet};

use noble_idl_api::{PackageName, QualifiedName, TypeParameter, TypeParameterOwner};

use super::*;



pub fn run(definitions: &mut HashMap<QualifiedName, DefinitionInfo>, type_names: &HashSet<QualifiedName>) -> Result<(), CheckError> {

	let model_types = ModelTypes {
		type_names,
	};

	for (definition_name, def) in definitions {
		let checker = ModelChecker {
			scope: GlobalScope {
				package: &def.package,
				imports: &def.imports,
				types: &model_types,
			},
			definition_name,
		};

		match &mut def.def {
			Definition::Record(rec) => checker.check_record(rec)?,
			Definition::Enum(e) => checker.check_enum(e)?,
			Definition::SimpleEnum(e) => checker.check_simple_enum(e)?,
			Definition::ExternType(et) => checker.check_extern_type(et)?,
			Definition::Interface(iface) => checker.check_interface(iface)?,
			Definition::ExceptionType(ex) => checker.check_exception_type_def(ex)?,
		}
	}

	Ok(())
}



pub trait TypeScope {
    fn resolve_type(&self, name: QualifiedName, args: Vec<TypeExpr>) -> Result<TypeExpr, CheckError>;
}

pub struct ModelTypes<'a> {
    pub type_names: &'a HashSet<QualifiedName>,
}


#[derive(Clone, Copy)]
pub struct GlobalScope<'a> {
    package: &'a PackageName,
    imports: &'a [PackageName],
    types: &'a ModelTypes<'a>,
}

impl <'a> TypeScope for GlobalScope<'a> {
    fn resolve_type(&self, mut full_name: QualifiedName, args: Vec<TypeExpr>) -> Result<TypeExpr, CheckError> {
        if full_name.0.0.is_empty() {
            *full_name.0 = self.package.clone();

            if self.types.type_names.contains(&full_name) {
                return Ok(TypeExpr::DefinedType(full_name, args));
            }

            let mut check_import_package = |mut package| {
                std::mem::swap(&mut package, full_name.0.as_mut());

				let res = self.types.type_names.contains(&full_name);

				std::mem::swap(&mut package, full_name.0.as_mut());

				if res { Some(package) }
				else { None }
            };

            if let Some(package) = check_import_package(PackageName(vec!())) {
                *full_name.0 = package;
                return Ok(TypeExpr::DefinedType(full_name, args));
            }


            let mut matching_defs: Vec<_> = self.imports.iter()
                .filter_map(move |package| check_import_package(package.clone()))
                .collect();

            if matching_defs.len() > 0 {
                if matching_defs.len() == 1 {
                    let package = matching_defs.swap_remove(0);
                    *full_name.0 = package;
                    Ok(TypeExpr::DefinedType(full_name, args))
                }
                else {
                    Err(CheckError::TypeInMultiplePackages(
                        full_name.1.clone(),
                        matching_defs.into_iter()
                            .map(|package| package)
                            .collect::<Vec<_>>(),
                    ))
                }
            }
            else {
                full_name.0.0.clear();
                Err(CheckError::UnknownType(full_name.clone()))
            }
        }
        else {
			if self.types.type_names.contains(&full_name) {
                return Ok(TypeExpr::DefinedType(full_name, args));
            }
            else {
                Err(CheckError::UnknownType(full_name))
            }
        }
    }
}

#[derive(Clone, Copy)]
struct TypeParameterScope<'a, ParentScope> {
    parent_scope: ParentScope,
	owner: TypeParameterOwner,
    type_parameters: &'a [TypeParameter],
}

impl <'a, ParentScope: TypeScope + Copy> TypeScope for TypeParameterScope<'a, ParentScope> {
    fn resolve_type(&self, name: QualifiedName, args: Vec<TypeExpr>) -> Result<TypeExpr, CheckError> {
        if name.0.0.is_empty() {
            if self.type_parameters.iter().any(|p| p.name() == name.1) {
				let expected = 0;
				let actual = args.len();
				if expected != actual {
					return Err(CheckError::TypeParameterMismatch { expected, actual });
				}

                return Ok(TypeExpr::TypeParameter { name: name.1, owner: self.owner });
            }
        }

        self.parent_scope.resolve_type(name, args)
    }
}



struct ModelChecker<'a, Scope> {
    scope: Scope,
	definition_name: &'a QualifiedName,
}

impl <'a, Scope: TypeScope + Copy> ModelChecker<'a, Scope> {
	fn with_type_parameters<'b>(&'b self, type_parameters: &'b [TypeParameter], owner: TypeParameterOwner) -> ModelChecker<'b, TypeParameterScope<'b, Scope>> {
		ModelChecker {
			scope: TypeParameterScope {
				parent_scope: self.scope,
				owner,
				type_parameters,
			},
			definition_name: self.definition_name,
		}
	}

	fn check_record(&self, rec: &mut RecordDefinition) -> Result<(), CheckError> {
		self.check_type_parameters(None, &rec.type_parameters)?;

		let inner = self.with_type_parameters(&rec.type_parameters, TypeParameterOwner::ByType);

		inner.check_fields(None, &mut rec.fields)?;

		Ok(())
	}

	fn check_enum(&self, e: &mut EnumDefinition) -> Result<(), CheckError> {
		self.check_type_parameters( None, &e.type_parameters)?;

		let inner = self.with_type_parameters(&e.type_parameters, TypeParameterOwner::ByType);

		let mut case_names = HashSet::new();

		for c in &mut e.cases {
			if let Some(name) = case_names.replace(c.name.clone()) {
				return Err(CheckError::DuplicateEnumCase(self.definition_name.clone(), name));
			}

			inner.check_fields(Some(c.name.as_str()), &mut c.fields)?;
		}

		Ok(())
	}

	fn check_simple_enum(&self, e: &mut SimpleEnumDefinition) -> Result<(), CheckError> {
		let mut case_names = HashSet::new();

		for c in &mut e.cases {
			if let Some(name) = case_names.replace(c.name.clone()) {
				return Err(CheckError::DuplicateEnumCase(self.definition_name.clone(), name));
			}
		}

		Ok(())
	}

	fn check_fields(&self, case_name: Option<&str>, fields: &mut [RecordField]) -> Result<(), CheckError> {
		let mut field_names = HashSet::new();


		for field in fields {
			if let Some(name) = field_names.replace(field.name.clone()) {
				return Err(CheckError::DuplicateRecordField(self.definition_name.clone(), case_name.map(str::to_owned), name));
			}

			self.check_type(&mut field.field_type)?;
		}

		Ok(())
	}

	fn check_extern_type(&self, et: &mut ExternTypeDefinition) -> Result<(), CheckError> {
		self.check_type_parameters(None, &et.type_parameters)?;

		Ok(())
	}

	fn check_interface(&self, iface: &mut InterfaceDefinition) -> Result<(), CheckError> {
		self.check_type_parameters(None, &iface.type_parameters)?;

		let inner = self.with_type_parameters(&iface.type_parameters, TypeParameterOwner::ByType);

		let mut method_names = HashSet::new();
		for method in &mut iface.methods {
			if let Some(name) = method_names.replace(method.name.clone()) {
				return Err(CheckError::DuplicateMethod(self.definition_name.clone(), name));
			}

			if !method.type_parameters.is_empty() {
				let type_parameters =
					iface.type_parameters.iter()
						.chain(method.type_parameters.iter())
						.map(|p| p.clone())
						.collect::<Vec<_>>();

				self.check_type_parameters(Some(&method.name), &type_parameters)?;
			}

			let inner = inner.with_type_parameters(&method.type_parameters, TypeParameterOwner::ByMethod);

			let mut param_names = HashSet::new();

			for param in &mut method.parameters {
				if let Some(name) = param_names.replace(param.name.clone()) {
					return Err(CheckError::DuplicateMethodParameter(self.definition_name.clone(), method.name.clone(), name));
				}

				inner.check_type(&mut param.parameter_type)?;
			}

			inner.check_type(&mut method.return_type)?;

			if let Some(throws_type) = &mut method.throws {
				inner.check_type(throws_type)?;
			}

		}

		Ok(())
	}

	fn check_exception_type_def(&self, ex: &mut ExceptionTypeDefinition) -> Result<(), CheckError> {
		self.check_type(&mut ex.information)?;

		Ok(())
	}

	fn check_type_parameters(&self, method_name: Option<&str>, params: &[TypeParameter]) -> Result<(), CheckError> {
		let mut names = HashSet::new();
		for param in params {
			if let Some(name) = names.replace(param.name().to_owned()) {
				return Err(CheckError::DuplicateTypeParameter(self.definition_name.clone(), method_name.map(|n| n.to_owned()), name));
			}
		}

		Ok(())
	}

	fn check_type(&self, t: &mut TypeExpr) -> Result<(), CheckError> {
		let mut t2 = TypeExpr::InvalidType;
		std::mem::swap(&mut t2, t);
		match self.check_type_impl(t2) {
			TypeResult::Success(t2) => {
				*t = t2;
				Ok(())
			},
			TypeResult::Failure(t2, e) => {
				*t = t2;
				Err(e)
			},
		}
	}

	fn check_type_impl(&self, t: TypeExpr) -> TypeResult {
		match t {
			TypeExpr::InvalidType => panic!("Unexpected invalid type"),

			TypeExpr::UnresolvedName(name, mut args) => {
				for arg in &mut args {
					if let Some(e) = self.check_type(arg).err() {
						return TypeResult::Failure(TypeExpr::UnresolvedName(name, args), e);
					}
				}

				match self.scope.resolve_type(name, args) {
					Ok(t) => TypeResult::Success(t),
					Err(e) => TypeResult::Failure(TypeExpr::InvalidType, e)
				}
			},

			TypeExpr::DefinedType(..) | TypeExpr::TypeParameter { .. } => TypeResult::Success(t),
		}
	}

}

enum TypeResult {
    Success(TypeExpr),
    Failure(TypeExpr, CheckError),
}



use std::collections::{HashMap, HashSet};

use noble_idl_api::{self as api, QualifiedName, TypeParameter};

use super::*;



pub fn run(definitions: &HashMap<QualifiedName, api::DefinitionInfo>) -> Result<(), CheckError> {
	let scope = GlobalScope {
		definitions,
	};

	for (definition_name, def) in definitions {

		let scope = TypeParameterScope {
			parent_scope: scope,
			type_parameters: &def.type_parameters,
		};

		let checker = ModelChecker {
			scope,
			definition_name,
		};

		checker.check_type_parameters(None, &def.type_parameters)?;

		match &*def.definition {
			api::Definition::Record(rec) => checker.check_record(rec)?,
			api::Definition::Enum(e) => checker.check_enum(e)?,
			api::Definition::SimpleEnum(e) => checker.check_simple_enum(e)?,
			api::Definition::ExternType(et) => checker.check_extern_type(et)?,
			api::Definition::Interface(iface) => checker.check_interface(iface)?,
			api::Definition::ExceptionType(ex) => checker.check_exception_type_def(ex)?,
		}
	}

	Ok(())
}



pub trait TypeScope<'a> {
	fn get_definition(&self, name: &QualifiedName) -> Result<&'a api::DefinitionInfo, CheckError>;
    fn get_type_parameter(&self, name: &str) -> Result<&'a api::TypeParameter, CheckError>;
}


#[derive(Clone, Copy)]
pub struct GlobalScope<'a> {
	definitions: &'a HashMap<QualifiedName, api::DefinitionInfo>
}

impl <'a> TypeScope<'a> for GlobalScope<'a> {
	fn get_definition(&self, name: &QualifiedName) -> Result<&'a noble_idl_api::DefinitionInfo, CheckError> {
		self.definitions.get(name).ok_or_else(|| {
			CheckError::UnknownType(name.clone())
		})
	}

	fn get_type_parameter(&self, name: &str) -> Result<&'a noble_idl_api::TypeParameter, CheckError> {
		Err(CheckError::UnknownType(QualifiedName(Box::new(PackageName(Vec::new())), name.to_owned())))
	}
}

#[derive(Clone, Copy)]
struct TypeParameterScope<'a, ParentScope> {
    parent_scope: ParentScope,
    type_parameters: &'a [Box<TypeParameter>],
}

impl <'a, ParentScope: TypeScope<'a> + Copy> TypeScope<'a> for TypeParameterScope<'a, ParentScope> {
	fn get_definition(&self, name: &QualifiedName) -> Result<&'a noble_idl_api::DefinitionInfo, CheckError> {
		self.parent_scope.get_definition(name)
	}

	fn get_type_parameter(&self, name: &str) -> Result<&'a noble_idl_api::TypeParameter, CheckError> {
		self.type_parameters.iter().find(|tp| tp.name() == name)
			.map(|tp| Ok(tp.as_ref()))
			.unwrap_or_else(|| self.parent_scope.get_type_parameter(name))
	}
}


struct ModelChecker<'a, Scope> {
	scope: Scope,
	definition_name: &'a QualifiedName,
}

impl <'a, Scope: TypeScope<'a> + Copy + 'a> ModelChecker<'a, Scope> {
	fn with_type_parameters(&self, type_parameters: &'a [Box<TypeParameter>]) -> ModelChecker<'a, TypeParameterScope<'a, Scope>> {
		ModelChecker {
			scope: TypeParameterScope {
				parent_scope: self.scope,
				type_parameters,
			},
			definition_name: self.definition_name,
		}
	}

	fn check_record(&self, rec: &api::RecordDefinition) -> Result<(), CheckError> {
		self.check_fields(None, &rec.fields)?;

		Ok(())
	}

	fn check_enum(&self, e: &api::EnumDefinition) -> Result<(), CheckError> {
		let mut case_names = HashSet::new();

		for c in &e.cases {
			if let Some(name) = case_names.replace(c.name.clone()) {
				return Err(CheckError::DuplicateEnumCase(self.definition_name.clone(), name));
			}

			self.check_fields(Some(c.name.as_str()), &c.fields)?;
		}

		Ok(())
	}

	fn check_simple_enum(&self, _e: &api::SimpleEnumDefinition) -> Result<(), CheckError> {
		Ok(())
	}

	fn check_fields(&self, _case_name: Option<&str>, fields: &[Box<api::RecordField>]) -> Result<(), CheckError> {
		for field in fields {
			self.check_type(&field.field_type)?;
		}

		Ok(())
	}

	fn check_extern_type(&self, _et: &api::ExternTypeDefinition) -> Result<(), CheckError> {
		Ok(())
	}

	fn check_interface(&self, iface: &'a api::InterfaceDefinition) -> Result<(), CheckError> {
		for method in &iface.methods {

			let inner = self.with_type_parameters(&method.type_parameters);

			inner.check_type_parameters(Some(&method.name), &method.type_parameters)?;

			for param in &method.parameters {
				self.check_type(&param.parameter_type)?;
			}

			inner.check_type(&method.return_type)?;

			if let Some(throws_type) = method.throws.as_deref() {
				inner.check_exception_type(throws_type)?;
			}

		}

		Ok(())
	}

	fn check_type_parameters(&self, _method_name: Option<&str>, _params: &[Box<api::TypeParameter>]) -> Result<(), CheckError> {
		Ok(())
	}

	fn check_exception_type_def(&self, ex: &api::ExceptionTypeDefinition) -> Result<(), CheckError> {
		self.check_type(&ex.information)?;

		Ok(())
	}

	fn check_exception_type(&self, t: &api::TypeExpr) -> Result<(), CheckError> {
		self.check_type(t)?;

		let is_exception = |dfn: &api::DefinitionInfo| match &*dfn.definition {
			api::Definition::ExceptionType(_) => true,
			_ => false,
		};

		let has_exception_constraint = |tp: &api::TypeParameter| {
			match tp {
				TypeParameter::Type { constraints, .. } =>
					constraints.iter().any(|c| match &**c {
						TypeParameterTypeConstraint::Exception => true,
					}),
			}
		};

		match t {
			api::TypeExpr::DefinedType(name, _) if is_exception(self.scope.get_definition(name)?) => {},
			api::TypeExpr::TypeParameter { name, .. } if has_exception_constraint(self.scope.get_type_parameter(name)?) => {},
			_ => return Err(CheckError::InvalidExceptionType(t.clone())),
		}

		Ok(())
	}

	fn check_type(&self, t: &api::TypeExpr) -> Result<(), CheckError> {
		match t {
			api::TypeExpr::DefinedType(name, args) => {
				let dfn = self.scope.get_definition(name.as_ref())?;

				if dfn.type_parameters.len() != args.len() {
					return Err(CheckError::TypeParameterMismatch { expected: dfn.type_parameters.len(), actual: args.len() });
				}

				for (param, arg) in dfn.type_parameters.iter().map(Box::as_ref).zip(args.iter().map(Box::as_ref)) {
					self.check_type_arg(param, arg)?;
				}
			},

			api::TypeExpr::TypeParameter { .. } => {},
		}

		Ok(())
	}

	fn check_type_arg(&self, param: &api::TypeParameter, arg: &api::TypeExpr) -> Result<(), CheckError> {
		match param {
			TypeParameter::Type { constraints, .. } => {
				for constraint in constraints {
					match &**constraint {
						TypeParameterTypeConstraint::Exception => {
							self.check_exception_type(arg)?;
						},
					}
				}
			},
		}

		Ok(())
	}
}


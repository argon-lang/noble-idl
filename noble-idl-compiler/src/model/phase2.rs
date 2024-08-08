
use std::collections::HashMap;

use esexpr::ESExprCodec;
use noble_idl_api::*;

use super::CheckError;

pub fn run(definitions: &mut HashMap<QualifiedName, DefinitionInfo>) -> Result<ESExprOptionParseExtern, CheckError> {
	let mut parse_extern = ESExprOptionParseExtern {
		optional_container_types: HashMap::new(),
		vararg_container_types: HashMap::new(),
		dict_container_types: HashMap::new(),
	};

	for dfn in definitions.values_mut() {
		parse_extern.scan_definition(dfn)?;
	}

	Ok(parse_extern)
}

pub struct ESExprOptionParseExtern {
	pub optional_container_types: HashMap<QualifiedName, ContainerTypeMetadata>,
	pub vararg_container_types: HashMap<QualifiedName, ContainerTypeMetadata>,
	pub dict_container_types: HashMap<QualifiedName, ContainerTypeMetadata>,
}

impl ESExprOptionParseExtern {

	fn scan_definition(&mut self, dfn: &mut DefinitionInfo) -> Result<(), CheckError> {
		match &mut dfn.definition {
			Definition::Record(_) => {},
			Definition::Enum(_) => {},
			Definition::SimpleEnum(_) => {},
			Definition::ExternType(et) => self.scan_extern_type(&dfn.name, &dfn.annotations, et)?,
			Definition::Interface(_) => {},
		}

		Ok(())
	}

	fn scan_extern_type(&mut self, def_name: &QualifiedName, annotations: &[Annotation], et: &mut ExternTypeDefinition) -> Result<(), CheckError> {
		let mut has_derive_codec = false;
		let mut allow_optional = None;
		let mut allow_vararg = None;
		let mut allow_dict = None;
		let mut literals = None;

		for ann in annotations {
			if ann.scope != "esexpr" {
				continue;
			}

			let esexpr_rec = EsexprAnnExternType::decode_esexpr(ann.value.clone())
				.map_err(|e| CheckError::InvalidESExprAnnotation(def_name.clone(), e))?;

			match esexpr_rec {
				EsexprAnnExternType::DeriveCodec => {
					if has_derive_codec {
						return Err(CheckError::DuplicateESExprAnnotation(def_name.clone(), vec![], "derive-codec".to_owned()));
					}

					has_derive_codec = true;
				},
				EsexprAnnExternType::AllowOptional(element_type) => {
					if allow_optional.is_some() {
						return Err(CheckError::DuplicateESExprAnnotation(def_name.clone(), vec![], "allow-optional".to_owned()));
					}

					allow_optional = Some(element_type.clone());

					self.optional_container_types.insert(def_name.clone(), ContainerTypeMetadata {
						element_type,
					});
				},
				EsexprAnnExternType::AllowVararg(element_type) => {
					if allow_vararg.is_some() {
						return Err(CheckError::DuplicateESExprAnnotation(def_name.clone(), vec![], "allow-vararg".to_owned()));
					}

					allow_vararg = Some(element_type.clone());

					self.vararg_container_types.insert(def_name.clone(), ContainerTypeMetadata {
						element_type,
					});
				},
				EsexprAnnExternType::AllowDict(element_type) => {
					if allow_dict.is_some() {
						return Err(CheckError::DuplicateESExprAnnotation(def_name.clone(), vec![], "allow-dict".to_owned()));
					}

					allow_dict = Some(element_type.clone());

					self.dict_container_types.insert(def_name.clone(), ContainerTypeMetadata {
						element_type,
					});

				},
				EsexprAnnExternType::Literals(l) => {
					if literals.is_some() {
						return Err(CheckError::DuplicateESExprAnnotation(def_name.clone(), vec![], "literals".to_owned()));
					}

					literals = Some(l);
				},
			}
		}

		if has_derive_codec || allow_optional.is_some() || allow_vararg.is_some() || allow_dict.is_some() {
			et.esexpr_options = Some(EsexprExternTypeOptions {
				allow_value: has_derive_codec,
				allow_optional,
				allow_vararg,
				allow_dict,
				literals: literals.unwrap_or(EsexprExternTypeLiterals {
					allow_bool: false,
					allow_int: false,
					min_int: None,
					max_int: None,
					allow_str: false,
					allow_binary: false,
					allow_float32: false,
					allow_float64: false,
					allow_null: false,
					build_literal_from: None,
				}),
			})
		}
		else if literals.is_some() {
			return Err(CheckError::ESExprAnnotationWithoutDerive(def_name.clone(), vec![]));
		}

		Ok(())
	}
}

#[derive(Debug)]
pub struct ContainerTypeMetadata {
	pub element_type: TypeExpr,
}


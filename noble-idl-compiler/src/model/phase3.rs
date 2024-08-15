use std::collections::{HashMap, HashSet};

use esexpr::ESExprCodec;
use noble_idl_api::*;

use super::CheckError;
use super::phase2::{ContainerTypeMetadata, ESExprOptionParseExtern};

pub fn run(definitions: &mut HashMap<QualifiedName, DefinitionInfo>, phase2_state: &ESExprOptionParseExtern) -> Result<ESExprOptionParserState, CheckError> {
	let mut parser = ESExprOptionParser {
		optional_container_types: &phase2_state.optional_container_types,
		vararg_container_types: &phase2_state.vararg_container_types,
		dict_container_types: &phase2_state.dict_container_types,
		esexpr_codecs: HashMap::new(),
	};

	for dfn in definitions.values_mut() {
		parser.scan_definition(dfn)?;
	}

	let state = ESExprOptionParserState {
		esexpr_codecs: parser.esexpr_codecs,
	};

	Ok(state)
}

pub struct ESExprOptionParserState {
	pub esexpr_codecs: HashMap<QualifiedName, bool>,
}

struct ESExprOptionParser<'a> {
	optional_container_types: &'a HashMap<QualifiedName, ContainerTypeMetadata>,
	vararg_container_types: &'a HashMap<QualifiedName, ContainerTypeMetadata>,
	dict_container_types: &'a HashMap<QualifiedName, ContainerTypeMetadata>,
	esexpr_codecs: HashMap<QualifiedName, bool>,
}

impl <'a> ESExprOptionParser<'a> {
	fn scan_definition(&mut self, dfn: &mut DefinitionInfo) -> Result<(), CheckError> {
		match dfn.definition.as_mut() {
			Definition::Record(rec) =>
				self.scan_record(&dfn.name, &dfn.annotations, rec)?,

			Definition::Enum(e) =>
				self.scan_enum(&dfn.name, &dfn.annotations, e)?,

			Definition::SimpleEnum(e) =>
				self.scan_simple_enum(&dfn.name, &dfn.annotations, e)?,

			Definition::ExternType(et) => {
				self.esexpr_codecs.insert(dfn.name.as_ref().clone(), et.esexpr_options.as_ref().is_some_and(|eo| eo.allow_value));
			},
			Definition::Interface(_) => {},
		}

		Ok(())
	}

	fn scan_record(&mut self, def_name: &QualifiedName, annotations: &[Box<Annotation>], rec: &mut RecordDefinition) -> Result<(), CheckError> {
		let mut has_derive_codec = false;
		let mut constructor = None;
		for ann in annotations {
			if ann.scope != "esexpr" {
				continue;
			}

			let esexpr_rec = EsexprAnnRecord::decode_esexpr(ann.value.clone())
				.map_err(|e| CheckError::InvalidESExprAnnotation(def_name.clone(), e))?;

			match esexpr_rec {
				EsexprAnnRecord::DeriveCodec => {
					if has_derive_codec {
						return Err(CheckError::DuplicateESExprAnnotation(def_name.clone(), vec![], "derive-codec".to_owned()));
					}

					has_derive_codec = true;
				},
				EsexprAnnRecord::Constructor(constructor_name) => {
					if constructor.is_some() {
						return Err(CheckError::DuplicateESExprAnnotation(def_name.clone(), vec![], "constructor".to_owned()));
					}

					constructor = Some(constructor_name);
				},
			}
		}

		if !has_derive_codec && constructor.is_some() {
			return Err(CheckError::ESExprAnnotationWithoutDerive(def_name.clone(), vec![]));
		}

		if has_derive_codec {
			rec.esexpr_options = Some(Box::new(EsexprRecordOptions {
				constructor: constructor.unwrap_or_else(|| def_name.name().to_owned()),
			}));
		}

		self.scan_fields(&mut rec.fields, def_name, None, has_derive_codec)?;


		self.esexpr_codecs.insert(def_name.clone(), has_derive_codec);

		Ok(())
	}

	fn scan_enum(&mut self, def_name: &QualifiedName, annotations: &[Box<Annotation>], e: &mut EnumDefinition) -> Result<(), CheckError> {
		let mut has_derive_codec = false;
		for ann in annotations {
			if ann.scope != "esexpr" {
				continue;
			}

			let esexpr_rec = EsexprAnnEnum::decode_esexpr(ann.value.clone())
				.map_err(|e| CheckError::InvalidESExprAnnotation(def_name.clone(), e))?;

			match esexpr_rec {
				EsexprAnnEnum::DeriveCodec => {
					if has_derive_codec {
						return Err(CheckError::DuplicateESExprAnnotation(def_name.clone(), vec![], "derive-codec".to_owned()));
					}

					has_derive_codec = true;
				},
			}
		}

		if has_derive_codec {
			e.esexpr_options = Some(Box::new(EsexprEnumOptions {}));
		}

		for c in &mut e.cases {
			let mut constructor = None;
			let mut has_inline_value = false;
			for ann in &c.annotations {
				if ann.scope != "esexpr" {
					continue;
				}

				if !has_derive_codec {
					return Err(CheckError::ESExprAnnotationWithoutDerive(def_name.clone(), vec![ c.name.clone() ]));
				}

				let esexpr_rec = EsexprAnnEnumCase::decode_esexpr(ann.value.clone())
					.map_err(|e| CheckError::InvalidESExprAnnotation(def_name.clone(), e))?;

				match esexpr_rec {
					EsexprAnnEnumCase::Constructor(constructor_name) => {
						if constructor.is_some() {
							return Err(CheckError::DuplicateESExprAnnotation(def_name.clone(), vec![], "constructor".to_owned()));
						}

						if has_inline_value {
							return Err(CheckError::ESExprEnumCaseIncompatibleOptions(def_name.clone(), c.name.clone()));
						}

						constructor = Some(constructor_name);
					},
					EsexprAnnEnumCase::InlineValue => {
						if has_inline_value {
							return Err(CheckError::DuplicateESExprAnnotation(def_name.clone(), vec![], "inline-value".to_owned()));
						}

						if constructor.is_some() {
							return Err(CheckError::ESExprEnumCaseIncompatibleOptions(def_name.clone(), c.name.clone()));
						}

						has_inline_value = true;

						if c.fields.len() != 1 {
							return Err(CheckError::ESExprInlineValueNotSingleField(def_name.clone(), c.name.clone()));
						};
					},
				}
			}

			if !has_derive_codec && (constructor.is_some() || has_inline_value) {
				return Err(CheckError::ESExprAnnotationWithoutDerive(def_name.clone(), vec![]));
			}

			if has_derive_codec {
				c.esexpr_options = Some(Box::new(EsexprEnumCaseOptions {
					case_type:
						if has_inline_value { Box::new(EsexprEnumCaseType::InlineValue) }
						else {
							Box::new(EsexprEnumCaseType::Constructor(constructor.unwrap_or_else(|| c.name.clone())))
						}
				}))
			}

			self.scan_fields(&mut c.fields, def_name, Some(&c.name), has_derive_codec)?;
		}

		self.esexpr_codecs.insert(def_name.clone(), has_derive_codec);

		Ok(())
	}

	fn scan_simple_enum(&mut self, def_name: &QualifiedName, annotations: &[Box<Annotation>], e: &mut SimpleEnumDefinition) -> Result<(), CheckError> {
		let mut has_derive_codec = false;
		for ann in annotations {
			if ann.scope != "esexpr" {
				continue;
			}

			let esexpr_rec = EsexprAnnSimpleEnum::decode_esexpr(ann.value.clone())
				.map_err(|e| CheckError::InvalidESExprAnnotation(def_name.clone(), e))?;

			match esexpr_rec {
				EsexprAnnSimpleEnum::DeriveCodec => {
					if has_derive_codec {
						return Err(CheckError::DuplicateESExprAnnotation(def_name.clone(), vec![], "derive-codec".to_owned()));
					}

					has_derive_codec = true;
				},
			}
		}

		if has_derive_codec {
			e.esexpr_options = Some(Box::new(EsexprSimpleEnumOptions {}));
		}

		for c in &mut e.cases {
			let mut constructor = None;
			for ann in &c.annotations {
				if ann.scope != "esexpr" {
					continue;
				}

				if !has_derive_codec {
					return Err(CheckError::ESExprAnnotationWithoutDerive(def_name.clone(), vec![ c.name.clone() ]));
				}

				let esexpr_rec = EsexprAnnSimpleEnumCase::decode_esexpr(ann.value.clone())
					.map_err(|e| CheckError::InvalidESExprAnnotation(def_name.clone(), e))?;

				match esexpr_rec {
					EsexprAnnSimpleEnumCase::Constructor(constructor_name) => {
						if constructor.is_some() {
							return Err(CheckError::DuplicateESExprAnnotation(def_name.clone(), vec![], "constructor".to_owned()));
						}

						constructor = Some(constructor_name);
					},
				}
			}

			if !has_derive_codec && constructor.is_some() {
				return Err(CheckError::ESExprAnnotationWithoutDerive(def_name.clone(), vec![]));
			}

			if has_derive_codec {
				c.esexpr_options = Some(Box::new(EsexprSimpleEnumCaseOptions {
					name: constructor.unwrap_or_else(|| c.name.clone()),
				}));
			}
		}

		self.esexpr_codecs.insert(def_name.clone(), has_derive_codec);

		Ok(())
	}

	fn scan_fields(&self, fields: &mut [Box<RecordField>], def_name: &QualifiedName, case_name: Option<&str>, is_esexpr_type: bool) -> Result<(), CheckError> {
		let mut keywords = HashSet::new();

		let mut has_dict = false;
		let mut has_vararg = false;
		let mut has_optional_positional = false;

		for field in fields {
			let mut is_keyword = None;
			let mut is_dict = false;
			let mut is_vararg = false;
			let mut is_optional = false;
			let mut is_default_value = None;

			for ann in &field.annotations {
				if ann.scope != "esexpr" {
					continue;
				}

				let current_path = || {
					let mut path = Vec::new();
					path.extend(case_name.map(str::to_owned));
					path.push(field.name.clone());
					path
				};

				let esexpr_field = EsexprAnnRecordField::decode_esexpr(ann.value.clone())
					.map_err(|e| CheckError::InvalidESExprAnnotation(def_name.clone(), e))?;

				if !is_esexpr_type {
					return Err(CheckError::ESExprAnnotationWithoutDerive(def_name.clone(), current_path()))
				}

				match esexpr_field {
					EsexprAnnRecordField::Keyword(name) => {
						if is_keyword.is_some() {
							return Err(CheckError::DuplicateESExprAnnotation(def_name.clone(), current_path(), "keyword".to_owned()));
						}

						if has_dict {
							return Err(CheckError::ESExprDictBeforeKeyword(def_name.clone(), case_name.map(str::to_owned), field.name.clone()));
						}

						let name = name.unwrap_or_else(|| field.name.clone());
						if let Some(name) = keywords.replace(name.clone()) {
							return Err(CheckError::ESExprDuplicateKeyword(def_name.clone(), case_name.map(str::to_owned), name));
						}

						is_keyword = Some(name);
					},
					EsexprAnnRecordField::Dict => {
						if has_dict {
							return Err(CheckError::ESExprMultipleDict(def_name.clone(), case_name.map(str::to_owned), field.name.clone()));
						}

						if is_dict {
							return Err(CheckError::DuplicateESExprAnnotation(def_name.clone(), current_path(), "dict".to_owned()));
						}

						has_dict = true;
						is_dict = true;
					},
					EsexprAnnRecordField::Vararg => {
						if has_vararg {
							return Err(CheckError::ESExprMultipleVararg(def_name.clone(), case_name.map(str::to_owned), field.name.clone()));
						}

						if has_optional_positional {
							return Err(CheckError::ESExprVarargAfterOptionalPositional(def_name.clone(), case_name.map(str::to_owned), field.name.clone()));
						}

						if is_vararg {
							return Err(CheckError::DuplicateESExprAnnotation(def_name.clone(), current_path(), "vararg".to_owned()));
						}

						has_vararg = true;
						is_vararg = true;
					},

					EsexprAnnRecordField::Optional => {
						if is_optional {
							return Err(CheckError::DuplicateESExprAnnotation(def_name.clone(), current_path(), "optional".to_owned()));
						}

						is_optional = true;
					},

					EsexprAnnRecordField::DefaultValue(value) => {
						if is_default_value.is_some() {
							return Err(CheckError::DuplicateESExprAnnotation(def_name.clone(), current_path(), "default-value".to_owned()));
						}

						is_default_value = Some(value);
					}
				}
			}

			if has_vararg && !(is_keyword.is_some() || is_dict || is_vararg) {
				return Err(CheckError::ESExprVarargBeforePositional(def_name.clone(), case_name.map(str::to_owned), field.name.clone()));
			}

			if
				(is_keyword.is_some() && (is_dict || is_vararg || (is_optional && is_default_value.is_some()))) ||
				(is_dict && is_vararg) ||
				((is_dict || is_vararg) && (is_optional || is_default_value.is_some())) ||
				(is_keyword.is_none() && !is_dict && !is_vararg && is_default_value.is_some())
			{
				return Err(CheckError::ESExprFieldIncompatibleOptions(def_name.clone(), case_name.map(str::to_owned), field.name.clone()));
			}

			if is_dict && is_vararg {
				return Err(CheckError::ESExprFieldIncompatibleOptions(def_name.clone(), case_name.map(str::to_owned), field.name.clone()));
			}

			if !is_esexpr_type && (is_keyword.is_some() || is_dict || is_vararg || is_optional || is_default_value.is_some()) {
				return Err(CheckError::ESExprAnnotationWithoutDerive(def_name.clone(), vec![]));
			}

			if is_esexpr_type {
				let kind =
					if is_vararg {
						let Some(vararg_metadata) = get_type_name(&field.field_type).and_then(|ftn| self.vararg_container_types.get(ftn)) else {
							eprintln!("{:?}", def_name);
							eprintln!("{:?}", self.vararg_container_types);
							return Err(CheckError::ESExprInvalidVarargFieldType(def_name.clone(), case_name.map(str::to_owned), field.name.clone()));
						};

						EsexprRecordFieldKind::Vararg(Box::new(vararg_metadata.element_type.clone()))
					}
					else if is_dict {
						let Some(dict_metadata) = get_type_name(&field.field_type).and_then(|ftn| self.dict_container_types.get(ftn)) else {
							return Err(CheckError::ESExprInvalidDictFieldType(def_name.clone(), case_name.map(str::to_owned), field.name.clone()));
						};

						EsexprRecordFieldKind::Dict(Box::new(dict_metadata.element_type.clone()))
					}
					else if let Some(name) = is_keyword {
						let mode =
							// Ignore default_value for now.
							// Default values will be added in a later pass.
							if is_optional {
								let Some(opt_metadata) = get_type_name(&field.field_type).and_then(|ftn| self.optional_container_types.get(ftn)) else {
									return Err(CheckError::ESExprInvalidOptionalFieldType(def_name.clone(), case_name.map(str::to_owned), field.name.clone()));
								};

								EsexprRecordKeywordMode::Optional(Box::new(opt_metadata.element_type.clone()))
							}
							else {
								EsexprRecordKeywordMode::Required
							};

						EsexprRecordFieldKind::Keyword(name, Box::new(mode))
					}
					else {
						let mode =
							if is_optional {
								if has_optional_positional {
									return Err(CheckError::ESExprMultipleOptionalPositional(def_name.clone(), case_name.map(str::to_owned), field.name.clone()));
								}

								has_optional_positional = true;

								let Some(opt_metadata) = get_type_name(&field.field_type).and_then(|ftn| self.optional_container_types.get(ftn)) else {
									return Err(CheckError::ESExprInvalidOptionalFieldType(def_name.clone(), case_name.map(str::to_owned), field.name.clone()));
								};

								EsexprRecordPositionalMode::Optional(Box::new(opt_metadata.element_type.clone()))
							}
							else {
								EsexprRecordPositionalMode::Required
							};

						EsexprRecordFieldKind::Positional(Box::new(mode))
					};

				field.esexpr_options = Some(Box::new(EsexprRecordFieldOptions { kind: Box::new(kind) }));
			}
		}

		Ok(())
	}
}

fn get_type_name(t: &TypeExpr) -> Option<&QualifiedName> {
	match t {
		TypeExpr::DefinedType(name, _) => Some(name),
		TypeExpr::TypeParameter(_) => None,
	}
}



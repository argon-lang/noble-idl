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
		match &mut dfn.definition {
			Definition::Record(rec) =>
				self.scan_record(&dfn.name, &dfn.annotations, rec)?,

			Definition::Enum(e) =>
				self.scan_enum(&dfn.name, &dfn.annotations, e)?,

			Definition::ExternType(et) => {
				self.esexpr_codecs.insert(dfn.name.clone(), et.esexpr_options.is_some());
			},
			Definition::Interface(_) => {},
		}

		Ok(())
	}

	fn scan_record(&mut self, def_name: &QualifiedName, annotations: &[Annotation], rec: &mut RecordDefinition) -> Result<(), CheckError> {
		let mut has_derive_codec = false;
		let mut constructor = None;
		for ann in annotations {
			if ann.scope != "esexpr" {
				continue;
			}

			let esexpr_rec = ESExprAnnRecord::decode_esexpr(ann.value.clone())
				.map_err(|_| CheckError::InvalidESExprAnnotation(def_name.clone()))?;

			match esexpr_rec {
				ESExprAnnRecord::DeriveCodec => {
					if has_derive_codec {
						return Err(CheckError::DuplicateESExprAnnotation(def_name.clone(), vec![], "derive-codec".to_owned()));
					}

					has_derive_codec = true;
				},
				ESExprAnnRecord::Constructor(constructor_name) => {
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
			rec.esexpr_options = Some(ESExprRecordOptions {
				constructor: constructor.unwrap_or_else(|| def_name.name().to_owned()),
			});
		}

		self.scan_fields(&mut rec.fields, def_name, None, has_derive_codec)?;


		self.esexpr_codecs.insert(def_name.clone(), has_derive_codec);

		Ok(())
	}

	fn scan_enum(&mut self, def_name: &QualifiedName, annotations: &[Annotation], e: &mut EnumDefinition) -> Result<(), CheckError> {
		let mut has_derive_codec = false;
		let mut has_simple_enum = false;
		for ann in annotations {
			if ann.scope != "esexpr" {
				continue;
			}

			let esexpr_rec = ESExprAnnEnum::decode_esexpr(ann.value.clone())
				.map_err(|_| CheckError::InvalidESExprAnnotation(def_name.clone()))?;

			match esexpr_rec {
				ESExprAnnEnum::DeriveCodec => {
					if has_derive_codec {
						return Err(CheckError::DuplicateESExprAnnotation(def_name.clone(), vec![], "derive-codec".to_owned()));
					}

					has_derive_codec = true;
				},
				ESExprAnnEnum::SimpleEnum => {
					if has_simple_enum {
						return Err(CheckError::DuplicateESExprAnnotation(def_name.clone(), vec![], "simple-enum".to_owned()));
					}

					has_simple_enum = true;
				},
			}
		}

		if !has_derive_codec && has_simple_enum {
			return Err(CheckError::ESExprAnnotationWithoutDerive(def_name.clone(), vec![]));
		}

		if has_derive_codec {
			e.esexpr_options = Some(ESExprEnumOptions {
				simple_enum: has_simple_enum,
			});
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

				let esexpr_rec = ESExprAnnEnumCase::decode_esexpr(ann.value.clone())
					.map_err(|_| CheckError::InvalidESExprAnnotation(def_name.clone()))?;

				match esexpr_rec {
					ESExprAnnEnumCase::Constructor(constructor_name) => {
						if constructor.is_some() {
							return Err(CheckError::DuplicateESExprAnnotation(def_name.clone(), vec![], "constructor".to_owned()));
						}

						if has_inline_value {
							return Err(CheckError::ESExprEnumCaseIncompatibleOptions(def_name.clone(), c.name.clone()));
						}

						constructor = Some(constructor_name);
					},
					ESExprAnnEnumCase::InlineValue => {
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
				c.esexpr_options = Some(ESExprEnumCaseOptions {
					case_type:
						if has_inline_value { ESExprEnumCaseType::InlineValue }
						else {
							ESExprEnumCaseType::Constructor(constructor.unwrap_or_else(|| c.name.clone()))
						}
				})
			}

			self.scan_fields(&mut c.fields, def_name, Some(&c.name), has_derive_codec)?;
		}

		self.esexpr_codecs.insert(def_name.clone(), has_derive_codec);

		Ok(())
	}

	fn scan_fields(&self, fields: &mut [RecordField], def_name: &QualifiedName, case_name: Option<&str>, is_esexpr_type: bool) -> Result<(), CheckError> {
		let mut keywords = HashSet::new();

		let mut has_dict = false;
		let mut has_vararg = false;

		for field in fields {
			let mut kind = None;

			let mut is_keyword = false;
			let mut is_dict = false;
			let mut is_vararg = false;

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

				let esexpr_field = ESExprAnnRecordField::decode_esexpr(ann.value.clone())
					.map_err(|_| CheckError::InvalidESExprAnnotation(def_name.clone()))?;

				if !is_esexpr_type {
					return Err(CheckError::ESExprAnnotationWithoutDerive(def_name.clone(), current_path()))
				}

				match esexpr_field {
					ESExprAnnRecordField::Keyword { name, required, default_value } => {
						if is_keyword {
							return Err(CheckError::DuplicateESExprAnnotation(def_name.clone(), current_path(), "keyword".to_owned()));
						}

						is_keyword = true;

						if is_dict || is_vararg || (!required && default_value.is_some()) {
							return Err(CheckError::ESExprFieldIncompatibleOptions(def_name.clone(), case_name.map(str::to_owned), field.name.clone()));
						}

						if has_dict {
							return Err(CheckError::ESExprDictBeforeKeyword(def_name.clone(), case_name.map(str::to_owned), field.name.clone()));
						}

						let name = name.unwrap_or_else(|| field.name.clone());
						if let Some(name) = keywords.replace(name.clone()) {
							return Err(CheckError::ESExprDuplicateKeyword(def_name.clone(), case_name.map(str::to_owned), name));
						}

						let mode =
							// Ignore default_value for now.
							// Default values will be added in a later pass.
							if required { ESExprRecordKeywordMode::Required }
							else {
								let Some(opt_metadata) = get_type_name(&field.field_type).and_then(|ftn| self.optional_container_types.get(ftn)) else {
									return Err(CheckError::ESExprInvalidOptionalFieldType(def_name.clone(), case_name.map(str::to_owned), field.name.clone()));
								};

								ESExprRecordKeywordMode::Optional(opt_metadata.element_type.clone())
							};

						kind = Some(ESExprRecordFieldKind::Keyword(name, mode));
					},
					ESExprAnnRecordField::Dict => {
						if has_dict {
							return Err(CheckError::ESExprMultipleDict(def_name.clone(), case_name.map(str::to_owned), field.name.clone()));
						}

						if is_dict {
							return Err(CheckError::DuplicateESExprAnnotation(def_name.clone(), current_path(), "dict".to_owned()));
						}

						has_dict = true;
						is_dict = true;

						if is_keyword || is_vararg {
							return Err(CheckError::ESExprFieldIncompatibleOptions(def_name.clone(), case_name.map(str::to_owned), field.name.clone()));
						}

						let Some(dict_metadata) = get_type_name(&field.field_type).and_then(|ftn| self.dict_container_types.get(ftn)) else {
							return Err(CheckError::ESExprInvalidDictFieldType(def_name.clone(), case_name.map(str::to_owned), field.name.clone()));
						};

						kind = Some(ESExprRecordFieldKind::Dict(dict_metadata.element_type.clone()));
					},
					ESExprAnnRecordField::Vararg => {
						if has_vararg {
							return Err(CheckError::ESExprMultipleDict(def_name.clone(), case_name.map(str::to_owned), field.name.clone()));
						}

						if is_vararg {
							return Err(CheckError::DuplicateESExprAnnotation(def_name.clone(), current_path(), "vararg".to_owned()));
						}

						has_vararg = true;
						is_vararg = true;

						if is_keyword || is_dict {
							return Err(CheckError::ESExprFieldIncompatibleOptions(def_name.clone(), case_name.map(str::to_owned), field.name.clone()));
						}

						let Some(vararg_metadata) = get_type_name(&field.field_type).and_then(|ftn| self.vararg_container_types.get(ftn)) else {
							eprintln!("{:?}", def_name);
							eprintln!("{:?}", self.vararg_container_types);
							return Err(CheckError::ESExprInvalidVarargFieldType(def_name.clone(), case_name.map(str::to_owned), field.name.clone()));
						};

						kind = Some(ESExprRecordFieldKind::Vararg(vararg_metadata.element_type.clone()));
					},
				}
			}

			if has_vararg && !(is_keyword || is_dict || is_vararg) {
				return Err(CheckError::ESExprVarargBeforePositional(def_name.clone(), case_name.map(str::to_owned), field.name.clone()));
			}

			if !is_esexpr_type && kind.is_some() {
				return Err(CheckError::ESExprAnnotationWithoutDerive(def_name.clone(), vec![]));
			}

			if is_esexpr_type {
				field.esexpr_options = Some(ESExprRecordFieldOptions {
					kind: kind.unwrap_or(ESExprRecordFieldKind::Positional),
				});
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



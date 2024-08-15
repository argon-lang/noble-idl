use std::collections::{HashMap, HashSet};

use esexpr::ESExprTag;
use noble_idl_api::*;

use super::{phase3::ESExprOptionParserState, tag_scanner::{TagScanner, TagScannerState}, CheckError};


pub fn run(definitions: &HashMap<QualifiedName, DefinitionInfo>, option_parser_state: &ESExprOptionParserState, tag_scan_state: &mut TagScannerState) -> Result<(), CheckError> {
	let mut checker = ESExprChecker {
		definitions,
		esexpr_codecs: &option_parser_state.esexpr_codecs,
		tag_scanner: TagScanner {
			definitions,
			state: tag_scan_state,
		},
	};

	checker.check()
}



struct ESExprChecker<'a> {
	definitions: &'a HashMap<QualifiedName, DefinitionInfo>,
	esexpr_codecs: &'a HashMap<QualifiedName, bool>,
	tag_scanner: TagScanner<'a>,
}

impl <'a> ESExprChecker<'a> {



	fn def_has_esexpr_codec(&mut self, name: &QualifiedName) -> bool {
		self.esexpr_codecs.get(name).copied().unwrap_or_default()
	}

	fn check(&mut self) -> Result<(), CheckError> {
		for def in self.definitions.values() {
			self.check_definition(def)?;
		}

		Ok(())
	}

	fn check_definition(&mut self, def: &'a DefinitionInfo) -> Result<(), CheckError> {
		match def.definition.as_ref() {
			noble_idl_api::Definition::Record(r) => self.check_record(def, r),
			noble_idl_api::Definition::Enum(e) => self.check_enum(def, e),
			noble_idl_api::Definition::SimpleEnum(e) => self.check_simple_enum(def, e),
			noble_idl_api::Definition::ExternType(et) => self.check_extern_type(def, et),
			noble_idl_api::Definition::Interface(_) => Ok(()),
		}
	}

	fn check_record(&mut self, def: &DefinitionInfo, r: &RecordDefinition) -> Result<(), CheckError> {
		if !r.esexpr_options.is_some() {
			return Ok(())
		}

		self.check_fields(&r.fields, &def.name, None)
	}

	fn check_enum(&mut self, def: &DefinitionInfo, e: &'a EnumDefinition) -> Result<(), CheckError> {
		if !e.esexpr_options.is_some() {
			return Ok(())
		}

		let mut tags = HashSet::new();

		let mut add_tag = |tag| {
			if let Some(tag) = tags.replace(tag) {
				return Err(CheckError::ESExprDuplicateTag(def.name.as_ref().clone(), tag));
			}
			else {
				return Ok(())
			}
		};

		for c in &e.cases {
			let Some(esexpr_options) = c.esexpr_options.as_ref() else { continue; };

			match esexpr_options.case_type.as_ref() {
				EsexprEnumCaseType::Constructor(name) => add_tag(ESExprTag::Constructor(name.clone()))?,
				EsexprEnumCaseType::InlineValue => {
					let [field] = &c.fields[..] else {
						return Err(CheckError::ESExprInlineValueNotSingleField(def.name.as_ref().clone(), c.name.clone()));
					};

					let iv_tags = self.tag_scanner.scan_type_for(&field.field_type, &def.name);
					if iv_tags.is_empty() {
						return Err(CheckError::ESExprInlineValueInvalidTags(def.name.as_ref().clone(), field.name.clone()));
					}

					for tag in iv_tags {
						add_tag(tag)?;
					}
				},
			}

			self.check_fields(&c.fields, &def.name, Some(&c.name))?;
		}

		Ok(())
	}

	fn check_simple_enum(&mut self, def: &DefinitionInfo, e: &'a SimpleEnumDefinition) -> Result<(), CheckError> {
		if !e.esexpr_options.is_some() {
			return Ok(())
		}

		let mut tags = HashSet::new();

		let mut add_tag = |tag| {
			if let Some(tag) = tags.replace(tag) {
				return Err(CheckError::ESExprDuplicateTag(def.name.as_ref().clone(), tag));
			}
			else {
				return Ok(())
			}
		};

		for c in &e.cases {
			let Some(esexpr_options) = c.esexpr_options.as_ref() else { continue; };

			add_tag(ESExprTag::Constructor(esexpr_options.name.clone()))?;
		}

		Ok(())
	}

	fn check_extern_type(&mut self, def: &'a DefinitionInfo, et: &ExternTypeDefinition) -> Result<(), CheckError> {
		let Some(esexpr_options) = &et.esexpr_options else {
			return Ok(());
		};

		if let Some(element_type) = &esexpr_options.allow_optional {
			if !self.check_type(element_type) {
				return Err(CheckError::ESExprExternTypeCodecMissing(def.name.as_ref().clone()));
			}
		}

		if let Some(element_type) = &esexpr_options.allow_vararg {
			if !self.check_type(element_type) {
				return Err(CheckError::ESExprExternTypeCodecMissing(def.name.as_ref().clone()));
			}
		}

		if let Some(element_type) = &esexpr_options.allow_dict {
			if !self.check_type(element_type) {
				return Err(CheckError::ESExprExternTypeCodecMissing(def.name.as_ref().clone()));
			}
		}

		if let Some(build_from) = &esexpr_options.literals.build_literal_from {
			if !self.check_type(build_from) {
				return Err(CheckError::ESExprExternTypeCodecMissing(def.name.as_ref().clone()));
			}
		}

		Ok(())
	}

	fn check_fields(&mut self, fields: &[Box<RecordField>], def_name: &QualifiedName, case_name: Option<&str>) -> Result<(), CheckError> {
		for field in fields {
			let Some(esexpr_options) = &field.esexpr_options else {
				continue;
			};

			match esexpr_options.kind.as_ref() {
				EsexprRecordFieldKind::Positional(mode) => {
					match mode.as_ref() {
						EsexprRecordPositionalMode::Required => {
							if !self.check_type(&field.field_type) {
								return Err(CheckError::ESExprMemberCodecMissing(def_name.clone(), case_name.map(str::to_owned), field.name.clone()));
							}
						},

						EsexprRecordPositionalMode::Optional(element_type) => {
							if !self.check_type(&element_type) {
								return Err(CheckError::ESExprMemberCodecMissing(def_name.clone(), case_name.map(str::to_owned), field.name.clone()));
							}
						}
					}
				},

				EsexprRecordFieldKind::Keyword(_, mode) => {
					match mode.as_ref() {
						EsexprRecordKeywordMode::Required | EsexprRecordKeywordMode::DefaultValue(_) => {
							if !self.check_type(&field.field_type) {
								return Err(CheckError::ESExprMemberCodecMissing(def_name.clone(), case_name.map(str::to_owned), field.name.clone()));
							}
						},

						EsexprRecordKeywordMode::Optional(element_type) => {
							if !self.check_type(&element_type) {
								return Err(CheckError::ESExprMemberCodecMissing(def_name.clone(), case_name.map(str::to_owned), field.name.clone()));
							}
						}
					}
				},

				EsexprRecordFieldKind::Dict(element_type) => {
					if !self.check_type(&element_type) {
						return Err(CheckError::ESExprMemberCodecMissing(def_name.clone(), case_name.map(str::to_owned), field.name.clone()));
					}
				},

				EsexprRecordFieldKind::Vararg(element_type) => {
					if !self.check_type(&element_type) {
						return Err(CheckError::ESExprMemberCodecMissing(def_name.clone(), case_name.map(str::to_owned), field.name.clone()));
					}
				},
			}
		}

		Ok(())
	}

	fn check_type(&mut self, t: &TypeExpr) -> bool {
		match t {
			TypeExpr::DefinedType(name, args) =>
				self.def_has_esexpr_codec(name) && args.iter().all(|arg| self.check_type(arg)),
			TypeExpr::TypeParameter(_) => true,
		}
	}



}


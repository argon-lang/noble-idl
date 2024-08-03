use std::collections::{HashMap, HashSet, VecDeque};

use esexpr::{ESExpr, ESExprCodec, ESExprTag};
use noble_idl_api::*;
use noble_idl_runtime::Binary;

use super::{tag_scanner::{TagScanner, TagScannerState}, CheckError};


pub fn run(definitions: &mut HashMap<QualifiedName, DefinitionInfo>, tag_scan_state: &mut TagScannerState) -> Result<(), CheckError> {
	let mut parser = ESExprOptionDefaultValueParser {
		definitions,
		tag_scanner: TagScanner {
			definitions,
			state: tag_scan_state,
		},
		default_values: HashMap::new(),
	};

	parser.scan()?;

	DefaultUpdater.update_all(definitions, parser.default_values);

	Ok(())
}


struct ESExprOptionDefaultValueParser<'a> {
	definitions: &'a HashMap<QualifiedName, DefinitionInfo>,
	tag_scanner: TagScanner<'a>,
	default_values: HashMap<FieldKey, Option<EsexprDecodedValue>>,
}

impl <'a> ESExprOptionDefaultValueParser<'a> {

	fn scan(&mut self) -> Result<(), CheckError> {
		for dfn in self.definitions.values() {
			self.scan_definition(dfn)?
		}

		Ok(())
	}

	fn scan_definition(&mut self, dfn: &'a DefinitionInfo) -> Result<(), CheckError> {
		match &dfn.definition {
			Definition::Record(rec) =>
				self.scan_record(dfn, rec)?,

			Definition::Enum(e) =>
				self.scan_enum(dfn, e)?,

			Definition::ExternType(_) => {},
			Definition::Interface(_) => {},
		}

		Ok(())
	}

	fn scan_record(&mut self, dfn: &'a DefinitionInfo, rec: &'a RecordDefinition) -> Result<(), CheckError> {
		if rec.esexpr_options.is_some() {
			self.scan_fields(&rec.fields, dfn, None)?;
		}

		Ok(())
	}

	fn scan_enum(&mut self, dfn: &'a DefinitionInfo, e: &'a EnumDefinition) -> Result<(), CheckError> {
		if e.esexpr_options.is_some() {
			for c in &e.cases {
				self.scan_fields(&c.fields, dfn, Some(&c.name))?;
			}
		}

		Ok(())
	}

	fn scan_fields(&mut self, fields: &'a [RecordField], dfn: &'a DefinitionInfo, case_name: Option<&'a str>) -> Result<(), CheckError> {
		for field in fields {
			let Some(feo) = field.esexpr_options.as_ref() else { continue; };

			match &feo.kind {
				EsexprRecordFieldKind::Positional(_) => {},
				EsexprRecordFieldKind::Keyword(_, EsexprRecordKeywordMode::Optional(_)) => {},
				EsexprRecordFieldKind::Keyword(_, _) => {
					let mut value_parser = ValueParser {
						outer_parser: self,
						seen_fields: HashSet::new(),
						seen_decode_types: HashSet::new(),

						dfn,
						case_name,
						field,
					};

					let key = FieldKey {
						definition_name: dfn.name.clone(),
						case_name: case_name.map(str::to_owned),
						field_name: field.name.clone(),
					};

					value_parser.lookup_default_value(key, field)?;
				},
				EsexprRecordFieldKind::Dict(_) => {},
				EsexprRecordFieldKind::Vararg(_) => {},
			}
		}

		Ok(())
	}


}

struct ValueParser<'a, 'b> {
	outer_parser: &'b mut ESExprOptionDefaultValueParser<'a>,
	seen_fields: HashSet<FieldKey>,
	seen_decode_types: HashSet<QualifiedName>,

	dfn: &'a DefinitionInfo,
	case_name: Option<&'a str>,
	field: &'a RecordField,
}

impl <'a, 'b> ValueParser<'a, 'b> {
	fn error(&self) -> CheckError {
		CheckError::ESExprInvalidDefaultValue(self.dfn.name.clone(), self.case_name.map(str::to_owned), self.field.name.clone())
	}

	fn fail<A>(&self) -> Result<A, CheckError> {
		Err(self.error())
	}

	fn lookup_default_value<'c>(&'c mut self, field_key: FieldKey, field: &RecordField) -> Result<Option<&'c EsexprDecodedValue>, CheckError> where 'b : 'c {
		if self.outer_parser.default_values.contains_key(&field_key) {
			return Ok(self.outer_parser.default_values.get(&field_key).unwrap().as_ref())
		}

		if !self.seen_fields.insert(field_key.clone()) {
			self.fail()?;
		}

		let parsed_value = field.annotations.iter()
			.filter(|ann| ann.scope == "esexpr")
			.filter_map(|ann| EsexprAnnRecordField::decode_esexpr(ann.value.clone()).ok())
			.find_map(|ann| match ann {
				EsexprAnnRecordField::DefaultValue(default_value) => Some(default_value),
				_ => None,
			})
			.map(|expr| self.parse_value(&field.field_type, expr))
			.transpose()?;

		let res = self.outer_parser.default_values.entry(field_key).or_insert(parsed_value);

		Ok(res.as_ref())
	}


	fn parse_value(&mut self, t: &TypeExpr, value: ESExpr) -> Result<EsexprDecodedValue, CheckError> {
		match t {
			TypeExpr::DefinedType(name, args) => {
				let dfn = self.outer_parser.definitions.get(name).ok_or_else(|| self.error())?;

				match &dfn.definition {
					Definition::Record(r) => self.parse_record_value(dfn, r, t, args, value),
					Definition::Enum(e) => self.parse_enum_value(dfn, e, t, args, value),
					Definition::ExternType(et) => self.parse_extern_type_value(dfn, et, t, args, value),
					Definition::Interface(_) => self.fail(),
				}
			},
			TypeExpr::TypeParameter(_) => self.fail(),
		}
	}

	fn parse_record_value(&mut self, dfn: &'a DefinitionInfo, r: &'a RecordDefinition, t: &TypeExpr, type_args: &[TypeExpr], value: ESExpr) -> Result<EsexprDecodedValue, CheckError> {
		let ESExpr::Constructor { name, args, kwargs } = value else { self.fail()? };

		if name != dfn.name.name() {
			self.fail()?;
		}

		let fields = self.parse_field_values(dfn, None, type_args, &r.fields, args.into(), kwargs)?;

		Ok(EsexprDecodedValue::Record(t.clone(), fields))
	}

	fn parse_enum_value(&mut self, dfn: &'a DefinitionInfo, e: &'a EnumDefinition, t: &TypeExpr, type_args: &[TypeExpr], value: ESExpr) -> Result<EsexprDecodedValue, CheckError> {
		let ESExpr::Constructor { name, args, kwargs } = value else { self.fail()? };

		for c in &e.cases {
			let case_options = c.esexpr_options.as_ref().ok_or_else(|| self.error())?;

			match &case_options.case_type {
				EsexprEnumCaseType::InlineValue => {
					let [field] = &c.fields[..] else { self.fail()? };

					let tags = self.outer_parser.tag_scanner.scan_type_for(&field.field_type, &dfn.name);

					if !tags.contains(&ESExprTag::Constructor(name.clone())) {
						continue;
					}
				},
				EsexprEnumCaseType::Constructor(case_ctor_name) => {
					if name != *case_ctor_name {
						continue;
					}
				}
			}


			let fields = self.parse_field_values(dfn, Some(c.name.as_ref()), type_args, &c.fields, args.into(), kwargs)?;

			return Ok(EsexprDecodedValue::Enum(t.clone(), c.name.clone(), fields));
		}

		self.fail()?
	}

	fn parse_extern_type_value(&mut self, dfn: &'a DefinitionInfo, et: &'a ExternTypeDefinition, t: &TypeExpr, type_args: &[TypeExpr], value: ESExpr) -> Result<EsexprDecodedValue, CheckError> {
		let Some(esexpr_options) = et.esexpr_options.as_ref() else { self.fail()? };

		let mapping = dfn.type_parameters.iter().map(|tp| tp.name()).zip(type_args).collect::<HashMap<_, _>>();

		Ok(match &value {
			ESExpr::Constructor { .. } => {
				let Some(mut build_from) = esexpr_options.literals.build_literal_from.clone() else { self.fail()? };
				if !build_from.substitute(&mapping) {
					self.fail()?
				}

				let build_from_type_name = match &build_from {
						TypeExpr::DefinedType(name, _) => name,
						TypeExpr::TypeParameter(_) => self.fail()?,
					};

				if !self.seen_decode_types.insert(build_from_type_name.clone()) {
					self.fail()?
				}

				let dec_value = self.parse_value(&build_from, value)?;
				self.seen_decode_types.remove(&build_from_type_name);

				EsexprDecodedValue::BuildFrom(t.clone(), Box::new(dec_value))
			},
			ESExpr::Bool(b) => if esexpr_options.literals.allow_bool { EsexprDecodedValue::FromBool(t.clone(), *b) } else { self.fail()? },
			ESExpr::Int(i) => {
				if esexpr_options.literals.allow_int {
					EsexprDecodedValue::FromInt {
						t: t.clone(),
						i: i.clone(),
						min_int: esexpr_options.literals.min_int.clone(),
						max_int: esexpr_options.literals.max_int.clone(),
					}
				}
				else {
					self.fail()?
				}
			},

			ESExpr::Str(s) => if esexpr_options.literals.allow_bool { EsexprDecodedValue::FromStr(t.clone(), s.clone()) } else { self.fail()? },
			ESExpr::Binary(b) => if esexpr_options.literals.allow_bool { EsexprDecodedValue::FromBinary(t.clone(), Binary(b.clone())) } else { self.fail()? },
			ESExpr::Float32(f) => if esexpr_options.literals.allow_bool { EsexprDecodedValue::FromFloat32(t.clone(), *f) } else { self.fail()? },
			ESExpr::Float64(f) => if esexpr_options.literals.allow_bool { EsexprDecodedValue::FromFloat64(t.clone(), *f) } else { self.fail()? },
			ESExpr::Null => if esexpr_options.literals.allow_bool { EsexprDecodedValue::FromNull(t.clone()) } else { self.fail()? },
		})
	}

	fn parse_field_values(&mut self, dfn: &'a DefinitionInfo, case_name: Option<&'a str>, type_args: &[TypeExpr], fields: &'a [RecordField], mut args: VecDeque<ESExpr>, mut kwargs: HashMap<String, ESExpr>) -> Result<HashMap<String, EsexprDecodedValue>, CheckError> {
		let mut parsed = HashMap::new();

		let mapping = dfn.type_parameters.iter().map(|tp| tp.name()).zip(type_args).collect::<HashMap<_, _>>();

		for field in fields {
			let options = field.esexpr_options.as_ref().ok_or_else(|| self.error())?;

			let mut field_type = field.field_type.clone();
			if !field_type.substitute(&mapping) {
				self.fail()?;
			}

			let value = match &options.kind {
				EsexprRecordFieldKind::Positional(EsexprRecordPositionalMode::Required) => {
					let arg_value = args.pop_front().ok_or_else(|| self.error())?;
					self.parse_value(&field_type, arg_value)?
				},

				EsexprRecordFieldKind::Positional(EsexprRecordPositionalMode::Optional(element_type)) =>
					EsexprDecodedValue::Optional {
						optional_type: field_type,
						value: Box::new(
							args.pop_front()
								.map(|value| self.parse_value(element_type, value))
								.transpose()?
						),
					},

				EsexprRecordFieldKind::Keyword(_, EsexprRecordKeywordMode::Optional(element_type)) =>
					EsexprDecodedValue::Optional {
						optional_type: field_type,
						value: Box::new(
							kwargs.remove(&field.name)
								.map(|value| self.parse_value(element_type, value))
								.transpose()?
						),
					},

				EsexprRecordFieldKind::Keyword(_, _) => {
					if let Some(value) = kwargs.remove(&field.name) {
						self.parse_value(&field_type, value)?
					}
					else {
						let key = FieldKey {
							definition_name: dfn.name.clone(),
							case_name: case_name.map(str::to_owned),
							field_name: field.name.clone(),
						};

						match self.lookup_default_value(key, field)? {
							Some(default_value) => default_value.clone(),
							None => self.fail()?,
						}
					}
				},

				EsexprRecordFieldKind::Dict(element_type) => {
					let mut dict = HashMap::new();
					for (k, v) in kwargs.drain() {
						let item_value = self.parse_value(element_type, v)?;
						dict.insert(k, item_value);
					}

					EsexprDecodedValue::Dict { dict_type: field_type, values: dict }
				},

				EsexprRecordFieldKind::Vararg(element_type) => {
					let mut vararg = Vec::new();
					for v in args.drain(..) {
						let item_value = self.parse_value(element_type, v)?;
						vararg.push(item_value);
					}

					EsexprDecodedValue::Vararg { vararg_type: field_type, values: vararg }
				},
			};

			parsed.insert(field.name.clone(), value);
		}

		Ok(parsed)
	}
}



#[derive(Debug, Clone, PartialEq, Eq, Hash)]
struct FieldKey {
	definition_name: QualifiedName,
	case_name: Option<String>,
	field_name: String,
}



struct DefaultUpdater;

impl DefaultUpdater {
	fn update_all(&self, definitions: &mut HashMap<QualifiedName, DefinitionInfo>, default_values: HashMap<FieldKey, Option<EsexprDecodedValue>>) {
		for (k, v) in default_values {
			let Some(v) = v else { continue; };
			self.update(definitions, k, v);
		}
	}

	fn update(&self, definitions: &mut HashMap<QualifiedName, DefinitionInfo>, key: FieldKey, value: EsexprDecodedValue) {
		let dfn = definitions.get_mut(&key.definition_name).expect("Could not find definition");

		match &mut dfn.definition {
			Definition::Record(r) =>
				self.update_record(r, &key.field_name, value),
			Definition::Enum(e) => {
				let Some(case_name) = key.case_name.as_ref() else { return; };
				self.update_enum(e, case_name, &key.field_name, value);
			},
			Definition::ExternType(_) => {},
			Definition::Interface(_) => {},
		}
	}

	fn update_record(&self, r: &mut RecordDefinition, field_name: &str, value: EsexprDecodedValue) {
		self.update_fields(&mut r.fields, field_name, value)
	}

	fn update_enum(&self, e: &mut EnumDefinition, case_name: &str, field_name: &str, value: EsexprDecodedValue) {
		let c = e.cases.iter_mut().find(|c| c.name == case_name).expect("Could not find case");
		self.update_fields(&mut c.fields, field_name, value)
	}

	fn update_fields(&self, fields: &mut [RecordField], field_name: &str, value: EsexprDecodedValue) {
		let field = fields.iter_mut().find(|f| f.name == field_name).expect("Could not find field");
		let esexpr_options = field.esexpr_options.as_mut().expect("esexpr_options are missing");

		match &mut esexpr_options.kind {
			EsexprRecordFieldKind::Keyword(_, mode) =>
				*mode = EsexprRecordKeywordMode::DefaultValue(value),

			_ => {},
		}
	}
}




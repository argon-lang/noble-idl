
use std::collections::{HashMap, HashSet};

use esexpr::{ESExprCodec, ESExprTag};
use noble_idl_api::*;

pub struct TagScannerState {
	pub tags: HashMap<QualifiedName, HashSet<ESExprTag>>,
}

pub struct TagScanner<'a> {
	pub definitions: &'a HashMap<QualifiedName, DefinitionInfo>,
	pub state: &'a mut TagScannerState,
}

impl <'a> TagScanner<'a> {
	fn scan<'b, 'c, 'd>(&'b mut self, state: &mut ScanState<'c>, name: &'d QualifiedName) -> HashSet<ESExprTag> where 'a: 'c {
		if let Some(tags) = self.state.tags.get(name) {
			return tags.clone();
		}

		let Some((name, def)) = self.definitions.get_key_value(name) else {
			return HashSet::new();
		};

		if !state.seen_types.insert(name) {
			return HashSet::new();
		}

		let tags = match &def.definition {
			Definition::Record(_) => self.scan_record(def),
			Definition::Enum(e) => self.scan_enum(state, def, e),
			Definition::ExternType(_) => self.scan_extern_type(state, def),
			Definition::Interface(_) => HashSet::new(),
		};

		state.seen_types.remove(name);

		self.state.tags.insert(name.clone(), tags.clone());
		tags
	}

	fn scan_record(&mut self, def: &DefinitionInfo) -> HashSet<ESExprTag> {
		let constructor = def.annotations
			.iter()
			.filter(|ann| ann.scope == "esexpr")
			.filter_map(|ann| ESExprAnnRecord::decode_esexpr(ann.value.clone()).ok())
			.find_map(|ann| match ann {
				ESExprAnnRecord::Constructor(constructor) => Some(constructor),
				_ => None,
			})
			.unwrap_or_else(|| def.name.name().to_owned());

		HashSet::from([ ESExprTag::Constructor(constructor) ])
	}

	fn scan_enum<'c>(&mut self, state: &mut ScanState<'c>, def: &DefinitionInfo, e: &'a EnumDefinition) -> HashSet<ESExprTag> where 'a: 'c {
		let mut tags = HashSet::new();

		for c in &e.cases {
			let has_inline_value = c.annotations
				.iter()
				.filter(|ann| ann.scope == "esexpr")
				.filter_map(|ann| ESExprAnnEnumCase::decode_esexpr(ann.value.clone()).ok())
				.any(|ann| match ann {
					ESExprAnnEnumCase::InlineValue => true,
					_ => false,
				});

			let case_tags =
				if has_inline_value {
					match &c.fields[..] {
						[field] => self.scan_type(state, &field.field_type),
						_ => HashSet::new(),
					}
				}
				else {
					let constructor = c.annotations
						.iter()
						.filter(|ann| ann.scope == "esexpr")
						.filter_map(|ann| ESExprAnnEnumCase::decode_esexpr(ann.value.clone()).ok())
						.find_map(|ann| match ann {
							ESExprAnnEnumCase::Constructor(constructor) => Some(constructor),
							_ => None,
						})
						.unwrap_or_else(|| def.name.name().to_owned());

					HashSet::from([ ESExprTag::Constructor(constructor) ])
				};

			if case_tags.is_empty() {
				return case_tags;
			}


			for tag in case_tags {
				if !tags.insert(tag) {
					return HashSet::new();
				}
			}
		}

		tags
	}

	fn scan_extern_type<'c>(&mut self, state: &mut ScanState<'c>, def: &DefinitionInfo) -> HashSet<ESExprTag> where 'a: 'c {

		let literals = def.annotations.iter()
			.filter(|ann| ann.scope == "esexpr")
			.filter_map(|ann| ESExprAnnExternType::decode_esexpr(ann.value.clone()).ok())
			.find_map(|ann| match ann {
				ESExprAnnExternType::Literals(literals) => Some(literals),
				_ => None,
			})
			.unwrap_or(ESExprAnnExternTypeLiterals {
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
			});

		let mut tags = HashSet::new();

		if let Some(build_literal_from) = literals.build_literal_from {
			let build_from_type_name = match build_literal_from {
				TypeExpr::DefinedType(name, _) => name,
				TypeExpr::TypeParameter(_) => return tags,
			};

			let from_tags = self.scan(state, &build_from_type_name);
			if from_tags.is_empty() {
				return from_tags;
			}

			for tag in from_tags {
				match tag {
					tag @ ESExprTag::Constructor(_) => {
						tags.insert(tag);
					},
					_ => {},
				}
			}
		}

		if literals.allow_bool {
			tags.insert(ESExprTag::Bool);
		}

		if literals.allow_int {
			tags.insert(ESExprTag::Int);
		}

		if literals.allow_str {
			tags.insert(ESExprTag::Str);
		}

		if literals.allow_binary {
			tags.insert(ESExprTag::Binary);
		}

		if literals.allow_float32 {
			tags.insert(ESExprTag::Float32);
		}

		if literals.allow_float64 {
			tags.insert(ESExprTag::Float64);
		}

		if literals.allow_null {
			tags.insert(ESExprTag::Null);
		}

		tags
	}

	fn scan_type<'c>(&mut self, state: &mut ScanState<'c>, t: &'a TypeExpr) -> HashSet<ESExprTag> where 'a: 'c {
		match t {
			TypeExpr::DefinedType(name, _) => self.scan(state, name),
			TypeExpr::TypeParameter(_) => HashSet::new(),
		}
	}

	pub fn scan_type_for(&mut self, t: &'a TypeExpr, owner_name: &QualifiedName) -> HashSet<ESExprTag> {
		let mut state = ScanState {
			seen_types: HashSet::from([ owner_name ]),
		};
		self.scan_type(&mut state, t)
	}
}

struct ScanState<'b> {
	seen_types: HashSet<&'b QualifiedName>,
}


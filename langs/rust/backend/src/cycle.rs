use std::{borrow::Borrow, collections::{HashMap, HashSet}, hash::Hash};

use esexpr::ESExprCodec;
use noble_idl_api::*;

use crate::annotations::RustAnnExternTypeParameter;


#[derive(Debug, Clone, Copy, PartialEq, Eq, Hash)]
enum SimpleType<'a> {
	DefinedType(&'a QualifiedName),
	TypeParameter(&'a str),
}

pub struct CycleScanner<'a, QualName, DefInfo> {
	definitions: &'a HashMap<QualName, DefInfo>,
	referenced_types: HashMap<&'a QualifiedName, HashSet<SimpleType<'a>>>,
}

struct ScanState<'a> {
	start_type: &'a QualifiedName,
	seen_types: HashSet<&'a QualifiedName>,
	is_complete: bool,
}

impl <'a, QualName: Borrow<QualifiedName> + Eq + Hash, DefInfo: Borrow<DefinitionInfo>> CycleScanner<'a, QualName, DefInfo> {
	pub fn new(definitions: &'a HashMap<QualName, DefInfo>) -> Self {
		Self {
			definitions,
			referenced_types: HashMap::new(),
		}
	}

	pub fn field_contains_cycle(&mut self, owning_type: &'a QualifiedName, t: &'a TypeExpr) -> bool {
		let mut state = ScanState {
			start_type: owning_type,
			seen_types: HashSet::new(),
			is_complete: true,
		};

		let mut types = HashSet::new();

		self.scan_type(&mut state, &mut types, t);

		types.contains(&SimpleType::DefinedType(owning_type))
	}


	fn scan_type(&mut self, state: &mut ScanState<'a>, types: &mut HashSet<SimpleType<'a>>, t: &'a TypeExpr) {
		match t {
			TypeExpr::DefinedType(name, args) => {
				types.insert(SimpleType::DefinedType(name));

				let dfn = self.definitions.get(name).expect("Undefined type was referenced").borrow();


				let mapping = dfn.type_parameters.iter()
					.map(|tp| tp.name())
					.zip(args)
					.collect::<HashMap<_, _>>();

				let type_scan = self.scan_definition(state, dfn);

				for st in type_scan {
					match st {
						SimpleType::DefinedType(_) => {
							types.insert(st);
						},
						SimpleType::TypeParameter(name) => {
							let t2 = *mapping.get(name).expect("An undefined type parameter was referenced");
							self.scan_type(state, types, t2);
						},
					}
				}
			},

			TypeExpr::TypeParameter(name) => {
				types.insert(SimpleType::TypeParameter(name));
			},
		}
	}

	fn scan_definition<'b>(&'b mut self, state: &mut ScanState<'a>, dfn: &'a DefinitionInfo) -> HashSet<SimpleType<'a>> {
		if self.referenced_types.contains_key(&dfn.name) {
			return self.referenced_types.get(&dfn.name).unwrap().clone();
		}

		if state.start_type == &dfn.name {
			return HashSet::new();
		}

		if !state.seen_types.insert(&dfn.name) {
			state.is_complete = false;
			return HashSet::new();
		}

		let old_is_complete = state.is_complete;
		state.is_complete = true;

		let result = match &dfn.definition {
			Definition::Record(r) => self.scan_record(state, r),
			Definition::Enum(e) => self.scan_enum(state, e),
			Definition::SimpleEnum(_) => HashSet::new(),
			Definition::ExternType(_) => self.scan_extern_type(dfn),
			Definition::Interface(_) => HashSet::new(),
		};

		state.seen_types.remove(&dfn.name);

		if state.is_complete {
			self.referenced_types.entry(&dfn.name).or_insert(result.clone());
		}

		state.is_complete &= old_is_complete;

		result
	}

	fn scan_record(&mut self, state: &mut ScanState<'a>, r: &'a RecordDefinition) -> HashSet<SimpleType<'a>> {
		let mut types = HashSet::new();
		self.scan_fields(state, &mut types, &r.fields);
		types
	}

	fn scan_enum(&mut self, state: &mut ScanState<'a>, e: &'a EnumDefinition) -> HashSet<SimpleType<'a>> {
		let mut types = HashSet::new();
		for c in &e.cases {
			self.scan_fields(state, &mut types, &c.fields);
		}
		types
	}

	fn scan_extern_type(&mut self, dfn: &'a DefinitionInfo) -> HashSet<SimpleType<'a>> {
		dfn.type_parameters.iter()
			.filter(|tp| {
				let is_boxed_usage = tp.annotations()
					.iter()
					.filter(|ann| ann.scope == "rust")
					.filter_map(|ann| RustAnnExternTypeParameter::decode_esexpr(ann.value.clone()).ok())
					.any(|ann| match ann {
						RustAnnExternTypeParameter::BoxedUsage => true,
					});

				!is_boxed_usage
			})
			.map(|tp| SimpleType::TypeParameter(tp.name()))
			.collect()
	}

	fn scan_fields(&mut self, state: &mut ScanState<'a>, types: &mut HashSet<SimpleType<'a>>, fields: &'a [RecordField]) {
		for f in fields {
			self.scan_type(state, types, &f.field_type)
		}
	}
}



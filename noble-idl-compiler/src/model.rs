use std::{borrow::Cow, collections::{hash_map, HashMap, HashSet}};

use esexpr::{ESExpr, ESExprCodec, ESExprTag};
use noble_idl_api::{self as api, ESExprAnnExternTypeLiterals, NobleIDLModel};


use crate::ast::*;

#[derive(Debug)]
pub enum CheckError {
    UnknownType(QualifiedName),
    DuplicateRecordField(QualifiedName, Option<String>, String),
    DuplicateEnumCase(QualifiedName, String),
    DuplicateMethod(QualifiedName, String),
    DuplicateMethodParameter(QualifiedName, String, String),
    DuplicateTypeParameter(QualifiedName, Option<String>, String),
    DuplicateDefinition(QualifiedName),
    TypeInMultiplePackages(String, Vec<PackageName>),

    TypeParameterMismatch { expected: usize, actual: usize, },


	InvalidESExprAnnotation(QualifiedName),
	DuplicateESExprAnnotation(QualifiedName, Vec<String>, String),
	ESExprAnnotationWithoutDerive(QualifiedName, Vec<String>),
	ESExprMemberCodecMissing(QualifiedName, Option<String>, String),
	ESExprDuplicateTag(QualifiedName, ESExprTag),
	ESExprInlineValueNotSingleField(QualifiedName, String),
	ESExprInlineValueInvalidTags(QualifiedName, String),
	ESExprEnumCaseIncompatibleOptions(QualifiedName, String),
	ESExprFieldIncompatibleOptions(QualifiedName, Option<String>, String),
	ESExprDictBeforeKeyword(QualifiedName, Option<String>, String),
	ESExprVarargBeforePositional(QualifiedName, Option<String>, String),
	ESExprMultipleDict(QualifiedName, Option<String>, String),
	ESExprMultipleVararg(QualifiedName, Option<String>, String),
	ESExprDuplicateKeyword(QualifiedName, Option<String>, String),
	ESExprInvalidDefaultValue(QualifiedName, Option<String>, String),
	ESExprBuildLiteralFromCodecMissing(QualifiedName),
	ESExprInvalidOptionalFieldType(QualifiedName, Option<String>, String),
	ESExprInvalidDictFieldType(QualifiedName, Option<String>, String),
	ESExprInvalidVarargFieldType(QualifiedName, Option<String>, String),
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

    fn into_api(self) -> noble_idl_api::DefinitionInfo {
        match self.def {
            Definition::Record(r) => r.into_api(self.package, self.is_library),
            Definition::Enum(e) => e.into_api(self.package, self.is_library),
            Definition::ExternType(ext) => ext.into_api(self.package, self.is_library),
            Definition::Interface(iface) => iface.into_api(self.package, self.is_library),
        }
    }
}


trait TypeScope {
    fn resolve_type(&self, name: QualifiedName, args: Vec<TypeExpr>) -> Result<TypeExpr, CheckError>;
}


struct DefinitionEntry {
    def: DefinitionInfo,
    metadata: TypeMetadata,
}

struct TypeMetadata {
	parameter_count: usize,
}

struct ModelTypes {
    definitions: HashMap<QualifiedName, TypeMetadata>,
}


#[derive(Clone, Copy)]
struct GlobalScope<'a> {
    package: &'a PackageName,
    imports: &'a [PackageName],
    types: &'a ModelTypes,
}

impl <'a> TypeScope for GlobalScope<'a> {
    fn resolve_type(&self, mut full_name: QualifiedName, args: Vec<TypeExpr>) -> Result<TypeExpr, CheckError> {
        if full_name.0.0.is_empty() {
            full_name.0 = self.package.clone();

            if let Some(metadata) = self.types.definitions.get(&full_name) {
				let expected = metadata.parameter_count;
				let actual = args.len();
				if expected != actual {
					return Err(CheckError::TypeParameterMismatch { expected, actual });
				}

                return Ok(TypeExpr::DefinedType(full_name, args));
            }

            let mut check_import_package = |mut package| {
                std::mem::swap(&mut package, &mut full_name.0);

				let res = self.types.definitions.get(&full_name).map(|metadata| {
					let expected = metadata.parameter_count;
					let actual = args.len();
					if expected != actual {
						return Err(CheckError::TypeParameterMismatch { expected, actual });
					}

					return Ok(());
				});

                std::mem::swap(&mut package, &mut full_name.0);

                res.map(|res| res.map(|_| package))
            };

            if let Some(package) = check_import_package(PackageName(vec!())) {
                full_name.0 = package?;
                return Ok(TypeExpr::DefinedType(full_name, args));
            }


            let mut matching_defs: Vec<_> = self.imports.iter()
                .filter_map(move |package| check_import_package(package.clone()))
                .collect();

            if matching_defs.len() > 0 {
                if matching_defs.len() == 1 {
                    let package = matching_defs.swap_remove(0);
                    full_name.0 = package?;
                    Ok(TypeExpr::DefinedType(full_name, args))
                }
                else {
                    Err(CheckError::TypeInMultiplePackages(
                        full_name.1.clone(),
                        matching_defs.into_iter()
                            .map(|package| package)
                            .collect::<Result<Vec<_>, _>>()?,
                    ))
                }
            }
            else {
                full_name.0.0.clear();
                Err(CheckError::UnknownType(full_name.clone()))
            }
        }
        else {
			if let Some(metadata) = self.types.definitions.get(&full_name) {
				let expected = metadata.parameter_count;
				let actual = args.len();
				if expected != actual {
					return Err(CheckError::TypeParameterMismatch { expected, actual });
				}

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

                return Ok(TypeExpr::TypeParameter(name.1));
            }
        }

        self.parent_scope.resolve_type(name, args)
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
                let metadata = get_type_metadata(&def.def);
                ve.insert(DefinitionEntry { def, metadata })
            },
        };

        Ok(())
    }

    pub(crate) fn check(self) -> Result<NobleIDLModel, CheckError> {
        let mut types = HashMap::new();
        let mut definitions = HashMap::new();

        for (qual_name, entry) in self.definitions {
            types.insert(qual_name.clone(), entry.metadata);
            definitions.insert(qual_name, entry.def);
        }

        let model_types = ModelTypes {
            definitions: types,
        };

        for (definition_name, def) in &mut definitions {
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
				Definition::ExternType(et) => checker.check_extern_type(et)?,
				Definition::Interface(iface) => checker.check_interface(iface)?,
			}
        }


		let definitions: HashMap<_, _> = definitions.into_iter()
			.map(|(k, v)| (k, v.into_api()))
			.collect();

		let mut esexpr_codecs = HashMap::new();
		let mut tag_scanner = TagScanner {
			definitions: &definitions,
			tags: HashMap::new(),
		};

        for def in definitions.values() {
			let mut checker = ESExprChecker {
				definitions: &definitions,
				definition_name: &def.name,
				esexpr_codecs: &mut esexpr_codecs,
				tag_scanner: &mut tag_scanner,
			};

			checker.check_definition(def)?;
        }

        let model_definitions = definitions.into_iter()
            .map(|(_, v)| v)
            .collect();

        Ok(NobleIDLModel {
            definitions: model_definitions,
        })
    }
}

fn get_type_metadata(def: &Definition) -> TypeMetadata {
    match def {
        Definition::Record(rec) => TypeMetadata { parameter_count: rec.type_parameters.len() },
        Definition::Enum(e) => TypeMetadata { parameter_count: e.type_parameters.len() },
        Definition::ExternType(et) => TypeMetadata { parameter_count: et.type_parameters.len() },
        Definition::Interface(iface) => TypeMetadata { parameter_count: iface.type_parameters.len() },
    }
}



struct ModelChecker<'a, Scope> {
    scope: Scope,
	definition_name: &'a QualifiedName,
}

impl <'a, Scope: TypeScope + Copy> ModelChecker<'a, Scope> {
	fn with_type_parameters<'b>(&'b self, type_parameters: &'b [TypeParameter]) -> ModelChecker<'b, TypeParameterScope<Scope>> {
		ModelChecker {
			scope: TypeParameterScope {
				parent_scope: self.scope,
				type_parameters,
			},
			definition_name: self.definition_name,
		}
	}

	fn check_record(&self, rec: &mut RecordDefinition) -> Result<(), CheckError> {
		self.check_type_parameters(None, &rec.type_parameters)?;

		let inner = self.with_type_parameters(&rec.type_parameters);

		inner.check_fields(None, &mut rec.fields)?;

		Ok(())
	}

	fn check_enum(&self, e: &mut EnumDefinition) -> Result<(), CheckError> {
		self.check_type_parameters( None, &e.type_parameters)?;

		let inner = self.with_type_parameters(&e.type_parameters);

		let mut case_names = HashSet::new();

		for c in &mut e.cases {
			if let Some(name) = case_names.replace(c.name.clone()) {
				return Err(CheckError::DuplicateEnumCase(self.definition_name.clone(), name));
			}

			inner.check_fields(Some(c.name.as_str()), &mut c.fields)?;
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

		let inner = self.with_type_parameters(&iface.type_parameters);

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

			let inner = inner.with_type_parameters(&method.type_parameters);

			let mut param_names = HashSet::new();

			for param in &mut method.parameters {
				if let Some(name) = param_names.replace(param.name.clone()) {
					return Err(CheckError::DuplicateMethodParameter(self.definition_name.clone(), method.name.clone(), name));
				}

				inner.check_type(&mut param.parameter_type)?;
			}

			inner.check_type(&mut method.return_type)?;
		}

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

			TypeExpr::DefinedType(..) | TypeExpr::TypeParameter(_) => TypeResult::Success(t),
		}
	}

}

enum TypeResult {
    Success(TypeExpr),
    Failure(TypeExpr, CheckError),
}


struct TagScanner<'a> {
	definitions: &'a HashMap<QualifiedName, api::DefinitionInfo>,
	tags: HashMap<&'a QualifiedName, HashSet<ESExprTag>>,
}

impl <'a> TagScanner<'a> {
	fn scan<'b, 'c, 'd>(&'b mut self, state: &mut ScanState<'c>, name: &'d QualifiedName) -> HashSet<ESExprTag> where 'a: 'c {
		if let Some(tags) = self.tags.get(name) {
			return tags.clone();
		}

		let Some((name, def)) = self.definitions.get_key_value(name) else {
			return HashSet::new();
		};

		if !state.seen_types.insert(name) {
			return HashSet::new();
		}

		let tags = match &def.definition {
			api::Definition::Record(_) => self.scan_record(def),
			api::Definition::Enum(e) => self.scan_enum(state, def, e),
			api::Definition::ExternType => self.scan_extern_type(state, def),
			api::Definition::Interface(_) => HashSet::new(),
		};

		state.seen_types.remove(name);

		self.tags.insert(name, tags.clone());
		tags
	}

	fn scan_record(&mut self, def: &api::DefinitionInfo) -> HashSet<ESExprTag> {
		let constructor = def.annotations
			.iter()
			.filter(|ann| ann.scope == "esexpr")
			.filter_map(|ann| api::ESExprAnnRecord::decode_esexpr(ann.value.clone()).ok())
			.find_map(|ann| match ann {
				api::ESExprAnnRecord::Constructor(constructor) => Some(constructor),
				_ => None,
			})
			.unwrap_or_else(|| def.name.name().to_owned());

		HashSet::from([ ESExprTag::Constructor(constructor) ])
	}

	fn scan_enum<'c>(&mut self, state: &mut ScanState<'c>, def: &api::DefinitionInfo, e: &'a api::EnumDefinition) -> HashSet<ESExprTag> where 'a: 'c {
		let mut tags = HashSet::new();

		for c in &e.cases {
			let has_inline_value = c.annotations
				.iter()
				.filter(|ann| ann.scope == "esexpr")
				.filter_map(|ann| api::ESExprAnnEnumCase::decode_esexpr(ann.value.clone()).ok())
				.any(|ann| match ann {
					api::ESExprAnnEnumCase::InlineValue => true,
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
						.filter_map(|ann| api::ESExprAnnEnumCase::decode_esexpr(ann.value.clone()).ok())
						.find_map(|ann| match ann {
							api::ESExprAnnEnumCase::Constructor(constructor) => Some(constructor),
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

	fn scan_extern_type<'c>(&mut self, state: &mut ScanState<'c>, def: &api::DefinitionInfo) -> HashSet<ESExprTag> where 'a: 'c {

		let literals = def.annotations.iter()
			.filter(|ann| ann.scope == "esexpr")
			.filter_map(|ann| api::ESExprAnnExternType::decode_esexpr(ann.value.clone()).ok())
			.find_map(|ann| match ann {
				api::ESExprAnnExternType::Literals(literals) => Some(literals),
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
			let from_tags = self.scan(state, &build_literal_from);
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

	fn scan_type<'c>(&mut self, state: &mut ScanState<'c>, t: &'a api::TypeExpr) -> HashSet<ESExprTag> where 'a: 'c {
		match t {
			api::TypeExpr::DefinedType(name, _) => self.scan(state, name),
			api::TypeExpr::TypeParameter(_) => HashSet::new(),
		}
	}

	fn scan_type_for(&mut self, t: &'a api::TypeExpr, owner_name: &QualifiedName) -> HashSet<ESExprTag> {
		let mut state = ScanState {
			seen_types: HashSet::from([ owner_name ]),
		};
		self.scan_type(&mut state, t)
	}
}

struct ScanState<'b> {
	seen_types: HashSet<&'b QualifiedName>,
}

struct ESExprChecker<'a, 'b> {
	definition_name: &'a QualifiedName,
	definitions: &'a HashMap<QualifiedName, api::DefinitionInfo>,
	esexpr_codecs: &'b mut HashMap<QualifiedName, bool>,
	tag_scanner: &'b mut TagScanner<'a>,
}

impl <'a, 'b> ESExprChecker<'a, 'b> {
	fn def_has_esexpr_codec(&mut self, name: &QualifiedName) -> bool {
		match self.esexpr_codecs.entry(name.clone()) {
			hash_map::Entry::Occupied(oe) => *oe.get(),
			hash_map::Entry::Vacant(ve) => {
				let Some(def) = self.definitions.get(ve.key()) else { return false; };

				let res = match def.definition {
					api::Definition::Record(_) =>
						def.annotations
							.iter()
							.filter(|ann| ann.scope == "esexpr")
							.filter_map(|ann| api::ESExprAnnRecord::decode_esexpr(ann.value.clone()).ok())
							.any(|ann| match ann {
								api::ESExprAnnRecord::DeriveCodec => true,
								_ => false
							}),

					api::Definition::Enum(_) =>
						def.annotations
							.iter()
							.filter(|ann| ann.scope == "esexpr")
							.filter_map(|ann| api::ESExprAnnEnum::decode_esexpr(ann.value.clone()).ok())
							.any(|ann| match ann {
								api::ESExprAnnEnum::DeriveCodec => true,
								_ => false
							}),

					api::Definition::ExternType =>
						def.annotations
							.iter()
							.filter(|ann| ann.scope == "esexpr")
							.filter_map(|ann| api::ESExprAnnExternType::decode_esexpr(ann.value.clone()).ok())
							.any(|ann| match ann {
								api::ESExprAnnExternType::DeriveCodec => true,
								_ => false
							}),

					api::Definition::Interface(_) => false,
				};

				ve.insert(res);
				res
			},
		}
	}

	fn check_definition(&mut self, def: &'a api::DefinitionInfo) -> Result<(), CheckError> {
		match &def.definition {
			noble_idl_api::Definition::Record(r) => self.check_record(def, r),
			noble_idl_api::Definition::Enum(e) => self.check_enum(def, e),
			noble_idl_api::Definition::ExternType => self.check_extern_type(def),
			noble_idl_api::Definition::Interface(_) => Ok(()),
		}
	}

	fn check_record(&mut self, def: &'a api::DefinitionInfo, r: &'a api::RecordDefinition) -> Result<(), CheckError> {
		let mut has_derive_codec = false;
		let mut has_constructor = false;
		for ann in &def.annotations {
			if ann.scope != "esexpr" {
				continue;
			}

			let esexpr_rec = api::ESExprAnnRecord::decode_esexpr(ann.value.clone())
				.map_err(|_| CheckError::InvalidESExprAnnotation(self.definition_name.clone()))?;

			match esexpr_rec {
				api::ESExprAnnRecord::DeriveCodec => {
					if has_derive_codec {
						return Err(CheckError::DuplicateESExprAnnotation(self.definition_name.clone(), vec![], "derive-codec".to_owned()));
					}

					has_derive_codec = true;
				},
				api::ESExprAnnRecord::Constructor(_) => {
					if has_constructor {
						return Err(CheckError::DuplicateESExprAnnotation(self.definition_name.clone(), vec![], "constructor".to_owned()));
					}

					has_constructor = true;
				},
			}
		}

		if !has_derive_codec && has_constructor {
			return Err(CheckError::ESExprAnnotationWithoutDerive(self.definition_name.clone(), vec![]));
		}

		self.esexpr_codecs.insert(def.name.clone(), has_derive_codec);

		self.check_fields(&r.fields, None, has_derive_codec)?;

		Ok(())
	}

	fn check_enum(&mut self, def: &'a api::DefinitionInfo, e: &'a api::EnumDefinition) -> Result<(), CheckError> {
		let mut has_derive_codec = false;
		let mut has_simple_enum = false;
		for ann in &def.annotations {
			if ann.scope != "esexpr" {
				continue;
			}

			let esexpr_rec = api::ESExprAnnEnum::decode_esexpr(ann.value.clone())
				.map_err(|_| CheckError::InvalidESExprAnnotation(self.definition_name.clone()))?;

			match esexpr_rec {
				api::ESExprAnnEnum::DeriveCodec => {
					if has_derive_codec {
						return Err(CheckError::DuplicateESExprAnnotation(self.definition_name.clone(), vec![], "derive-codec".to_owned()));
					}

					has_derive_codec = true;
				},
				api::ESExprAnnEnum::SimpleEnum => {
					if has_simple_enum {
						return Err(CheckError::DuplicateESExprAnnotation(self.definition_name.clone(), vec![], "simple-enum".to_owned()));
					}

					has_simple_enum = true;
				},
			}
		}

		if !has_derive_codec && has_simple_enum {
			return Err(CheckError::ESExprAnnotationWithoutDerive(self.definition_name.clone(), vec![]));
		}

		self.esexpr_codecs.insert(def.name.clone(), has_derive_codec);

		let mut tags = HashSet::new();

		let mut add_tag = |tag| {
			if let Some(tag) = tags.replace(tag) {
				return Err(CheckError::ESExprDuplicateTag(self.definition_name.clone(), tag));
			}
			else {
				return Ok(())
			}
		};

		for c in &e.cases {
			let mut has_constructor = false;
			let mut has_inline_value = false;
			for ann in &c.annotations {
				if ann.scope != "esexpr" {
					continue;
				}

				if !has_derive_codec {
					return Err(CheckError::ESExprAnnotationWithoutDerive(self.definition_name.clone(), vec![ c.name.clone() ]));
				}

				let esexpr_rec = api::ESExprAnnEnumCase::decode_esexpr(ann.value.clone())
					.map_err(|_| CheckError::InvalidESExprAnnotation(self.definition_name.clone()))?;

				match esexpr_rec {
					api::ESExprAnnEnumCase::Constructor(constructor) => {
						if has_constructor {
							return Err(CheckError::DuplicateESExprAnnotation(self.definition_name.clone(), vec![], "constructor".to_owned()));
						}

						if has_inline_value {
							return Err(CheckError::ESExprEnumCaseIncompatibleOptions(self.definition_name.clone(), c.name.clone()));
						}

						has_constructor = true;

						add_tag(ESExprTag::Constructor(constructor))?;
					},
					api::ESExprAnnEnumCase::InlineValue => {
						if has_inline_value {
							return Err(CheckError::DuplicateESExprAnnotation(self.definition_name.clone(), vec![], "inline-value".to_owned()));
						}

						if has_constructor {
							return Err(CheckError::ESExprEnumCaseIncompatibleOptions(self.definition_name.clone(), c.name.clone()));
						}

						has_inline_value = true;

						let [field] = &c.fields[..] else {
							return Err(CheckError::ESExprInlineValueNotSingleField(self.definition_name.clone(), c.name.clone()));
						};

						let iv_tags = self.tag_scanner.scan_type_for(&field.field_type, self.definition_name);
						if iv_tags.is_empty() {
							return Err(CheckError::ESExprInlineValueInvalidTags(self.definition_name.clone(), field.name.clone()));
						}

						for tag in iv_tags {
							add_tag(tag)?;
						}
					},
				}
			}

			if has_derive_codec && !has_constructor && !has_inline_value {
				add_tag(ESExprTag::Constructor(c.name.clone()))?;
			}

			self.check_fields(&c.fields, Some(&c.name), has_derive_codec)?;
		}

		Ok(())
	}

	fn check_extern_type(&mut self, def: &'a api::DefinitionInfo) -> Result<(), CheckError> {
		let mut has_derive_codec = false;
		let mut has_allow_optional = false;
		let mut has_allow_vararg = false;
		let mut has_allow_dict = false;
		let mut has_literals = false;

		for ann in &def.annotations {
			if ann.scope != "esexpr" {
				continue;
			}

			let esexpr_rec = api::ESExprAnnExternType::decode_esexpr(ann.value.clone())
				.map_err(|_| CheckError::InvalidESExprAnnotation(self.definition_name.clone()))?;

			match esexpr_rec {
				api::ESExprAnnExternType::DeriveCodec => {
					if has_derive_codec {
						return Err(CheckError::DuplicateESExprAnnotation(self.definition_name.clone(), vec![], "derive-codec".to_owned()));
					}

					has_derive_codec = true;
				},
				api::ESExprAnnExternType::AllowOptional => {
					if has_allow_optional {
						return Err(CheckError::DuplicateESExprAnnotation(self.definition_name.clone(), vec![], "allow-optional".to_owned()));
					}

					has_allow_optional = true;
				},
				api::ESExprAnnExternType::AllowVararg => {
					if has_allow_vararg {
						return Err(CheckError::DuplicateESExprAnnotation(self.definition_name.clone(), vec![], "allow-vararg".to_owned()));
					}

					has_allow_vararg = true;
				},
				api::ESExprAnnExternType::AllowDict => {
					if has_allow_dict {
						return Err(CheckError::DuplicateESExprAnnotation(self.definition_name.clone(), vec![], "allow-dict".to_owned()));
					}

					has_allow_dict = true;
				},
				api::ESExprAnnExternType::Literals(literals) => {
					if has_literals {
						return Err(CheckError::DuplicateESExprAnnotation(self.definition_name.clone(), vec![], "literals".to_owned()));
					}

					has_literals = true;

					if let Some(from_type_name) = literals.build_literal_from {
						if !self.def_has_esexpr_codec(&from_type_name) {
							return Err(CheckError::ESExprBuildLiteralFromCodecMissing(from_type_name))
						}
					}
				},
			}
		}

		self.esexpr_codecs.insert(def.name.clone(), has_derive_codec);


		Ok(())
	}

	fn check_fields(&mut self, fields: &'a [api::RecordField], case_name: Option<&str>, is_esexpr_type: bool) -> Result<(), CheckError> {
		let mut keywords = HashSet::new();

		let mut has_dict = false;
		let mut has_vararg = false;

		for field in fields {
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

				let esexpr_field = api::ESExprAnnRecordField::decode_esexpr(ann.value.clone())
					.map_err(|_| CheckError::InvalidESExprAnnotation(self.definition_name.clone()))?;

				if !is_esexpr_type {
					return Err(CheckError::ESExprAnnotationWithoutDerive(self.definition_name.clone(), current_path()))
				}

				match esexpr_field {
					api::ESExprAnnRecordField::Keyword { name, required, default_value } => {
						if is_keyword {
							return Err(CheckError::DuplicateESExprAnnotation(self.definition_name.clone(), current_path(), "keyword".to_owned()));
						}

						is_keyword = true;

						if is_dict || is_vararg || (!required && default_value.is_some()) {
							return Err(CheckError::ESExprFieldIncompatibleOptions(self.definition_name.clone(), case_name.map(str::to_owned), field.name.clone()));
						}

						if has_dict {
							return Err(CheckError::ESExprDictBeforeKeyword(self.definition_name.clone(), case_name.map(str::to_owned), field.name.clone()));
						}

						let name = name.unwrap_or_else(|| field.name.clone());
						if let Some(name) = keywords.replace(name.clone()) {
							return Err(CheckError::ESExprDuplicateKeyword(self.definition_name.clone(), case_name.map(str::to_owned), name));
						}

						if let Some(value) = default_value {
							if !self.check_value(field.field_type.clone(), value) {
								return Err(CheckError::ESExprInvalidDefaultValue(self.definition_name.clone(), case_name.map(str::to_owned), name));
							}
						}

						if !required {
							self.check_optional_field_type(&field.field_type, case_name, &field.name)?;
						}
					},
					api::ESExprAnnRecordField::Dict => {
						if has_dict {
							return Err(CheckError::ESExprMultipleDict(self.definition_name.clone(), case_name.map(str::to_owned), field.name.clone()));
						}

						if is_dict {
							return Err(CheckError::DuplicateESExprAnnotation(self.definition_name.clone(), current_path(), "dict".to_owned()));
						}

						has_dict = true;
						is_dict = true;

						if is_keyword || is_vararg {
							return Err(CheckError::ESExprFieldIncompatibleOptions(self.definition_name.clone(), case_name.map(str::to_owned), field.name.clone()));
						}

						self.check_dict_type(&field.field_type, case_name, &field.name)?;
					},
					api::ESExprAnnRecordField::Vararg => {
						if has_vararg {
							return Err(CheckError::ESExprMultipleDict(self.definition_name.clone(), case_name.map(str::to_owned), field.name.clone()));
						}

						if is_vararg {
							return Err(CheckError::DuplicateESExprAnnotation(self.definition_name.clone(), current_path(), "vararg".to_owned()));
						}

						has_vararg = true;
						is_vararg = true;

						if is_keyword || is_dict {
							return Err(CheckError::ESExprFieldIncompatibleOptions(self.definition_name.clone(), case_name.map(str::to_owned), field.name.clone()));
						}

						self.check_vararg_type(&field.field_type, case_name, &field.name)?;
					},
				}
			}

			if has_vararg && !(is_keyword || is_dict || is_vararg) {
				return Err(CheckError::ESExprVarargBeforePositional(self.definition_name.clone(), case_name.map(str::to_owned), field.name.clone()));
			}

			if is_esexpr_type {
				if !self.check_type(&field.field_type) {
					return Err(CheckError::ESExprMemberCodecMissing(self.definition_name.clone(), case_name.map(str::to_owned), field.name.clone()));
				}
			}
		}

		Ok(())
	}

	fn check_type(&mut self, t: &'a api::TypeExpr) -> bool {
		match t {
			api::TypeExpr::DefinedType(name, args) =>
				self.def_has_esexpr_codec(name) && args.iter().all(|arg| self.check_type(arg)),
			api::TypeExpr::TypeParameter(_) => true,
		}
	}

	fn check_value(&mut self, t: api::TypeExpr, value: ESExpr) -> bool {
		self.check_value_impl(t, value, HashSet::new())
	}

	fn check_value_impl<'c>(&mut self, t: api::TypeExpr, value: ESExpr, mut seen_types: HashSet<api::QualifiedName>) -> bool where 'a : 'c {
		let Some((type_name, args, mapping)) = self.get_type_info(t) else { return false; };
		if !seen_types.insert(type_name.clone()) {
			return false;
		}

		let Some(def) = self.definitions.get(&type_name) else { return false; };

		match &def.definition {
			api::Definition::Interface(_) => false,

			api::Definition::Record(r) => match value {
				ESExpr::Constructor { name, args, kwargs } => {
					let constructor = def.annotations.iter()
						.filter(|ann| ann.scope == "esexpr")
						.filter_map(|ann| api::ESExprAnnRecord::decode_esexpr(ann.value.clone()).ok())
						.find_map(|ann| match ann {
							api::ESExprAnnRecord::Constructor(constructor) => Some(Cow::Owned(constructor)),
							_ => None,
						})
						.unwrap_or_else(|| Cow::Borrowed(def.name.name()));

					constructor == name && self.check_field_values(&r.fields, &mapping, args, kwargs)
				}
				_ => false,
			},

			api::Definition::Enum(e) => {
				let tag = value.tag();

				for c in &e.cases {

					let has_inline_value = def.annotations.iter()
						.filter(|ann| ann.scope == "esexpr")
						.filter_map(|ann| api::ESExprAnnEnumCase::decode_esexpr(ann.value.clone()).ok())
						.any(|ann| match ann {
							api::ESExprAnnEnumCase::InlineValue => true,
							_ => false,
						});

					if has_inline_value {
						let [field] = &c.fields[..] else { return false; };
						if !self.tag_scanner.scan_type_for(&field.field_type, &type_name).contains(&tag) {
							return false;
						}

						let mut field_type = field.field_type.clone();
						if !substitute_type(&mut field_type, &mapping) {
							return false;
						}

						return self.check_value(field_type, value);
					}

					let constructor = def.annotations.iter()
						.filter(|ann| ann.scope == "esexpr")
						.filter_map(|ann| api::ESExprAnnEnumCase::decode_esexpr(ann.value.clone()).ok())
						.find_map(|ann| match ann {
							api::ESExprAnnEnumCase::Constructor(constructor) => Some(Cow::Owned(constructor)),
							_ => None,
						})
						.unwrap_or_else(|| Cow::Borrowed(def.name.name()));

					if tag.is_constructor(constructor.as_ref()) {
						return match value {
							ESExpr::Constructor { name: _, args, kwargs } => {
								self.check_field_values(&c.fields, &mapping, args, kwargs)
							},

							_ => false,
						};
					}
				}

				false
			},

			api::Definition::ExternType => {
				let literals = def.annotations.iter()
					.filter(|ann| ann.scope == "esexpr")
					.filter_map(|ann| api::ESExprAnnExternType::decode_esexpr(ann.value.clone()).ok())
					.find_map(|ann| match ann {
						api::ESExprAnnExternType::Literals(literals) => Some(literals),
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



				match value {
					ctor @ ESExpr::Constructor { .. } => {
						let Some(build_literal_from) = literals.build_literal_from else { return false };

						let Some(from_def) = self.definitions.get(&build_literal_from) else { return false; };
						if from_def.type_parameters.len() != args.len() {
							return false;
						}

						let from_type = api::TypeExpr::DefinedType(build_literal_from, args);

						self.check_value_impl(from_type, ctor, seen_types)
					}
					ESExpr::Bool(_) => literals.allow_bool,
					ESExpr::Int(i) =>
						literals.allow_int &&
							literals.min_int.map_or(true, |min| i >= min) &&
							literals.max_int.map_or(true, |max| i <= max),

					ESExpr::Str(_) => literals.allow_str,
					ESExpr::Binary(_) => literals.allow_binary,
					ESExpr::Float32(_) => literals.allow_float32,
					ESExpr::Float64(_) => literals.allow_float64,
					ESExpr::Null => literals.allow_null,
				}
			},
		}
	}

	fn check_field_values(&mut self, fields: &[api::RecordField], mapping: &HashMap<String, api::TypeExpr>, mut args: Vec<ESExpr>, mut kwargs: HashMap<String, ESExpr>) -> bool {
		for field in fields {

			let is_vararg = field.annotations.iter()
				.filter(|ann| ann.scope == "esexpr")
				.filter_map(|ann| api::ESExprAnnRecordField::decode_esexpr(ann.value.clone()).ok())
				.any(|ann| match ann {
					api::ESExprAnnRecordField::Vararg => true,
					_ => false,
				});


			if is_vararg {
				let Some(mut field_type) = self.get_single_type_arg(field.field_type.clone()) else { return false; };
				if !substitute_type(&mut field_type, &mapping) {
					return false;
				}

				for arg in args.drain(..) {
					if !self.check_value(field_type.clone(), arg) {
						return false;
					}
				}

				continue;
			}

			let is_dict = field.annotations.iter()
				.filter(|ann| ann.scope == "esexpr")
				.filter_map(|ann| api::ESExprAnnRecordField::decode_esexpr(ann.value.clone()).ok())
				.any(|ann| match ann {
					api::ESExprAnnRecordField::Dict => true,
					_ => false,
				});


			if is_dict {
				let Some(mut field_type) = self.get_single_type_arg(field.field_type.clone()) else { return false; };
				if !substitute_type(&mut field_type, &mapping) {
					return false;
				}

				for (_, arg) in kwargs.drain() {
					if !self.check_value(field_type.clone(), arg) {
						return false;
					}
				}

				continue;
			}



			let mut is_keyword = false;

			for ann in &field.annotations {
				if ann.scope != "esexpr" {
					continue;
				}

				let Ok(ann) = api::ESExprAnnRecordField::decode_esexpr(ann.value.clone()) else {
					continue;
				};

				match ann {
					api::ESExprAnnRecordField::Keyword { name, required, default_value } => {
						is_keyword = true;

						let name = name.as_ref().map(String::as_str).unwrap_or(&field.name);

						let value = kwargs.remove(name);

						if !required {
							match value {
								Some(value) => {
									let mut field_type = field.field_type.clone();
									if !substitute_type(&mut field_type, &mapping) {
										return false;
									}

									if !self.check_value(field_type, value) {
										return false;
									}
								},
								None => if default_value.is_none() { return false },
							}
						}
						else {
							if let Some(value) = value {
								let Some(mut field_type) = self.get_single_type_arg(field.field_type.clone()) else { return false; };
								if !substitute_type(&mut field_type, &mapping) {
									return false;
								}

								if !self.check_value(field_type, value) {
									return false;
								}
							}
						}

						break;
					},
					_ => {},
				}
			}

			if is_keyword {
				continue;
			}


			let mut field_type = field.field_type.clone();
			if !substitute_type(&mut field_type, &mapping) {
				return false;
			}

			if args.is_empty() {
				return false;
			}

			let value = args.remove(0);
			if !self.check_value(field_type, value) {
				return false;
			}
		}

		args.is_empty() && kwargs.is_empty()
	}

	fn get_single_type_arg(&self, t: api::TypeExpr) -> Option<api::TypeExpr> {
		let api::TypeExpr::DefinedType(_, mut args) = t else { return None; };

		if args.len() != 1 {
			return None;
		}

		args.pop()
	}

	fn check_optional_field_type(&self, t: &api::TypeExpr, case_name: Option<&str>, field_name: &str) -> Result<(), CheckError> {
		let is_optional_field_type = self.get_type_name(t)
			.and_then(|name| self.definitions.get(name))
			.filter(|def| match &def.definition {
				noble_idl_api::Definition::ExternType => true,
				_ => false,
			})
			.map(|def|
				def.annotations.iter()
					.filter(|ann| ann.scope == "esexpr")
					.filter_map(|ann| api::ESExprAnnExternType::decode_esexpr(ann.value.clone()).ok())
					.any(|ann| match ann {
						api::ESExprAnnExternType::AllowOptional => true,
						_ => false
					})
			)
			.unwrap_or(false);

		if !is_optional_field_type {
			return Err(CheckError::ESExprInvalidOptionalFieldType(self.definition_name.clone(), case_name.map(str::to_owned), field_name.to_owned()));
		}

		Ok(())
	}

	fn check_dict_type(&self, t: &api::TypeExpr, case_name: Option<&str>, field_name: &str) -> Result<(), CheckError> {
		let is_optional_field_type = self.get_type_name(t)
			.and_then(|name| self.definitions.get(name))
			.filter(|def| match &def.definition {
				noble_idl_api::Definition::ExternType => true,
				_ => false,
			})
			.map(|def|
				def.annotations.iter()
					.filter(|ann| ann.scope == "esexpr")
					.filter_map(|ann| api::ESExprAnnExternType::decode_esexpr(ann.value.clone()).ok())
					.any(|ann| match ann {
						api::ESExprAnnExternType::AllowDict => true,
						_ => false
					})
			)
			.unwrap_or(false);

		if !is_optional_field_type {
			return Err(CheckError::ESExprInvalidDictFieldType(self.definition_name.clone(), case_name.map(str::to_owned), field_name.to_owned()));
		}

		Ok(())
	}

	fn check_vararg_type(&self, t: &api::TypeExpr, case_name: Option<&str>, field_name: &str) -> Result<(), CheckError> {
		let is_optional_field_type = self.get_type_name(t)
			.and_then(|name| self.definitions.get(name))
			.filter(|def| match &def.definition {
				noble_idl_api::Definition::ExternType => true,
				_ => false,
			})
			.map(|def|
				def.annotations.iter()
					.filter(|ann| ann.scope == "esexpr")
					.filter_map(|ann| api::ESExprAnnExternType::decode_esexpr(ann.value.clone()).ok())
					.any(|ann| match ann {
						api::ESExprAnnExternType::AllowVararg => true,
						_ => false
					})
			)
			.unwrap_or(false);

		if !is_optional_field_type {
			return Err(CheckError::ESExprInvalidVarargFieldType(self.definition_name.clone(), case_name.map(str::to_owned), field_name.to_owned()));
		}

		Ok(())
	}

	fn get_type_info(&self, t: api::TypeExpr) -> Option<(api::QualifiedName, Vec<api::TypeExpr>, HashMap<String, api::TypeExpr>)> {
		match t {
			api::TypeExpr::DefinedType(name, args) => {
				let def = self.definitions.get(&name)?;
				let type_params = def.type_parameters.iter().map(|tp| tp.name().to_owned()).zip(args.iter().cloned()).collect();

				Some((name, args.clone(), type_params))
			},
			api::TypeExpr::TypeParameter(_) => None,
		}
	}

	fn get_type_name<'c>(&self, t: &'c api::TypeExpr) -> Option<&'c api::QualifiedName> {
		match t {
			api::TypeExpr::DefinedType(name, _) => Some(name),
			api::TypeExpr::TypeParameter(_) => None,
		}
	}



}


fn substitute_type(t: &mut api::TypeExpr, mapping: &HashMap<String, api::TypeExpr>) -> bool {
	match t {
		api::TypeExpr::DefinedType(_, args) => args.iter_mut().all(|arg| substitute_type(arg, mapping)),
		api::TypeExpr::TypeParameter(name) => {
			let Some(mapped_type) = mapping.get(name.as_str()) else { return false; };
			*t = (*mapped_type).clone();
			true
		},
	}
}


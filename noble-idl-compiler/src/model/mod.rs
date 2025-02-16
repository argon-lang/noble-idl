use std::collections::{hash_map, HashMap, HashSet};
use esexpr::{DecodeError, ESExprTag};
use itertools::Itertools;
use noble_idl_api::NobleIdlModel;
use tag_scanner::TagScannerState;

mod tag_scanner;

mod phase1; // Phase 1 - Type resolution
mod phase2; // Phase 2 - Type checking
mod phase3; // Phase 3 - Parse extern metdata, defer checking element types
mod phase4; // Phase 4 - Parse ESExpr metadata, excluding default values
mod phase5; // Phase 5 - Process default values
mod phase6; // Phase 6 - Check type codecs
mod phase7; // Phase 7 - Remove annotations

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

	InvalidExceptionType(noble_idl_api::TypeExpr),
    ExceptionTypeParameterNotAllowed(String),
    ExceptionTypeNotAllowed(noble_idl_api::TypeExpr),

    TypeParameterMismatch { expected: usize, actual: usize, },


	InvalidESExprAnnotation(QualifiedName, DecodeError),
	DuplicateESExprAnnotation(QualifiedName, Vec<String>, String),
	ESExprAnnotationWithoutDerive(QualifiedName, Vec<String>),
	ESExprExternTypeCodecMissing(QualifiedName),
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
	ESExprVarargAfterOptionalPositional(QualifiedName, Option<String>, String),
	ESExprMultipleOptionalPositional(QualifiedName, Option<String>, String),
	ESExprDuplicateKeyword(QualifiedName, Option<String>, String),
	ESExprInvalidDefaultValue(String, QualifiedName, Option<String>, String),
	ESExprBuildLiteralFromCodecMissing(QualifiedName),
	ESExprInvalidOptionalFieldType(QualifiedName, Option<String>, String),
	ESExprInvalidDictFieldType(QualifiedName, Option<String>, String),
	ESExprInvalidVarargFieldType(QualifiedName, Option<String>, String),
	ESExprInvalidElementType(QualifiedName),
}


pub(crate) struct DefinitionInfo {
    pub package: PackageName,
    pub imports: Vec<PackageName>,
    pub def: Definition,
    pub is_library: bool,
}

impl DefinitionInfo {
    fn qualified_name(&self) -> QualifiedName {
        QualifiedName(Box::new(self.package.clone()), self.def.name().to_owned())
    }

    fn into_api(self) -> noble_idl_api::DefinitionInfo {
        match self.def {
            Definition::Record(r) => r.into_api(self.package, self.is_library),
            Definition::Enum(e) => e.into_api(self.package, self.is_library),
			Definition::SimpleEnum(e) => e.into_api(self.package, self.is_library),
            Definition::ExternType(ext) => ext.into_api(self.package, self.is_library),
            Definition::Interface(iface) => iface.into_api(self.package, self.is_library),
            Definition::ExceptionType(ex) => ex.into_api(self.package, self.is_library),
        }
    }
}

pub(crate) struct ModelBuilder {
    definitions: HashMap<QualifiedName, DefinitionInfo>,
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
                ve.insert(def)
            },
        };

        Ok(())
    }

    pub(crate) fn check(self) -> Result<NobleIdlModel, CheckError> {
        let mut types = HashSet::new();
        let mut definitions = HashMap::new();

        for (qual_name, entry) in self.definitions {
            types.insert(qual_name.clone());
            definitions.insert(qual_name, entry);
        }


		phase1::run(&mut definitions, &types)?;


		let mut definitions: HashMap<_, _> = definitions.into_iter()
			.map(|(k, v)| (k, v.into_api()))
			.collect();

		phase2::run(&definitions)?;

		let phase3_state = phase3::run(&mut definitions)?;
		let phase4_state = phase4::run(&mut definitions, &phase3_state)?;

		let mut tag_scan_state = TagScannerState {
			tags: HashMap::new(),
		};
		phase5::run(&mut definitions, &mut tag_scan_state)?;
		phase6::run(&definitions, &phase4_state, &mut tag_scan_state)?;
		phase7::run(&mut definitions);

        let mut model_definitions = definitions.into_values().map(Box::new).collect_vec();
		model_definitions.sort_by_key(|dfn| dfn.name.clone());

        Ok(NobleIdlModel {
            definitions: model_definitions,
        })
    }
}



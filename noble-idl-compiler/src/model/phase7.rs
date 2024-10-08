use std::collections::HashMap;

use noble_idl_api::*;


pub fn run(definitions: &mut HashMap<QualifiedName, DefinitionInfo>) {
	for dfn in definitions.values_mut() {
		remove_definition(dfn);
	}
}


fn non_esexpr_ann(ann: &Box<Annotation>) -> bool {
	ann.scope != "esexpr"
}

fn remove_definition(dfn: &mut DefinitionInfo) {
	dfn.annotations.retain(non_esexpr_ann);

	match dfn.definition.as_mut() {
		Definition::Record(r) => remove_record(r),
		Definition::Enum(e) => remove_enum(e),
		Definition::SimpleEnum(e) => remove_simple_enum(e),
		Definition::ExternType(_) => {},
		Definition::Interface(_) => {},
		Definition::ExceptionType(_) => {},
	}
}

fn remove_record(r: &mut RecordDefinition) {
	remove_fields(&mut r.fields);
}

fn remove_enum(e: &mut EnumDefinition) {
	for c in &mut e.cases {
		c.annotations.retain(non_esexpr_ann);
		remove_fields(&mut c.fields);
	}
}

fn remove_simple_enum(e: &mut SimpleEnumDefinition) {
	for c in &mut e.cases {
		c.annotations.retain(non_esexpr_ann);
	}
}

fn remove_fields(fields: &mut [Box<RecordField>]) {
	for field in fields {
		field.annotations.retain(non_esexpr_ann);
	}
}






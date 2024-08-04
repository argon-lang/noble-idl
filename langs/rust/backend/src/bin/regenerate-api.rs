use std::{collections::HashMap, path::PathBuf};

use noble_idl_compiler::{compile, NobleIDLOptions};
use noble_idl_compiler_rust::{CrateOptions, Crates, PackageMapping, RustLanguageOptions, RustPlugin};


fn main() {

	let mut dir = PathBuf::from(file!());
	dir.pop();

	let options = NobleIDLOptions {
		plugin_options: RustLanguageOptions {
			crate_name: "noble-idl-api".to_owned(),
			crates: Crates {
				crate_options: HashMap::from([
					("noble-idl-runtime".to_owned(), CrateOptions {
						package_mapping: PackageMapping {
							package_mapping: HashMap::from([
								("nobleidl.core".to_owned(), "".to_owned())
							]),
						},
					}),
					("noble-idl-api".to_owned(), CrateOptions {
						package_mapping: PackageMapping {
							package_mapping: HashMap::from([
								("nobleidl.compiler.api".to_owned(), "".to_owned())
							]),
						},
					}),
				]),
			},

			output_dir: dir.join("../../../../../noble-idl-api/src").into_os_string().into_string().unwrap(),
		},

		files: vec![
			dir.join("../../../../noble-idl/backend/compiler-api.nidl"),
		],

		library_files: vec![
			dir.join("../../../../noble-idl/runtime/nobleidl-core.nidl"),
		],
	};


	compile(&RustPlugin, &options).unwrap();

}


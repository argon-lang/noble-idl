use std::path::PathBuf;

use noble_idl_compiler::{compile, NobleIDLOptions};
use noble_idl_rust_plugin::RustPlugin;
use noble_idl_rust_plugin::cargo_util::load_options;


pub fn compile_from_build_script() {
    let rust_options = load_options();

    let options = NobleIDLOptions {
        library_files: rust_options.library_files.into_iter().map(PathBuf::from).collect(),
        files: rust_options.input_files.iter().map(PathBuf::from).collect(),
        plugin_options: rust_options.language_options,
    };

    compile(&RustPlugin, &options).unwrap();

	for file in rust_options.input_files {
		println!("cargo::rerun-if-changed={}", file);
	}
}

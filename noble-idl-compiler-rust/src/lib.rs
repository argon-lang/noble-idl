use std::path::PathBuf;

use noble_idl_compiler::{compile, NobleIDLOptions};
use noble_idl_rust_plugin::RustPlugin;
use noble_idl_rust_plugin::cargo_util::load_options;


pub fn compile_from_build_script() {

    let rust_options = load_options();

    println!("cargo::warning=Library files {}:", rust_options.library_files.len());
    for file in &rust_options.library_files {
        println!("cargo::warning={}", file);
    }

    println!("cargo::warning=Input files {}:", rust_options.input_files.len());
    for file in &rust_options.input_files {
        println!("cargo::warning={}", file);
    }

    println!("cargo::warning=Options {:?}", rust_options.language_options);
    
    let options = NobleIDLOptions {
        library_files: rust_options.library_files.into_iter().map(PathBuf::from).collect(),
        files: rust_options.input_files.into_iter().map(PathBuf::from).collect(),
        plugin_options: rust_options.language_options,
    };

    let result = compile(&RustPlugin, &options).unwrap();

    println!("cargo::warning=Generated {} files:", result.generated_files.len());
    for file in &result.generated_files {
        println!("cargo::warning={}", file);
    }

}

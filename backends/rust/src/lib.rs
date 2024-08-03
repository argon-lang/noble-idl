use std::path::PathBuf;

use noble_idl_compiler::{compile, NobleIDLOptions};
use cargo_util::load_options;

mod annotations;
mod cycle;
pub mod emit;
pub mod cargo_util;


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


use std::collections::HashMap;

use esexpr::ESExprCodec;
use noble_idl_api::NobleIDLPluginExecutor;


#[derive(ESExprCodec, Debug)]
pub struct RustIDLCompilerOptions {
    #[keyword]
    pub language_options: RustLanguageOptions,

    #[keyword]
    pub input_files: Vec<String>,

    #[keyword]
    pub library_files: Vec<String>,
}

#[derive(ESExprCodec, Debug, Clone)]
pub struct RustLanguageOptions {
    #[keyword]
    pub crate_name: String,

    #[keyword]
    pub crates: Crates,

    #[keyword]
    pub output_dir: String,
}


#[derive(ESExprCodec, Debug, Clone)]

pub struct Crates {
    #[dict]
    pub crate_options: HashMap<String, CrateOptions>,
}


#[derive(ESExprCodec, Debug, Clone)]
pub struct CrateOptions {
    #[keyword]
    pub package_mapping: PackageMapping,
}

#[derive(ESExprCodec, Debug, Clone)]
pub struct PackageMapping {
    #[dict]
    pub package_mapping: HashMap<String, String>,
}


pub struct RustPlugin;

impl NobleIDLPluginExecutor for RustPlugin {
    type LanguageOptions = RustLanguageOptions;
    type Error = emit::EmitError;

    fn generate(&self, request: noble_idl_api::NobleIDLGenerationRequest<Self::LanguageOptions>) -> Result<noble_idl_api::NobleIDLGenerationResult, Self::Error> {
        emit::emit(request)
    }
}






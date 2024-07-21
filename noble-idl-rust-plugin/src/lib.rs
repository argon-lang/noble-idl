use std::collections::HashMap;

use esexpr::ESExprCodec;
use noble_idl_api::NobleIDLPluginExecutor;

pub mod emit;
pub mod cargo_util;


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
    package_mapping: HashMap<String, String>,
}


pub struct RustPlugin;

impl NobleIDLPluginExecutor for RustPlugin {
    type LanguageOptions = RustLanguageOptions;
    type Error = emit::EmitError;

    fn generate(&self, model: noble_idl_api::NobleIDLDefinitions<Self::LanguageOptions>) -> Result<noble_idl_api::NobleIDLGenerationResult, Self::Error> {
        emit::emit(model)
    }
}




use std::{ffi::OsString, path::{Path, PathBuf}};


use derive_more::From;
use esexpr::ESExpr;

pub mod ast;
pub mod model;
pub mod parser;

use model::{CheckError, ModelBuilder};
use noble_idl_api::{NobleIdlCompileModelOptions, NobleIdlCompileModelResult, NobleIdlGenerationRequest, NobleIdlGenerationResult, NobleIdlModel, NobleIDLPluginExecutor};
use esexpr::ESExprCodec;


#[derive(From, Debug)]
pub enum Error<PE> {
    #[from(ignore)]
    ParseError(String),
    ModelCheckError(CheckError),
    IOError(std::io::Error),
    FormatError(std::fmt::Error),
    UnmappedPackageError(ast::PackageName),
    #[from(ignore)]
    PluginError(PE),
}

impl <'input, PE> From<nom::Err<nom::error::Error<&str>>> for Error<PE> {
    fn from(value: nom::Err<nom::error::Error<&str>>) -> Self {
        Error::ParseError(format!("{:?}", value))
    }
}

impl From<CompileModelError> for Error<CompileModelError> {
    fn from(value: CompileModelError) -> Self {
        Error::PluginError(value)
    }
}

pub struct ProcessPlugin {
    pub plugin_command: OsString,
    pub plugin_arguments: Vec<OsString>,
}

impl NobleIDLPluginExecutor for ProcessPlugin {
    type LanguageOptions = ESExpr;
    type Error = ProcessPluginError;

    fn generate(&self, request: NobleIdlGenerationRequest<Self::LanguageOptions>) -> Result<NobleIdlGenerationResult, Self::Error> {
        use std::process::{Command, Stdio};

        let mut child = Command::new(&self.plugin_command)
            .args(&self.plugin_arguments)
            .stdin(Stdio::piped())
            .stdout(Stdio::piped())
            .spawn()?;

        let mut stdin: std::process::ChildStdin = child.stdin.take().unwrap();

        let model = request.encode_esexpr();

        esexpr_binary::generate_single(&mut stdin, &model)?;

        drop(stdin);



        let stdout = child.stdout.take().unwrap();

        let mut results = esexpr_binary::parse(stdout)
            .map(|res| -> Result<_, ProcessPluginError> {
                let res = res?;
                let res = NobleIdlGenerationResult::decode_esexpr(res)?;
                Ok(res)
            })
            .collect::<Result<Vec<_>, _>>()?;

        let num_res = results.len();

        match results.pop() {
            Some(res) if num_res == 1 => Ok(res),
            _ => Err(ProcessPluginError::UnexpectedGenerationResult {
                expected_count: 1,
                actual_count: num_res,
            }),
        }
    }
}

#[derive(From, Debug)]
pub enum ProcessPluginError {
    ESExprParseError(esexpr_binary::ParseError),
    ESExprGeneratorError(esexpr_binary::GeneratorError),
    ESExprDecodeError(esexpr::DecodeError),
    IOError(std::io::Error),
    UnexpectedGenerationResult {
        expected_count: usize,
        actual_count: usize,
    },
}


#[derive(Debug)]
pub struct NobleIDLOptions<L> {
    pub library_files: Vec<PathBuf>,
    pub files: Vec<PathBuf>,
    pub plugin_options: L,
}


pub fn compile<P: NobleIDLPluginExecutor>(p: &P, options: &NobleIDLOptions<P::LanguageOptions>) -> Result<NobleIdlGenerationResult, Error<P::Error>> {
    let mut model = ModelBuilder::new();
    for file in &options.library_files {
        load_file(&mut model, file, true)?;
    }

    for file in &options.files {
        load_file(&mut model, file, false)?;
    }

    let model = model.check()?;
    let request = NobleIdlGenerationRequest {
        language_options: options.plugin_options.clone(),
        model: Box::new(model),
    };

    p.generate(request).map_err(Error::PluginError)
}

fn load_file<PE>(model: &mut model::ModelBuilder, file: &Path, is_library: bool) -> Result<(), Error<PE>> {

    let source = std::fs::read_to_string(file)?;
    let (_, def_file) = parser::definition_file(&source)?;

    for def in def_file.definitions {
        model.add_definition(model::DefinitionInfo {
            package: def_file.package.clone(),
            imports: def_file.imports.clone(),
            def,
            is_library,
        })?;
    }

    Ok(())
}

#[derive(From, Debug)]
pub enum CompileModelError {
    ParseError(esexpr_binary::ParseError),
    OptionsDecodeError(esexpr::DecodeError),
    ModuleNotSpecified,
    ExtraModulesFound,
}


#[no_mangle]
pub unsafe extern "C" fn nobleidl_alloc(size: usize) -> *mut u8 {
	std::alloc::alloc(std::alloc::Layout::array::<u8>(size).unwrap())
}

#[no_mangle]
pub unsafe extern "C" fn nobleidl_free(ptr: *mut u8, size: usize) {
    std::alloc::dealloc(ptr, std::alloc::Layout::array::<u8>(size).unwrap());
}

#[no_mangle]
pub unsafe extern "C" fn nobleidl_compile_model(options: *mut u8, options_size: usize, result_size: *mut usize) -> *mut u8 {
    let options = std::slice::from_raw_parts(options, options_size);

    let model = compile_model_serialized(options);

    let buff = nobleidl_alloc(model.len());

	if buff.is_null() {
		return buff;
	}

    std::ptr::copy(model.as_ptr(), buff, model.len());

	result_size.write_unaligned(model.len());

    buff
}


fn compile_model_serialized(options: &[u8]) -> Vec<u8> {
    serialize_result(match compile_model_options_ser(options) {
        Ok(model) => NobleIdlCompileModelResult::Success(Box::new(model)),
        Err(e) => NobleIdlCompileModelResult::Failure {
            errors: vec![ format!("{:?}", e) ],
        },
    })
}

fn serialize_result(result: NobleIdlCompileModelResult) -> Vec<u8> {
    let result = result.encode_esexpr();
    let mut buff = Vec::new();

	esexpr_binary::generate_single(&mut buff, &result).unwrap();

    buff
}


fn compile_model_options_ser(options: &[u8]) -> Result<NobleIdlModel, Error<CompileModelError>> {
    let mut options_vec = esexpr_binary::parse(options)
        .collect::<Result<Vec<_>, _>>()
        .map_err(CompileModelError::ParseError)?;

    let Some(options) = options_vec.pop() else {
        Err(CompileModelError::ModuleNotSpecified)?
    };

    if !options_vec.is_empty() {
        Err(CompileModelError::ExtraModulesFound)?
    }

    let options = NobleIdlCompileModelOptions::decode_esexpr(options)
        .map_err(CompileModelError::OptionsDecodeError)?;


    compile_model(options)
}


pub fn compile_model(options: NobleIdlCompileModelOptions) -> Result<NobleIdlModel, Error<CompileModelError>> {
    let mut model = ModelBuilder::new();
    for file in &options.library_files {
        load_source(&mut model, file, true)?;
    }

    for file in &options.files {
        load_source(&mut model, file, false)?;
    }

    Ok(model.check()?)
}

fn load_source<PE>(model: &mut model::ModelBuilder, source: &String, is_library: bool) -> Result<(), Error<PE>> {

    let (_, def_file) = parser::definition_file(&source)?;

    for def in def_file.definitions {
        model.add_definition(model::DefinitionInfo {
            package: def_file.package.clone(),
            imports: def_file.imports.clone(),
            def,
            is_library,
        })?;
    }

    Ok(())
}




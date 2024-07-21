use std::{ffi::OsString, path::{Path, PathBuf}};


use derive_more::From;
use esexpr::ESExpr;
use lalrpop_util::{lalrpop_mod, lexer::Token, ParseError};

pub mod ast;
pub mod model;

use model::{CheckError, ModelBuilder};
use noble_idl_api::{NobleIDLDefinitions, NobleIDLGenerationResult, NobleIDLPluginExecutor};
use esexpr::ESExprCodec;

lalrpop_mod!(grammar);


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

impl <'input, PE> From<ParseError<usize, Token<'input>, &str>> for Error<PE> {
    fn from(value: ParseError<usize, Token<'input>, &str>) -> Self {
        Error::ParseError(format!("{:?}", value))
    }
}

pub struct ProcessPlugin {
    pub plugin_command: OsString,
    pub plugin_arguments: Vec<OsString>,
}

impl NobleIDLPluginExecutor for ProcessPlugin {
    type LanguageOptions = ESExpr;
    type Error = ProcessPluginError;

    fn generate(&self, model: NobleIDLDefinitions<Self::LanguageOptions>) -> Result<NobleIDLGenerationResult, Self::Error> {
        use std::process::{Command, Stdio};
        use esexpr_binary::FixedStringPool;
    
        let mut child = Command::new(&self.plugin_command)
            .args(&self.plugin_arguments)
            .stdin(Stdio::piped())
            .stdout(Stdio::piped())
            .spawn()?;
    
        let mut stdin = child.stdin.take().unwrap();
    
        let model = model.encode_esexpr();
        
        
        let mut sp = esexpr_binary::StringPoolBuilder::new();
        sp.add(&model);
        let mut sp = sp.into_fixed_string_pool();
    
        esexpr_binary::generate(&mut stdin, &mut FixedStringPool { strings: vec!() }, &sp.clone().encode_esexpr())?;
        esexpr_binary::generate(&mut stdin, &mut sp, &model)?;
    
        drop(stdin);
    
    
    
        let stdout = child.stdout.take().unwrap();
    
        let mut results = esexpr_binary::parse_embedded_string_pool(stdout)?
            .map(|res| -> Result<_, ProcessPluginError> {
                let res = res?;
                let res = NobleIDLGenerationResult::decode_esexpr(res)?;
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


pub fn compile<P: NobleIDLPluginExecutor>(p: &P, options: &NobleIDLOptions<P::LanguageOptions>) -> Result<NobleIDLGenerationResult, Error<P::Error>> {
    let mut model = ModelBuilder::new();
    for file in &options.library_files {
        load_file(&mut model, file, true)?;
    }

    for file in &options.files {
        load_file(&mut model, file, false)?;
    }

    let model = model.check(options.plugin_options.clone())?;
    p.generate(model).map_err(Error::PluginError)
}

fn load_file<PE>(model: &mut model::ModelBuilder, file: &Path, is_library: bool) -> Result<(), Error<PE>> {

    let source = std::fs::read_to_string(file)?;
    let parser = grammar::DefinitionFileParser::new();
    let def_file = parser.parse(&source)?;

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



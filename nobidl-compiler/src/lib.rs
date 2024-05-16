use std::path::{Path, PathBuf};


use derive_more::From;
use lalrpop_util::{lalrpop_mod, lexer::Token, ParseError};

pub mod ast;
pub mod model;
pub mod emit;

use model::{CheckError, ModelBuilder};

lalrpop_mod!(grammar);


#[derive(From, Debug)]
pub enum Error {
    #[from(ignore)]
    ParseError(String),
    ModelCheckError(CheckError),
    IOError(std::io::Error),
    FormatError(std::fmt::Error),
    UnmappedPackageError(ast::PackageName),
}

impl <'input> From<ParseError<usize, Token<'input>, &str>> for Error {
    fn from(value: ParseError<usize, Token<'input>, &str>) -> Self {
        Error::ParseError(format!("{:?}", value))
    }
}


#[derive(Debug)]
pub struct NobIDLOptions {
    pub library_files: Vec<PathBuf>,
    pub files: Vec<PathBuf>,
    pub emit: Vec<emit::EmitOptions>,
}


pub fn compile(options: &NobIDLOptions) -> Result<(), Error> {
    let mut model = ModelBuilder::new();
    for file in &options.library_files {
        load_file(&mut model, file, true)?;
    }

    for file in &options.files {
        load_file(&mut model, file, false)?;
    }

    let model = model.check()?;

    for emit_options in &options.emit {
        emit::emit(&model, emit_options)?;
    }

    Ok(())
}

fn load_file(model: &mut model::ModelBuilder, file: &Path, is_library: bool) -> Result<(), Error> {

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



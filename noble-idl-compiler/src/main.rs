use std::{ffi::OsString, path::{Path, PathBuf}};

use clap::Parser;
use esexpr::ESExpr;
use noble_idl_compiler::{compile, NobleIDLOptions, ProcessPlugin};

#[derive(Debug, Parser)]
pub struct CommandLineOptions {
    #[arg(short = 'L', long = "lib")]
    pub library_files: Vec<PathBuf>,

    #[arg(short = 'i', long = "input")]
    pub files: Vec<PathBuf>,

    #[arg(short = 'P', long = "plugin-command")]
    pub plugin_command: OsString,

    #[arg(short = 'A', long = "plugin-arguments")]
    pub plugin_arguments: Vec<OsString>,

    #[arg(short = 'c', long = "plugin-options")]
    pub plugin_options: PathBuf,
}


fn main() {
    let args = CommandLineOptions::parse();

    let plugin_options = read_plugin_options(&args.plugin_options);

    let proc_plugin = ProcessPlugin {
        plugin_command: args.plugin_command,
        plugin_arguments: args.plugin_arguments,
    };

    let options = NobleIDLOptions {
        library_files: args.library_files,
        files: args.files,
        plugin_options,
    };

    let result = compile(&proc_plugin, &options).expect("Error compiling");

    println!("Generated {} files:", result.generated_files.len());
    for file in &result.generated_files {
        println!("{}", file);
    }
}

fn read_plugin_options(path: &Path) -> ESExpr {
    let s = std::fs::read_to_string(path).expect("Could not read plugin options.");
    esexpr_text::parse(&s).ok().expect("Could not parse plugin options.")
}

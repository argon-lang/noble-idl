use std::collections::HashMap;
use std::{fs::File, path::Path};
use std::io::BufWriter;
use std::fmt::Write;


use io_adapters::{FmtToIo, WriteExtension};

use crate::ast::*;
use crate::model::{DefinitionInfo, Model};
use crate::emit::{for_sep, IndentWriter, name_to_pascal_case};

pub(super) trait ScalaCommonEmitOptions {
    type Extras;

    fn make_extras(&self) -> Self::Extras;
    fn output_file(&self) -> &Path;
    fn package_mapping(&self) -> &HashMap<String, String>;
}

pub(super) fn emit<Options: ScalaCommonEmitOptions>(model: &Model, options: &Options) -> Result<(), crate::Error>
    where for<'a, 'b, 'c> Emitter<'a, 'a, FmtToIo<&'b mut BufWriter<&'c mut File>>, Options>: ScalaEmitter
{
    let mut f = std::fs::File::create(&options.output_file())?;
    let mut writer = BufWriter::new(&mut f);
    let mut fmt_writer = writer.write_adapter();
    let mut indent_writer = IndentWriter::new(&mut fmt_writer, "  ");
    let mut emitter = Emitter::new(&mut indent_writer, options);

    let use_single = model.definitions.len() == 1;
    for (package, defs) in &model.definitions {
        if use_single {
            emitter.emit_package_single(package, defs)?;
        }
        else {
            emitter.emit_package_block(package, defs)?;
        }
    }

    Ok(())
}

const KEYWORDS: &[&str] = &[
    "abstract",
    "case",
    "catch",
    "class",
    "def",
    "do",
    "else",
    "enum",
    "export",
    "extends",
    "false",
    "final",
    "finally",
    "for",
    "given",
    "if",
    "implicit",
    "import",
    "lazy",
    "match",
    "new",
    "null",
    "object",
    "override",
    "package",
    "private",
    "protected",
    "return",
    "sealed",
    "super",
    "then",
    "throw",
    "trait",
    "true",
    "try",
    "type",
    "val",
    "var",
    "while",
    "with",
    "yield",
];


pub(super) struct Emitter<'a, 'b, W, Options: ScalaCommonEmitOptions> {
    pub f: &'a mut IndentWriter<'b, W>,
    options: &'a Options,
    current_type_parameters: Vec<String>,
    pub extras: Options::Extras,
}

pub(super) trait ScalaEmitter {
    fn emit_record(&mut self, package: &PackageName, rec: &RecordDefinition) -> Result<(), crate::Error>;
    fn emit_enum(&mut self, package: &PackageName, e: &EnumDefinition) -> Result<(), crate::Error>;
    fn emit_interface(&mut self, package: &PackageName, iface: &InterfaceDefinition) -> Result<(), crate::Error>;

    fn convert_identifier(&self, name: &str) -> String;
}

impl <'a, 'b, W: Write, Options: ScalaCommonEmitOptions> Emitter<'a, 'b, W, Options> where Emitter<'a, 'b, W, Options>: ScalaEmitter {

    pub(super) fn new(writer: &'a mut IndentWriter<'b, W>, options: &'a Options) -> Self {
        let extras = options.make_extras();
        Emitter {
            f: writer,
            options,
            current_type_parameters: Vec::new(),
            extras,            
        }
    }

    pub(super) fn options(&self) -> &Options {
        return self.options;
    }

    pub(super) fn is_type_parameter(&self, name: &str) -> bool {
        self.current_type_parameters.iter().any(|s| s == name)
    }

    fn emit_package_single(&mut self, package: &PackageName, defs: &[DefinitionInfo]) -> Result<(), crate::Error> {
        write!(self.f, "package ")?;
        self.write_package_name(package)?;
        writeln!(self.f)?;

        for def in defs {
            self.emit_definition(def)?;
        }

        Ok(())
    }

    fn emit_package_block(&mut self, package: &PackageName, defs: &[DefinitionInfo]) -> Result<(), crate::Error> {
        write!(self.f, "package ")?;
        self.write_package_name(package)?;
        writeln!(self.f, " {{")?;
        self.f.indent();

        for def in defs {
            self.emit_definition(def)?;
        }

        self.f.dedent();
        writeln!(self.f, "}}")?;

        Ok(())
    }
    
    fn emit_definition(&mut self, def: &DefinitionInfo) -> Result<(), crate::Error> {
        match &def.def {
            Definition::Record(rec) => self.emit_record(&def.package, rec),
            Definition::Enum(e) => self.emit_enum(&def.package, e),
            Definition::ExternType(_) => Ok(()),
            Definition::Interface(iface) => self.emit_interface(&def.package, iface),
        }
    }


    pub(super) fn push_type_parameters(&mut self, params: &[TypeParameter]) -> TypeParameterState {
        let old_type_parameters = self.current_type_parameters.clone();
        self.current_type_parameters.extend(params.iter().map(|p| p.name().to_owned()));
        TypeParameterState {
            old_type_parameters,
        }
    }

    pub(super) fn pop_type_parameters(&mut self, prev: TypeParameterState) {
        self.current_type_parameters = prev.old_type_parameters;
    }


    pub(super) fn write_package_name(&mut self, package: &PackageName) -> Result<(), crate::Error> {
        let idl_pkg = package.0.join(".");
        let pkg = self.options.package_mapping().get(&idl_pkg).ok_or_else(|| crate::Error::UnmappedPackageError(package.clone()))?;
        write!(self.f, "{}", pkg)?;
        Ok(())
    }
    
    pub(super) fn write_qualified_name(&mut self, name: &QualifiedName) -> Result<(), crate::Error> {
        self.write_qualified_name_param_suffix(name, "")
    }
    
    pub(super) fn write_qualified_name_param_suffix(&mut self, name: &QualifiedName, suffix: &str) -> Result<(), crate::Error> {
        if name.0.0.is_empty() && self.is_type_parameter(&name.1) {
            self.write_type_identifier(&name.1)?;
            write!(self.f, "{}", suffix)?;
            return Ok(());
        }

        write!(self.f, "_root_.")?;

        if !name.0.0.is_empty() {
            self.write_package_name(&name.0)?;
            write!(self.f, ".")?;
        }

        self.write_type_identifier(&name.1)
    }

    pub(super) fn write_identifier(&mut self, name: &str) -> Result<(), crate::Error> {
        let name = self.convert_identifier(name);
        if KEYWORDS.contains(&name.as_str()) {
            write!(self.f, "`{}`", name)?;
        }
        else {
            write!(self.f, "{}", name)?;
        }

        Ok(())
    }

    pub(super) fn write_type_identifier(&mut self, name: &str) -> Result<(), crate::Error> {
        write!(self.f, "{}", name_to_pascal_case(name))?;

        Ok(())
    }

    pub(super) fn write_type_parameters(&mut self, params: &[TypeParameter]) -> Result<(), crate::Error> {
        if !params.is_empty() {
            write!(self.f, "[")?;
            for_sep!(param, params, { write!(self.f, ", ")?; }, {
                match param {
                    TypeParameter::Type(name) => self.write_type_identifier(name)?,
                }
            });
            write!(self.f, "]")?;
        }

        Ok(())
    }

    pub(super) fn write_type_parameters_12(&mut self, params: &[TypeParameter]) -> Result<(), crate::Error> {
        if !params.is_empty() {
            write!(self.f, "[")?;
            for_sep!(param, params, { write!(self.f, ", ")?; }, {
                match param {
                    TypeParameter::Type(name) => {
                        self.write_type_identifier(name)?;
                        write!(self.f, "1, ")?;
                        self.write_type_identifier(name)?;
                        write!(self.f, "2")?;
                    },
                }
            });
            write!(self.f, "]")?;
        }

        Ok(())
    }

    pub(super) fn write_type_arguments(&mut self, params: &[TypeParameter]) -> Result<(), crate::Error> {
        self.write_type_arguments_suffix(params, "")
    }

    pub(super) fn write_type_arguments_suffix(&mut self, params: &[TypeParameter], suffix: &str) -> Result<(), crate::Error> {
        if !params.is_empty() {
            write!(self.f, "[")?;
            for_sep!(param, params, { write!(self.f, ", ")?; }, {
                self.write_type_identifier(param.name())?;
                write!(self.f, "{}", suffix)?;
            });
            write!(self.f, "]")?;
        }

        Ok(())
    }
    
    pub(super) fn write_type(&mut self, t: &TypeExpr) -> Result<(), crate::Error> {
        self.write_type_param_suffix(t, "")
    }
    
    pub(super) fn write_type_param_suffix(&mut self, t: &TypeExpr, suffix: &str) -> Result<(), crate::Error> {
        match t {
            TypeExpr::Name(name) => self.write_qualified_name_param_suffix(name, suffix)?,
            TypeExpr::Apply(f, args) => {
                self.write_type_param_suffix(f, suffix)?;
                write!(self.f, "[")?;
                for_sep!(arg, args, { write!(self.f, ", ")?; }, {
                    self.write_type_param_suffix(arg, suffix)?;
                });
                write!(self.f, "]")?;
            },
        }

        Ok(())
    }

}

pub(super) struct TypeParameterState {
    old_type_parameters: Vec<String>,
}

use std::path::PathBuf;
use std::io::BufWriter;
use std::fmt::Write;
use std::collections::HashMap;


use io_adapters::WriteExtension;

use crate::ast::*;
use super::for_sep;
use crate::model::{DefinitionInfo, Model};


#[derive(Debug)]
pub struct JavaEmitOptions {
    pub package_mapping: HashMap<String, String>,
    pub output_dir: PathBuf,

    pub extern_type_mapping: HashMap<String, String>,
}

pub(crate) fn emit(model: &Model, options: &JavaEmitOptions) -> Result<(), crate::Error> {
    for (package, defs) in &model.definitions {
        for def in defs {
            let mut path = options.output_dir.clone();
            for part in get_java_package(package, options)?.split(".") {
                path.push(part);
            }
            std::fs::create_dir_all(&path)?;

            path.push(super::name_to_pascal_case(def.def.name()) + ".java");
    
            let mut f = std::fs::File::create(path)?;
            let mut writer = BufWriter::new(&mut f);
            let mut fmt_witer = writer.write_adapter();
            let mut emitter = Emitter {
                f: super::IndentWriter::new(&mut fmt_witer, "\t"),
                options,
            };
    
            emitter.emit_package(def)?;
        }
    }

    Ok(())
}

const KEYWORDS: &[&str] = &[
    "_",
    "abstract",
    "assert",
    "boolean",
    "break",
    "byte",
    "case",
    "catch",
    "char",
    "class",
    "continue",
    "default",
    "do",
    "double",
    "else",
    "enum",
    "extends",
    "final",
    "finally",
    "for",
    "if",
    "implements",
    "import",
    "instanceof",
    "int",
    "interface",
    "long",
    "native",
    "new",
    "package",
    "private",
    "protected",
    "public",
    "return",
    "short",
    "static",
    "super",
    "switch",
    "synchronized",
    "this",
    "throw",
    "throws",
    "transient",
    "try",
    "void",
    "volatile",
    "while",
];

const PRIMITIVE_TYPES: &[(&str, &str)] = &[
    ("byte", "java.lang.Byte"),
    ("short", "java.lang.Short"),
    ("int", "java.lang.Integer"),
    ("long", "java.lang.Long"),
    ("float", "java.lang.Float"),
    ("double", "java.lang.Double"),
    ("char", "java.lang.Character"),
    ("boolean", "java.lang.Boolean"),
    ("void", "java.lang.Object"),
];


struct Emitter<'a, W> {
    f: super::IndentWriter<'a, W>,
    options: &'a JavaEmitOptions,
}

impl <'a, W: Write> Emitter<'a, W> {
    fn emit_package(&mut self, def: &DefinitionInfo) -> Result<(), crate::Error> {
        write!(self.f, "package ")?;
        self.write_package_name(&def.package)?;
        writeln!(self.f, ";")?;

        match &def.def {
            Definition::Record(rec) => self.emit_record(rec)?,
            Definition::Enum(e) => self.emit_enum(e)?,
            Definition::ExternType(_) => (),
            Definition::Interface(iface) => self.emit_interface(iface)?,
        }

        Ok(())
    }

    fn emit_record(&mut self, rec: &RecordDefinition) -> Result<(), crate::Error> {
        write!(self.f, "public record ")?;
        self.write_type_identifier(&rec.name)?;
        self.write_type_parameters(&rec.type_parameters)?;
        writeln!(self.f, "(")?;
        self.f.indent();

        for (i, field) in rec.fields.iter().enumerate() {
            self.write_type(&field.field_type, TypeOptions { is_boxed: false, allow_void: false })?;
            write!(self.f, " ")?;
            self.write_identifier(&field.name)?;
            
            if i < rec.fields.len() - 1 {
                writeln!(self.f, ",")?;
            }
            else {
                writeln!(self.f)?;
            }
        }

        self.f.dedent();
        writeln!(self.f, ") {{}}")?;

        Ok(())
    }

    fn emit_enum(&mut self, e: &EnumDefinition) -> Result<(), crate::Error> {
        if e.type_parameters.is_empty() && e.cases.iter().all(|c| c.fields.is_empty()) {
            write!(self.f, "public enum ")?;
            self.write_type_identifier(&e.name)?;
            writeln!(self.f, " {{")?;
            self.f.indent();
    
            for c in &e.cases {
                writeln!(self.f, "{},", c.name)?;
            }
    
            self.f.dedent();
            writeln!(self.f, "}}")?;
        }
        else {
            write!(self.f, "public sealed interface ")?;
            self.write_type_identifier(&e.name)?;
            self.write_type_parameters(&e.type_parameters)?;
            writeln!(self.f, " {{")?;
            self.f.indent();
    
            for c in &e.cases {
                write!(self.f, "public record ")?;
                self.write_type_identifier(&c.name)?;
                self.write_type_parameters(&e.type_parameters)?;
                writeln!(self.f, "(")?;
                self.f.indent();
        
                for (i, field) in c.fields.iter().enumerate() {
                    self.write_type(&field.field_type, TypeOptions { is_boxed: false, allow_void: false })?;
                    write!(self.f, " ")?;
                    self.write_identifier(&field.name)?;
                    
                    if i < c.fields.len() - 1 {
                        writeln!(self.f, ",")?;
                    }
                    else {
                        writeln!(self.f)?;
                    }
                }
        
                self.f.dedent();
                write!(self.f, ") implements ")?;
                self.write_type_identifier(&e.name)?;
                if !e.type_parameters.is_empty() {
                    write!(self.f, "<")?;
                    for_sep!(param, &e.type_parameters, { write!(self.f, ", ")?; }, {
                        self.write_type_identifier(param.name())?;
                    });
                    write!(self.f, ">")?;
                }
                writeln!(self.f, " {{}}")?;
            }
    
            self.f.dedent();
            writeln!(self.f, "}}")?;
        }


        Ok(())
    }
    
    fn emit_interface(&mut self, iface: &InterfaceDefinition) -> Result<(), crate::Error> {
        write!(self.f, "public interface ")?;
        self.write_type_identifier(&iface.name)?;
        self.write_type_parameters(&iface.type_parameters)?;
        writeln!(self.f, " {{")?;
        self.f.indent();

        for method in &iface.methods {
            self.write_type(&method.return_type, TypeOptions { is_boxed: false, allow_void: true })?;
            write!(self.f, " ")?;
            self.write_identifier(&method.name)?;
            self.write_type_parameters(&method.type_parameters)?;
            writeln!(self.f, "(")?;

            for_sep!(param, &method.parameters, { write!(self.f, ", ")?; }, {
                self.write_type(&param.parameter_type, TypeOptions { is_boxed: false, allow_void: false })?;
                write!(self.f, " ")?;
                self.write_identifier(&param.name)?;
            });

            write!(self.f, ") throws java.lang.Throwable;")?;
        }

        self.f.dedent();
        writeln!(self.f, "}}")?;

        Ok(())
    }



    fn write_package_name(&mut self, package: &PackageName) -> Result<(), crate::Error> {
        let pkg = get_java_package(package, &self.options)?;
        write!(self.f, "{}", pkg)?;
        Ok(())
    }

    fn write_identifier(&mut self, name: &str) -> Result<(), crate::Error> {
        let name = super::name_to_camel_case(name);
        if KEYWORDS.contains(&name.as_str()) {
            write!(self.f, "`{}`", name)?;
        }
        else {
            write!(self.f, "{}", name)?;
        }

        Ok(())
    }

    fn write_type_identifier(&mut self, name: &str) -> Result<(), crate::Error> {
        write!(self.f, "{}", super::name_to_pascal_case(name))?;

        Ok(())
    }

    fn write_qualified_name(&mut self, name: &QualifiedName, options: TypeOptions) -> Result<(), crate::Error> {

        let name_str = format!("{}.{}", name.0.0.join("."), &name.1);
        if let Some(mapped_type) = self.options.extern_type_mapping.get(&name_str) {
            if mapped_type == "void" && !options.allow_void {
                write!(self.f, "java.lang.Object")?;
                return Ok(());
            }
            else if options.is_boxed {
                if let Some(mapped_type) = PRIMITIVE_TYPES.iter().find(|(pt, _)| pt == mapped_type).map(|(_, boxed)| boxed) {
                    write!(self.f, "{}", mapped_type)?;
                    return Ok(());
                }
            }

            write!(self.f, "{}", mapped_type)?;
            return Ok(());
        }
        else {
            if !name.0.0.is_empty() {
                self.write_package_name(&name.0)?;
                write!(self.f, ".")?;
            }
    
            self.write_type_identifier(&name.1)    
        }
    }

    fn write_type_parameters(&mut self, params: &[TypeParameter]) -> Result<(), crate::Error> {
        if !params.is_empty() {
            write!(self.f, "<")?;
            for_sep!(param, params, { write!(self.f, ", ")?; }, {
                match param {
                    TypeParameter::Type(name) => self.write_type_identifier(name)?,
                }
            });
            write!(self.f, ">")?;
        }

        Ok(())
    }
    
    fn write_type(&mut self, t: &TypeExpr, options: TypeOptions) -> Result<(), crate::Error> {
        match t {
            TypeExpr::Name(name) => self.write_qualified_name(name, options)?,
            TypeExpr::Apply(f, args) => {
                self.write_type(f, options)?;
                write!(self.f, "<")?;
                for_sep!(arg, args, { write!(self.f, ", ")?; }, {
                    self.write_type(arg, TypeOptions { is_boxed: true, ..options })?;
                });
                write!(self.f, ">")?;
            },
        }

        Ok(())
    }

}


fn get_java_package<'a>(package: &PackageName, options: &'a JavaEmitOptions) -> Result<&'a str, crate::Error> {
    let idl_pkg = package.0.join(".");
    options.package_mapping
        .get(&idl_pkg)
        .map(|s| s.as_str())
        .ok_or_else(|| crate::Error::UnmappedPackageError(package.clone()))
}

#[derive(Debug, Clone, Copy)]
struct TypeOptions {
    is_boxed: bool,
    allow_void: bool,
}



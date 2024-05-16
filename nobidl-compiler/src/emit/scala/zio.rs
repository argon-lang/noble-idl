use std::{fmt::Write, path::PathBuf};
use std::collections::HashMap;


use crate::ast::*;
use crate::emit::{for_sep, name_to_camel_case};
use crate::model::Model;

use super::common::{self, *};



#[derive(Debug)]
pub struct ScalaEmitOptions {
    pub package_mapping: HashMap<String, String>,
    pub output_file: PathBuf,
}

impl ScalaCommonEmitOptions for ScalaEmitOptions {
    type Extras = ();
    
    fn make_extras(&self) -> Self::Extras {
    }

    fn output_file(&self) -> &std::path::Path {
        &self.output_file
    }

    fn package_mapping(&self) -> &HashMap<String, String> {
        &self.package_mapping
    }
}

pub(in crate::emit) fn emit_zio(model: &Model, options: &ScalaEmitOptions) -> Result<(), crate::Error> {
    common::emit(model, options)
}

impl <'a, 'b, W: Write> ScalaEmitter for Emitter<'a, 'b, W, ScalaEmitOptions> {
    fn emit_record(&mut self, package: &PackageName, rec: &RecordDefinition) -> Result<(), crate::Error> {
        let old_type_params = self.push_type_parameters(&rec.type_parameters);

        write!(self.f, "final case class ")?;
        self.write_type_identifier(&rec.name)?;
        self.write_type_parameters(&rec.type_parameters)?;
        writeln!(self.f, "(")?;
        self.f.indent();

        for field in &rec.fields {
            self.write_identifier(&field.name)?;
            write!(self.f, ": ")?;
            self.write_type(&field.field_type)?;
            writeln!(self.f, ",")?;
        }

        self.f.dedent();
        writeln!(self.f, ")")?;

        self.pop_type_parameters(old_type_params);

        Ok(())
    }

    fn emit_enum(&mut self, package: &PackageName, e: &EnumDefinition) -> Result<(), crate::Error> {
        let old_type_params = self.push_type_parameters(&e.type_parameters);

        write!(self.f, "enum ")?;
        self.write_type_identifier(&e.name)?;
        self.write_type_parameters(&e.type_parameters)?;
        writeln!(self.f, " {{")?;
        self.f.indent();

        for c in &e.cases {
            write!(self.f, "case ")?;
            self.write_type_identifier(&c.name)?;
            writeln!(self.f, "(")?;
            self.f.indent();

            for field in &c.fields {
                self.write_identifier(&field.name)?;
                write!(self.f, ": ")?;
                self.write_type(&field.field_type)?;
                writeln!(self.f, ",")?;
            }

            self.f.dedent();
            writeln!(self.f, ")")?;
        }

        self.f.dedent();
        writeln!(self.f, "}}")?;

        self.pop_type_parameters(old_type_params);

        Ok(())
    }
    
    fn emit_interface(&mut self, package: &PackageName, iface: &InterfaceDefinition) -> Result<(), crate::Error> {
        let old_type_params = self.push_type_parameters(&iface.type_parameters);

        write!(self.f, "trait ")?;
        self.write_type_identifier(&iface.name)?;
        self.write_type_parameters(&iface.type_parameters)?;
        writeln!(self.f, " {{")?;
        self.f.indent();

        for method in &iface.methods {
            let old_type_params = self.push_type_parameters(&method.type_parameters);

            write!(self.f, "def ")?;
            self.write_identifier(&method.name)?;
            self.write_type_parameters(&method.type_parameters)?;
            write!(self.f, "(")?;

            for_sep!(param, &method.parameters, { write!(self.f, ", ")?; }, {
                self.write_identifier(&param.name)?;
                write!(self.f, ": ")?;
                self.write_type(&param.parameter_type)?;
            });

            write!(self.f, "): _root_.zio.Task[")?;
            
            self.write_type(&method.return_type)?;
            writeln!(self.f, "]")?;

            self.pop_type_parameters(old_type_params);
        }

        self.f.dedent();
        writeln!(self.f, "}}")?;

        self.pop_type_parameters(old_type_params);

        Ok(())
    }
    
    fn convert_identifier(&self, name: &str) -> String {
        name_to_camel_case(name)
    }

    
}

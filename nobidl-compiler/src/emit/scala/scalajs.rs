use std::{fmt::Write, path::PathBuf};
use std::collections::HashMap;


use crate::ast::*;
use crate::emit::{for_sep, name_to_camel_case};
use crate::model::Model;

use super::common::{self, *};
use super::ScalaEmitOptions;

const TYPE_ADAPTER_TYPE: &str = "_root_.dev.argon.nobidl.sjs.core.JSTypeAdapter";

#[derive(Debug)]
pub struct ScalaJSEmitOptions {
    pub package_mapping: HashMap<String, String>,
    pub output_file: PathBuf,

    pub scala_zio_package_mapping: Option<HashMap<String, String>>,
}

impl ScalaCommonEmitOptions for ScalaJSEmitOptions {
    type Extras = ScalaJSExtras;
    
    fn make_extras(&self) -> Self::Extras {
        ScalaJSExtras {
            scala_zio_options: super::zio::ScalaEmitOptions {
                package_mapping: self.scala_zio_package_mapping.clone().unwrap_or_default(),
                output_file: self.output_file.clone(),
            },
        }
    }

    fn output_file(&self) -> &std::path::Path {
        &self.output_file
    }

    fn package_mapping(&self) -> &HashMap<String, String> {
        &self.package_mapping
    }
}


pub(super) struct ScalaJSExtras {
    scala_zio_options: super::zio::ScalaEmitOptions,
}




macro_rules! write_type_adapter {
    ($emitter: expr, $current_name: expr, $type_parameters: expr, $to_js: block, $from_js: block) => {


        write!($emitter.f, "def jsTypeAdapter")?;
        if !$type_parameters.is_empty() {
            $emitter.write_type_parameters_12($type_parameters)?;
            write!($emitter.f, "(")?;
            for_sep!(param, $type_parameters, { write!($emitter.f, ", ")?; }, {
                let param_name = param.name();
                write!($emitter.f, "{}JSTypeAdapter: {}[", $emitter.convert_identifier(param_name), TYPE_ADAPTER_TYPE)?;
                $emitter.write_type_identifier(param_name)?;
                write!($emitter.f, "1, ")?;
                $emitter.write_type_identifier(param_name)?;
                write!($emitter.f, "2]")?;
            });
            write!($emitter.f, ")")?;
        }

        write!($emitter.f, ": ")?;
        write_type_adapter_type($emitter, $current_name, $type_parameters)?;
        write!($emitter.f, " = new ")?;
        write_type_adapter_type($emitter, $current_name, $type_parameters)?;
        writeln!($emitter.f, " {{")?;
        $emitter.f.indent();

        write!($emitter.f, "override def toJS(a_ : ")?;
        zio_emitter($emitter).write_qualified_name($current_name)?;
        $emitter.write_type_arguments_suffix($type_parameters, "1")?;
        write!($emitter.f, "): ")?;
        $emitter.write_qualified_name($current_name)?;
        $emitter.write_type_arguments_suffix($type_parameters, "2")?;
        writeln!($emitter.f, " =")?;
        $emitter.f.indent();
        $to_js;
        $emitter.f.dedent();

        write!($emitter.f, "override def fromJS(b_ : ")?;
        $emitter.write_qualified_name($current_name)?;
        $emitter.write_type_arguments_suffix($type_parameters, "2")?;
        write!($emitter.f, "): ")?;
        zio_emitter($emitter).write_qualified_name($current_name)?;
        $emitter.write_type_arguments_suffix($type_parameters, "1")?;
        writeln!($emitter.f, " =")?;
        $emitter.f.indent();
        $from_js;
        $emitter.f.dedent();
        
        

        $emitter.f.dedent();
        writeln!($emitter.f, "}}")?;

        
    };
}


pub(in crate::emit) fn emit_sjs(model: &Model, options: &ScalaJSEmitOptions) -> Result<(), crate::Error> {
    common::emit(model, options)
}

impl <'a, 'b, W: Write> ScalaEmitter for Emitter<'a, 'b, W, ScalaJSEmitOptions> {
    fn emit_record<'c>(&'c mut self, package: &PackageName, rec: &RecordDefinition) -> Result<(), crate::Error> {
        let old_type_params = self.push_type_parameters(&rec.type_parameters);

        write!(self.f, "trait ")?;
        self.write_type_identifier(&rec.name)?;
        self.write_type_parameters(&rec.type_parameters)?;
        writeln!(self.f, " extends _root_.scala.scalajs.js.Object {{")?;
        self.f.indent();

        for field in &rec.fields {
            write!(self.f, "val ")?;
            self.write_identifier(&field.name)?;
            write!(self.f, ": ")?;
            self.write_type(&field.field_type)?;
            writeln!(self.f)?;
        }

        self.f.dedent();
        writeln!(self.f, "}}")?;


        if self.options().scala_zio_package_mapping.is_some() {
            write!(self.f, "object ")?;
            self.write_type_identifier(&rec.name)?;
            writeln!(self.f, " {{")?;
            self.f.indent();
    
            let current_name = QualifiedName(package.clone(), rec.name.clone());

            write_type_adapter!(self, &current_name, &rec.type_parameters, {
                write!(self.f, "new ")?;
                self.write_qualified_name(&current_name)?;
                self.write_type_arguments_suffix(&rec.type_parameters, "2")?;
                writeln!(self.f, "{{")?;
        
                self.f.indent();
        
        
                for field in &rec.fields {
                    write!(self.f, "override val ")?;
                    self.write_identifier(&field.name)?;
                    write!(self.f, ": ")?;
                    self.write_type_param_suffix(&field.field_type, "2")?;
                    write!(self.f, " = ")?;
                    write_type_adapter_of(self, &field.field_type)?;
                    write!(self.f, ".toJS(a_.")?;
        
                    
                    self.write_identifier(&field.name)?;
        
                    writeln!(self.f, ")")?;
                }
                
        
                self.f.dedent();
                writeln!(self.f, "}}")?;
            }, {
                zio_emitter(self).write_qualified_name(&current_name)?;
                self.write_type_arguments_suffix(&rec.type_parameters, "1")?;
                writeln!(self.f, "(")?;
        
                self.f.indent();
        
                for field in &rec.fields {
                    self.write_identifier(&field.name)?;
                    write!(self.f, " = ")?;
                    write_type_adapter_of(self, &field.field_type)?;
                    write!(self.f, ".fromJS(b_.")?;
                    self.write_identifier(&field.name)?;
                    writeln!(self.f, "),")?;
                }
        
                self.f.dedent();
                writeln!(self.f, ")")?;
            });

            self.f.dedent();
            writeln!(self.f, "}}")?;
        }

        self.pop_type_parameters(old_type_params);
        
        Ok(())
    }

    fn emit_enum(&mut self, package: &PackageName, e: &EnumDefinition) -> Result<(), crate::Error> {
        let old_type_params = self.push_type_parameters(&e.type_parameters);

        
        write!(self.f, "sealed trait ")?;
        self.write_type_identifier(&e.name)?;
        self.write_type_parameters(&e.type_parameters)?;
        writeln!(self.f, "{{")?;
        self.f.indent();

        writeln!(self.f, "val $type: String")?;

        self.f.dedent();
        writeln!(self.f, "}}")?;

        write!(self.f, "object ")?;
        self.write_type_identifier(&e.name)?;
        writeln!(self.f, " {{")?;
        self.f.indent();

        for c in &e.cases {
            write!(self.f, "trait ")?;
            self.write_type_identifier(&c.name)?;
            self.write_type_parameters(&e.type_parameters)?;
            write!(self.f, " extends ")?;

            self.write_type_identifier(&e.name)?;
            self.write_type_arguments(&e.type_parameters)?;

            writeln!(self.f, " {{")?;
            self.f.indent();

            writeln!(self.f, "override val $type: \"{}\"", c.name)?;

            for field in &c.fields {
                write!(self.f, "val ")?;
                self.write_identifier(&field.name)?;
                write!(self.f, ": ")?;
                self.write_type(&field.field_type)?;
                writeln!(self.f)?;
            }

            self.f.dedent();
            writeln!(self.f, "}}")?;
        }

        if self.options().scala_zio_package_mapping.is_some() {
            let current_name = QualifiedName(package.clone(), e.name.clone());

            write_type_adapter!(self, &current_name, &e.type_parameters, {
                writeln!(self.f, "a_ match {{")?;
                self.f.indent();

                for c in &e.cases {
                    write!(self.f, "case a_ : ")?;
                    zio_emitter(self).write_qualified_name(&current_name)?;
                    write!(self.f, ".")?;
                    self.write_type_identifier(&c.name)?;
                    self.write_type_arguments_suffix(&e.type_parameters, "1")?;
                    writeln!(self.f, " =>")?;

                    self.f.indent();

                    write!(self.f, "new ")?;
                    self.write_qualified_name(&current_name)?;
                    write!(self.f, ".")?;
                    self.write_type_identifier(&c.name)?;
                    self.write_type_arguments_suffix(&e.type_parameters, "2")?;
                    writeln!(self.f, "{{")?;
            
                    self.f.indent();
            
                    writeln!(self.f, "override val $type: \"{}\" = \"{}\"", c.name, c.name)?;
            
                    for field in &c.fields {
                        write!(self.f, "override val ")?;
                        self.write_identifier(&field.name)?;
                        write!(self.f, ": ")?;
                        self.write_type_param_suffix(&field.field_type, "2")?;
                        write!(self.f, " = ")?;
                        write_type_adapter_of(self, &field.field_type)?;
                        write!(self.f, ".toJS(a_.")?;
            
                        
                        self.write_identifier(&field.name)?;
            
                        writeln!(self.f, ")")?;
                    }
                    
            
                    self.f.dedent();
                    writeln!(self.f, "}}")?;

                    self.f.dedent();

                }

                self.f.dedent();
                writeln!(self.f, "}}")?;


            }, {
                writeln!(self.f, "b_.$type match {{")?;
                self.f.indent();

                for c in &e.cases {
                    writeln!(self.f, "case \"{}\" =>", c.name)?;
                    self.f.indent();

                    write!(self.f, "val b2_ = b_.asInstanceOf[")?;
                    self.write_qualified_name(&current_name)?;
                    write!(self.f, ".")?;
                    self.write_type_identifier(&c.name)?;
                    self.write_type_arguments_suffix(&e.type_parameters, "2")?;
                    writeln!(self.f, "]")?;


                    zio_emitter(self).write_qualified_name(&current_name)?;
                    write!(self.f, ".")?;
                    self.write_type_identifier(&c.name)?;
                    self.write_type_arguments_suffix(&e.type_parameters, "1")?;
                    writeln!(self.f, "(")?;
            
                    self.f.indent();
            
                    for field in &c.fields {
                        self.write_identifier(&field.name)?;
                        write!(self.f, " = ")?;
                        write_type_adapter_of(self, &field.field_type)?;
                        write!(self.f, ".fromJS(b2_.")?;
                        self.write_identifier(&field.name)?;
                        writeln!(self.f, "),")?;
                    }
            
                    self.f.dedent();
                    writeln!(self.f, ")")?;
            
                    self.f.dedent();

                }

                self.f.dedent();
                writeln!(self.f, "}}")?;


            });
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

            write!(self.f, "): _root_.scala.scalajs.js.Promise[")?;            
            self.write_type(&method.return_type)?;
            writeln!(self.f, "]")?;

            self.pop_type_parameters(old_type_params);
        }

        self.f.dedent();
        writeln!(self.f, "}}")?;


        if self.options().scala_zio_package_mapping.is_some() {
            write!(self.f, "object ")?;
            self.write_type_identifier(&iface.name)?;
            writeln!(self.f, " {{")?;
            self.f.indent();
    
            let current_name = QualifiedName(package.clone(), iface.name.clone());

            write_type_adapter!(self, &current_name, &iface.type_parameters, {
                write!(self.f, "new ")?;
                self.write_qualified_name(&current_name)?;
                self.write_type_arguments_suffix(&iface.type_parameters, "1")?;
                writeln!(self.f, " {{")?;
                self.f.indent();


                for method in &iface.methods {
                    let old_type_params = self.push_type_parameters(&method.type_parameters);
        
                    write!(self.f, "override def ")?;
                    self.write_identifier(&method.name)?;
                    self.write_type_parameters(&method.type_parameters)?;
                    write!(self.f, "(")?;
        
                    for_sep!(param, &method.parameters, { write!(self.f, ", ")?; }, {
                        self.write_identifier(&param.name)?;
                        write!(self.f, ": ")?;
                        self.write_type(&param.parameter_type)?;
                    });
        
                    write!(self.f, "): _root_.scala.scalajs.js.Promise[")?;            
                    self.write_type(&method.return_type)?;
                    writeln!(self.f, "] =")?;
                    self.f.indent();

                    

                    self.f.dedent();
        
                    self.pop_type_parameters(old_type_params);
                }


                self.f.dedent();
                writeln!(self.f, "}}")?;
                
            }, {
                zio_emitter(self).write_qualified_name(&current_name)?;
                self.write_type_arguments_suffix(&iface.type_parameters, "1")?;
            });

            self.f.dedent();
            writeln!(self.f, "}}")?;
        }


        self.pop_type_parameters(old_type_params);

        Ok(())
    }
    
    fn convert_identifier(&self, name: &str) -> String {
        name_to_camel_case(name)
    }

    
}

fn zio_emitter<'a: 'd, 'b, 'c: 'd, 'd, W: Write>(parent_emitter: &'c mut Emitter<'a, 'b, W, ScalaJSEmitOptions>) -> Emitter<'d, 'b, W, ScalaEmitOptions> {
    Emitter::new(parent_emitter.f, &parent_emitter.extras.scala_zio_options)
}

fn write_type_adapter_type<'a, 'b, W: Write>(emitter: &mut Emitter<'a, 'b, W, ScalaJSEmitOptions>, current_name: &QualifiedName, params: &[TypeParameter]) -> Result<(), crate::Error> {
    write!(emitter.f, "{}[", TYPE_ADAPTER_TYPE)?;
    zio_emitter(emitter).write_qualified_name(current_name)?;
    emitter.write_type_arguments_suffix(params, "1")?;
    write!(emitter.f, ", ")?;
    emitter.write_qualified_name(current_name)?;
    emitter.write_type_arguments_suffix(params, "2")?;
    write!(emitter.f, "]")?;

    Ok(())
}

fn write_type_adapter_of<'a, 'b, W: Write>(emitter: &mut Emitter<'a, 'b, W, ScalaJSEmitOptions>, t: &TypeExpr) -> Result<(), crate::Error> {
    match t {
        TypeExpr::Name(name) if name.0.0.is_empty() && emitter.is_type_parameter(&name.1) => {
            write!(emitter.f, "{}JSTypeAdapter", emitter.convert_identifier(&name.1))?;
        },
        TypeExpr::Name(name) => {
            emitter.write_qualified_name(name)?;
            write!(emitter.f, ".jsTypeAdapter")?;
        },
        TypeExpr::Apply(f, args) => {
            emitter.write_type(f.as_ref())?;
            write!(emitter.f, ".jsTypeAdapter(")?;
            for_sep!(arg, args, { write!(emitter.f, ", ")?; }, {
                write_type_adapter_of(emitter, arg)?;
            });
            write!(emitter.f, ")")?;
        },
    }

    Ok(())
}





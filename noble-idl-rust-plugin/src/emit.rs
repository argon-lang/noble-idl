use std::{collections::HashMap, io::{Read, Write}, path::PathBuf};
use esexpr_binary::FixedStringPool;
use itertools::Itertools;
use proc_macro2::TokenStream;
use quote::quote;

use esexpr::ESExprCodec;
use noble_idl_api::{Definition, DefinitionInfo, EnumCase, EnumDefinition, InterfaceDefinition, InterfaceMethod, InterfaceMethodParameter, NobleIDLDefinitions, NobleIDLGenerationResult, PackageName, RecordDefinition, RecordField, TypeExpr, TypeParameter};
use syn::punctuated::Punctuated;

use crate::RustLanguageOptions;

#[derive(derive_more::From, Debug)]
pub enum EmitError {
    #[from(ignore)]
    UnmappedPackage(PackageName),

    IOError(std::io::Error),
    ProtocolParseError(esexpr_binary::ParseError),
    ProtocolDecodeError(esexpr::DecodeError),
    ProtocolGenerateError(esexpr_binary::GeneratorError),
    RustParseError(syn::Error),
    InvalidFileName,
}

pub fn emit(definitions: NobleIDLDefinitions<RustLanguageOptions>) -> Result<NobleIDLGenerationResult, EmitError> {
    let mut def_groups = HashMap::new();

    for def in &definitions.definitions {
        let items = def_groups.entry(&def.name.0).or_insert_with(Vec::new);
        items.push(def);
    }

    let pkg_mapping = get_package_mapping(&definitions.language_options);

    let mut emitter = ModEmitter {
        definitions: definitions.definitions,
        pkg_mapping,

        output_dir: PathBuf::from(&definitions.language_options.output_dir),
        output_files: Vec::new(),
    };

    emitter.emit_modules()?;

    Ok(emitter.generation_result())
}

pub fn emit_from_stream<R: Read, W: Write>(input: R, mut output: W) -> Result<(), EmitError> {
    let mut sp = esexpr_binary::StringPoolBuilder::new();
    let mut results = Vec::new();

    for def in esexpr_binary::parse_embedded_string_pool(input)? {
        let def = def?;
        let def = ESExprCodec::decode_esexpr(def)?;

        let res = emit(def)?;

        let res = res.encode_esexpr();

        sp.add(&res);
        results.push(res);
    }

    let mut sp = sp.into_fixed_string_pool();

    esexpr_binary::generate(&mut output, &mut FixedStringPool { strings: vec!() }, &sp.clone().encode_esexpr())?;

    for expr in results {
        esexpr_binary::generate(&mut output, &mut sp, &expr)?;
    }

    Ok(())
}



#[derive(Debug)]
struct RustModule<'a> {
    crate_name: Option<String>,
    module: &'a str,
}

struct ModEmitter<'a> {
    definitions: Vec<DefinitionInfo>,
    pkg_mapping: HashMap<PackageName, RustModule<'a>>,

    output_dir: PathBuf,
    output_files: Vec<String>,
}

impl <'a> ModEmitter<'a> {
    fn emit_modules(&mut self) -> Result<(), EmitError> {
        let mut package_groups = HashMap::new();
        for dfn in &self.definitions {
            let dfns = package_groups.entry(dfn.name.package_name())
                .or_insert_with(|| Vec::new());

            dfns.push(dfn);
        }

        for (pkg, dfns) in &package_groups {
            let p = self.emit_module(pkg, dfns)?;
            self.output_files.push(p.as_os_str().to_str().ok_or(EmitError::InvalidFileName)?.to_owned());
        }

        Ok(())
    }

    fn generation_result(self) -> NobleIDLGenerationResult {
        NobleIDLGenerationResult {
            generated_files: self.output_files,
        }
    }

    fn build_package_path(&self, package_name: &PackageName) -> PathBuf {
        let mut p = self.output_dir.clone();
        let mut pkg_iter = package_name.0.iter();

        if let Some(last_seg) = pkg_iter.next_back() {
            for seg in pkg_iter {
                p.push(seg.replace("-", "_"));
            }

            p.push(last_seg.replace("-", "_") + ".rs");
        }
        else {
            p.push("_root_.rs");
        }

        p
    }

    fn get_rust_module(&self, package_name: &PackageName) -> Result<&RustModule, EmitError> {
        self.pkg_mapping.get(package_name).ok_or_else(|| EmitError::UnmappedPackage(package_name.clone()))
    }

    fn emit_module(&self, package_name: &PackageName, definitions: &[&DefinitionInfo]) -> Result<PathBuf, EmitError>  {
        let p = self.build_package_path(package_name);

        if let Some(parent) = p.parent() {
            std::fs::create_dir_all(parent)?;
        }

        let mut f = std::fs::File::create(&p)?;

        let defs_code = definitions.iter().map(|dfn| self.emit_definition(dfn)).collect::<Result<TokenStream, _>>()?;

        let content = quote! {
            #defs_code
        };

        let rust_file = syn::parse2::<syn::File>(content)?;

        let pretty_code = prettyplease::unparse(&rust_file);

        write!(f, "{}", pretty_code)?;

        Ok(p)
    }


    fn emit_definition(&self, dfn: &DefinitionInfo) -> Result<TokenStream, EmitError> {
        match &dfn.definition {
            Definition::Record(r) => self.emit_record(dfn, r),
            Definition::Enum(e) => self.emit_enum(dfn, e),
            Definition::ExternType => Ok(quote! {}),
            Definition::Interface(i) => self.emit_interface(dfn, i),
        }
    }

    fn emit_record(&self, dfn: &DefinitionInfo, r: &RecordDefinition) -> Result<TokenStream, EmitError> {
        let rec_name = convert_id_pascal(dfn.name.name());

        let type_parameters = self.emit_type_parameters(&dfn.type_parameters);

        let fields = self.emit_fields(&r.fields)?;

        Ok(quote! {
            struct #rec_name #type_parameters {
                #fields
            }
        })
    }

    fn emit_enum(&self, dfn: &DefinitionInfo, e: &EnumDefinition) -> Result<TokenStream, EmitError> {
        let enum_name = convert_id_pascal(dfn.name.name());

        let type_parameters = self.emit_type_parameters(&dfn.type_parameters);

        let cases: TokenStream = e.cases.iter().map(|c| self.emit_enum_case(c)).collect::<Result<_, _>>()?;

        Ok(quote! {
            enum #enum_name #type_parameters {
                #cases
            }
        })
    }

    fn emit_enum_case(&self, c: &EnumCase) -> Result<TokenStream, EmitError> {
        let case_name = convert_id_pascal(&c.name);
        let fields = self.emit_fields(&c.fields)?;
        Ok(quote! {
            #case_name {
                #fields
            },
        })
    }

    fn emit_interface(&self, dfn: &DefinitionInfo, i: &InterfaceDefinition) -> Result<TokenStream, EmitError> {
        let if_name = convert_id_pascal(dfn.name.name());

        let type_parameters = self.emit_type_parameters(&dfn.type_parameters);

        let methods: TokenStream = i.methods.iter().map(|m| self.emit_method(m)).collect::<Result<_, _>>()?;

        Ok(quote! {
            trait #if_name #type_parameters {
                #methods
            }
        })
    }


    fn emit_type_parameters(&self, type_params: &[TypeParameter]) -> TokenStream {
        if type_params.is_empty() {
            quote! {}
        } else {
            let type_params: Vec<syn::TypeParam> = type_params
                .into_iter()
                .map(|param| match param {
                    TypeParameter::Type(param) =>
                        syn::TypeParam::from(convert_id_pascal(param)),
                })
                .collect();

            quote! {
                <#(#type_params),*>
            }
        }
    }

    fn emit_fields(&self, fields: &[RecordField]) -> Result<TokenStream, EmitError> {
        fields.iter().map(|f| self.emit_field(f)).collect()
    }

    fn emit_field(&self, field: &RecordField) -> Result<TokenStream, EmitError> {
        let field_name = convert_id_snake(&field.name);
        let field_type = self.emit_type_expr(&field.field_type)?;
        Ok(quote! {
            #field_name: #field_type,
        })
    }

    fn emit_method(&self, m: &InterfaceMethod) -> Result<TokenStream, EmitError> {
        let method_name = convert_id_snake(&m.name);
        let type_params = self.emit_type_parameters(&m.type_parameters);
        let params = self.emit_method_parameters(&m.parameters)?;
        let return_type = self.emit_type_expr(&m.return_type)?;

        Ok(quote! {
            fn #method_name #type_params (#params) -> #return_type;
        })
    }

    fn emit_method_parameters(&self, parameters: &[InterfaceMethodParameter]) -> Result<TokenStream, EmitError> {
        let params = parameters.iter().map(|param| -> Result<TokenStream, EmitError> {
            let param_name = idstr(&param.name);
            let param_type = self.emit_type_expr(&param.parameter_type)?;
            Ok(quote! {
                #param_name: #param_type
            })
        }).collect::<Result<Vec<_>, _>>()?;

        Ok(quote! {
            #(#params),*
        })
    }

    fn emit_type_expr(&self, t: &TypeExpr) -> Result<syn::Type, EmitError> {
        Ok(match t {
            TypeExpr::DefinedType(name) => {
                
                let rust_mod = self.get_rust_module(name.package_name())?;
                let mut segments = Punctuated::new();

                segments.push(syn::PathSegment {
                    ident: idstr(rust_mod.crate_name.as_ref().map(String::as_str).unwrap_or("crate")),
                    arguments: syn::PathArguments::None,
                });
                
                if !rust_mod.module.is_empty() {
                    for mod_part in rust_mod.module.split("::") {
                        segments.push(syn::PathSegment {
                            ident: idstr(mod_part),
                            arguments: syn::PathArguments::None,
                        });
                    }    
                }

                segments.push(syn::PathSegment {
                    ident: convert_id_pascal(name.name()),
                    arguments: syn::PathArguments::None,
                });

                syn::Type::Path(syn::TypePath {
                    qself: None,
                    path: syn::Path {
                        leading_colon: if rust_mod.crate_name.is_some() { Some(Default::default()) } else { None },
                        segments,
                    },
                })
            },

            TypeExpr::TypeParameter(name) =>
                syn::Type::Path(syn::TypePath {
                    qself: None,
                    path: syn::Path::from(convert_id_pascal(&name)),
                }),

            TypeExpr::Apply(f, type_args) => {
                if let syn::Type::Path(mut type_path) = self.emit_type_expr(f)? {
                    let args = type_args.iter()
                        .map(|a| Ok(syn::GenericArgument::Type(self.emit_type_expr(a)?)))
                        .collect::<Result<Punctuated<_, _>, EmitError>>()?;
                    let segment = type_path.path.segments.last_mut().unwrap();
                    segment.arguments = syn::PathArguments::AngleBracketed(syn::AngleBracketedGenericArguments {
                        colon2_token: None,
                        lt_token: Default::default(),
                        args,
                        gt_token: Default::default(),
                    });
                    syn::Type::Path(type_path)
                } else {
                    panic!("Expected a TypePath in Apply")
                }
            },
        })
    }
    
}


fn convert_id_snake(s: &str) -> syn::Ident {
    idstr(&s.replace("-", "_"))
}

fn convert_id_pascal(s: &str) -> syn::Ident {
    idstr(&s.split("-").map(|seg| {
        let mut seg = seg.to_owned();
        if !seg.is_empty() {
            str::make_ascii_uppercase(&mut seg[0..1]);
        }
        seg
    }).join(""))
}

fn idstr(s: &str) -> syn::Ident {
    syn::Ident::new(s, proc_macro2::Span::mixed_site())
}


fn get_package_mapping<'a>(options: &'a RustLanguageOptions) -> HashMap<PackageName, RustModule<'a>> {
    let mut pkg_mapping = HashMap::new();

    for (crate_name, crate_options) in &options.crates.crate_options {
        let is_root_crate = options.crate_name == *crate_name;

        for (idl_pkg, rust_pkg) in &crate_options.package_mapping.package_mapping {
            let idl_pkg_name = PackageName::from_str(&idl_pkg);

            let crate_name = if is_root_crate { None } else { Some(crate_name.as_str().replace("-", "_")) };
    
            pkg_mapping.insert(idl_pkg_name, RustModule {
                crate_name,
                module: rust_pkg
            });
        }
    }

    pkg_mapping
}



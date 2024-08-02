use std::{collections::HashMap, io::{Read, Write}, path::PathBuf};
use esexpr_binary::FixedStringPool;
use itertools::Itertools;
use num_bigint::{BigInt, BigUint, Sign};
use proc_macro2::TokenStream;
use quote::quote;

use esexpr::ESExprCodec;
use noble_idl_api::*;
use syn::{parse_quote, punctuated::Punctuated};

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
	InvalidLiteralForType(TypeExpr),
    InvalidFileName,
}

pub fn emit(request: NobleIDLGenerationRequest<RustLanguageOptions>) -> Result<NobleIDLGenerationResult, EmitError> {
    let pkg_mapping = get_package_mapping(&request.language_options);

	let definitions = request.model.definitions
		.into_iter()
		.map(|dfn| (dfn.name.clone(), dfn))
		.collect::<HashMap<_, _>>();

    let mut emitter = ModEmitter {
        definitions,
        pkg_mapping,
		current_crate: &request.language_options.crate_name,

        output_dir: PathBuf::from(&request.language_options.output_dir),
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
	definitions: HashMap<QualifiedName, DefinitionInfo>,
    pkg_mapping: HashMap<PackageName, RustModule<'a>>,
	current_crate: &'a str,

    output_dir: PathBuf,
    output_files: Vec<String>,
}

impl <'a> ModEmitter<'a> {
    fn emit_modules(&mut self) -> Result<(), EmitError> {
        let mut package_groups = HashMap::new();
        for dfn in self.definitions.values() {
			if dfn.is_library {
				continue;
			}

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

    fn build_package_path(&self, package_name: &PackageName) -> Result<PathBuf, EmitError> {


        let mut p = self.output_dir.clone();

		let crate_name = self.current_crate.replace("-", "_");

		let rust_module = self.get_rust_module(package_name)?;
		if rust_module.module.is_empty() {
			p.push(format!("{}.rs", crate_name));
		}
		else {
			p.push(format!("{}::{}.rs", crate_name, rust_module.module));
		}

        Ok(p)
    }

    fn get_rust_module(&self, package_name: &PackageName) -> Result<&RustModule, EmitError> {
        self.pkg_mapping.get(package_name).ok_or_else(|| EmitError::UnmappedPackage(package_name.clone()))
    }

    fn emit_module(&self, package_name: &PackageName, definitions: &[&DefinitionInfo]) -> Result<PathBuf, EmitError>  {
        let p = self.build_package_path(package_name)?;

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
            Definition::ExternType(_) => Ok(quote! {}),
            Definition::Interface(i) => self.emit_interface(dfn, i),
        }
    }

    fn emit_record(&self, dfn: &DefinitionInfo, r: &RecordDefinition) -> Result<TokenStream, EmitError> {
        let rec_name = convert_id_pascal(dfn.name.name());

        let type_parameters = self.emit_type_parameters(&dfn.type_parameters);

        let fields = self.emit_fields(true, &r.fields)?;

        let mut derives = Vec::new();
        let mut attrs = Vec::new();
        self.process_record_ann(r, &mut derives, &mut attrs)?;
        let attrs = attrs.into_iter().collect::<TokenStream>();

        Ok(quote! {
            #[derive(#(#derives),*)]
            #attrs
            pub struct #rec_name #type_parameters {
                #fields
            }
        })
    }

    fn process_record_ann(&self, r: &RecordDefinition, derives: &mut Vec<TokenStream>, attrs: &mut Vec<TokenStream>) -> Result<(), EmitError>  {
        derives.push(quote! { ::std::fmt::Debug });
        derives.push(quote! { ::std::clone::Clone });
        derives.push(quote! { ::std::cmp::PartialEq });

		if let Some(esexpr_options) = &r.esexpr_options {
			derives.push(quote! { ::esexpr::ESExprCodec });

			let name = &esexpr_options.constructor;
			attrs.push(quote! { #[constructor = #name] });
		}

        Ok(())
    }

    fn emit_enum(&self, dfn: &DefinitionInfo, e: &EnumDefinition) -> Result<TokenStream, EmitError> {
        let enum_name = convert_id_pascal(dfn.name.name());

        let type_parameters = self.emit_type_parameters(&dfn.type_parameters);

        let cases: TokenStream = e.cases.iter().map(|c| self.emit_enum_case(c)).collect::<Result<_, _>>()?;

        let mut derives = Vec::new();
        let mut attrs = Vec::new();
        self.process_enum_ann(e, &mut derives, &mut attrs)?;
        let attrs = attrs.into_iter().collect::<TokenStream>();

        Ok(quote! {
            #[derive(#(#derives),*)]
            #attrs
            pub enum #enum_name #type_parameters {
                #cases
            }
        })
    }

    fn process_enum_ann(&self, e: &EnumDefinition, derives: &mut Vec<TokenStream>, attrs: &mut Vec<TokenStream>) -> Result<(), EmitError>  {
        derives.push(quote! { ::std::fmt::Debug });
        derives.push(quote! { ::std::clone::Clone });
        derives.push(quote! { ::std::cmp::PartialEq });

		if let Some(esexpr_options) = &e.esexpr_options {
			derives.push(quote! { ::esexpr::ESExprCodec });

			if esexpr_options.simple_enum {
				attrs.push(quote! { #[simple_enum] });
			}
		}

        Ok(())
    }

    fn emit_enum_case(&self, c: &EnumCase) -> Result<TokenStream, EmitError> {
        let case_name = convert_id_pascal(&c.name);
        let fields = self.emit_fields(false, &c.fields)?;

        let mut attrs = Vec::new();
        self.process_enum_case_ann(c, &mut attrs)?;
        let attrs = attrs.into_iter().collect::<TokenStream>();

        Ok(quote! {
            #attrs
            #case_name {
                #fields
            },
        })
    }

    fn process_enum_case_ann(&self, c: &EnumCase, attrs: &mut Vec<TokenStream>) -> Result<(), EmitError> {
		if let Some(esexpr_options) = &c.esexpr_options {
			match &esexpr_options.case_type {
				ESExprEnumCaseType::Constructor(name) => attrs.push(quote! { #[constructor = #name] }),
				ESExprEnumCaseType::InlineValue => attrs.push(quote! { #[inline_value] }),
			}
		}

        Ok(())
    }

    fn emit_interface(&self, dfn: &DefinitionInfo, i: &InterfaceDefinition) -> Result<TokenStream, EmitError> {
        let if_name = convert_id_pascal(dfn.name.name());

        let type_parameters = self.emit_type_parameters(&dfn.type_parameters);

        let methods: TokenStream = i.methods.iter().map(|m| self.emit_method(m)).collect::<Result<_, _>>()?;

        Ok(quote! {
            pub trait #if_name #type_parameters {
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

    fn emit_fields(&self, use_pub: bool, fields: &[RecordField]) -> Result<TokenStream, EmitError> {
        fields.iter().map(|f| self.emit_field(use_pub, f)).collect()
    }

    fn emit_field(&self, use_pub: bool, field: &RecordField) -> Result<TokenStream, EmitError> {
        let field_name = convert_id_snake(&field.name);
        let field_type = self.emit_type_expr(&field.field_type)?;

        let mut attrs = Vec::new();
        self.process_field_ann(field, &mut attrs)?;
        let attrs = attrs.into_iter().collect::<TokenStream>();

		let pub_kw = if use_pub { quote! { pub } } else { quote!{} };

        Ok(quote! {
            #attrs
            #pub_kw #field_name: #field_type,
        })
    }

    fn process_field_ann(&self, field: &RecordField, attrs: &mut Vec<TokenStream>) -> Result<(), EmitError> {
		if let Some(esexpr_options) = &field.esexpr_options {
			match &esexpr_options.kind {
				ESExprRecordFieldKind::Positional(ESExprRecordPositionalMode::Required) => {},
				ESExprRecordFieldKind::Positional(ESExprRecordPositionalMode::Optional(_)) => {
					attrs.push(quote! { #[optional] });
				},
				ESExprRecordFieldKind::Keyword(name, ESExprRecordKeywordMode::Required) => {
					attrs.push(quote! { #[keyword = #name] });
				},
				ESExprRecordFieldKind::Keyword(name, ESExprRecordKeywordMode::DefaultValue(default_value)) => {
					attrs.push(quote! { #[keyword = #name] });

					let value = self.emit_value(default_value)?;
					attrs.push(quote! { #[default_value = #value] });
				},
				ESExprRecordFieldKind::Keyword(name, ESExprRecordKeywordMode::Optional(_)) => {
					attrs.push(quote! { #[keyword = #name] });
					attrs.push(quote! { #[optional] });

				},
				ESExprRecordFieldKind::Dict(_) => attrs.push(quote! { #[dict] }),
				ESExprRecordFieldKind::Vararg(_) => attrs.push(quote! { #[vararg] }),
			}
		}

        Ok(())
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
		let path = self.get_type_path(t)?;

        Ok(syn::Type::Path(syn::TypePath {
			qself: None,
			path,
		}))
    }

	fn get_type_path(&self, t: &TypeExpr) -> Result<syn::Path, EmitError> {
        Ok(match t {
            TypeExpr::DefinedType(name, args) => {

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

				let arguments =
					if args.is_empty() {
						syn::PathArguments::None
					}
					else {
						let args = args.iter()
							.map(|a| Ok(syn::GenericArgument::Type(self.emit_type_expr(a)?)))
							.collect::<Result<Punctuated<_, _>, EmitError>>()?;

						syn::PathArguments::AngleBracketed(syn::AngleBracketedGenericArguments {
							colon2_token: None,
							lt_token: Default::default(),
							args,
							gt_token: Default::default(),
						})
					};

                segments.push(syn::PathSegment {
                    ident: convert_id_pascal(name.name()),
                    arguments,
                });

                syn::Path {
					leading_colon: if rust_mod.crate_name.is_some() { Some(Default::default()) } else { None },
					segments,
				}
            },

            TypeExpr::TypeParameter(name) =>
				syn::Path::from(convert_id_pascal(&name)),
        })
	}


	fn emit_value(&self, value: &ESExprDecodedValue) -> Result<syn::Expr, EmitError> {
		match value {
			ESExprDecodedValue::Record(t, field_values) => self.emit_record_value(t, field_values),
			ESExprDecodedValue::Enum(t, case_name, field_values) => self.emit_enum_value(t, case_name, field_values),
			ESExprDecodedValue::Optional { optional_type, value } => self.emit_optional_value(optional_type, value.as_deref()),
			ESExprDecodedValue::Vararg { vararg_type, values } => self.emit_vararg_value(vararg_type, values),
			ESExprDecodedValue::Dict { dict_type, values } => self.emit_dict_value(dict_type, values),
			ESExprDecodedValue::BuildFrom(built_type, value) => self.emit_build_from(built_type, &**value),
			ESExprDecodedValue::FromBool(t, value) => self.emit_literal_primitive::<bool>(t, *value),
			ESExprDecodedValue::FromInt { t, i, min_int, max_int } => self.emit_literal_int(t, i, min_int.as_ref(), max_int.as_ref()),
			ESExprDecodedValue::FromStr(t, value) => self.emit_literal_primitive::<&str>(t, value),
			ESExprDecodedValue::FromBinary(t, value) => self.emit_literal_binary(t, &value.0),
			ESExprDecodedValue::FromFloat32(t, value) => self.emit_literal_primitive::<f32>(t, *value),
			ESExprDecodedValue::FromFloat64(t, value) => self.emit_literal_primitive::<f64>(t, *value),
			ESExprDecodedValue::FromNull(t) => self.emit_literal_null(t),
		}
	}

	fn emit_record_value(&self, t: &TypeExpr, field_values: &HashMap<String, ESExprDecodedValue>) -> Result<syn::Expr, EmitError> {
		Ok(syn::Expr::Struct(syn::ExprStruct {
			attrs: vec![],
			qself: None,
			path: self.get_type_path(t)?,
			brace_token: syn::token::Brace::default(),
			fields: self.emit_field_values(field_values)?.into_iter().collect(),
			dot2_token: None,
			rest: None,
		}))
	}

	fn emit_enum_value(&self, t: &TypeExpr, case_name: &str, field_values: &HashMap<String, ESExprDecodedValue>) -> Result<syn::Expr, EmitError> {
		let mut path = self.get_type_path(t)?;
		path.segments.push(syn::PathSegment {
			ident: convert_id_pascal(case_name),
			arguments: syn::PathArguments::None,
		});

		Ok(syn::Expr::Struct(syn::ExprStruct {
			attrs: vec![],
			qself: None,
			path,
			brace_token: Default::default(),
			fields: self.emit_field_values(field_values)?.into_iter().collect(),
			dot2_token: None,
			rest: None,
		}))
	}

	fn emit_field_values(&self, field_values: &HashMap<String, ESExprDecodedValue>) -> Result<Vec<syn::FieldValue>, EmitError> {
		field_values.iter().map(|(name, value)| {
			let value = self.emit_value(value)?;
			Ok(syn::FieldValue {
				attrs: vec![],
				member: syn::Member::Named(convert_id_snake(name)),
				colon_token: Some(syn::token::Colon::default()),
				expr: value,
			})
		}).collect::<Result<Vec<_>, _>>()
	}

	fn emit_optional_value(&self, optional_type: &TypeExpr, value: Option<&ESExprDecodedValue>) -> Result<syn::Expr, EmitError> {
		let t = self.emit_type_expr(optional_type)?;
		let v = value.map(|v| self.emit_value(v)).transpose()?;
		let v: syn::Expr = match v {
			Some(v) => parse_quote! { std::option::Option::Some(#v) },
			None => parse_quote! { ::std::option::Option::None },
		};

		Ok(parse_quote! {
			<#t as std::convert::From<_>>::from(#v)
		})
	}

	fn emit_vararg_value(&self, vararg_type: &TypeExpr, values: &[ESExprDecodedValue]) -> Result<syn::Expr, EmitError> {
		let t = self.emit_type_expr(vararg_type)?;
		let v = values.iter().map(|v| self.emit_value(v)).collect::<Result<Vec<_>, _>>()?;
		let v: syn::Expr = parse_quote! { ::std::vec![#(#v),*] };

		Ok(parse_quote! {
			<#t as std::convert::From<_>>::from(#v)
		})
	}

	fn emit_dict_value(&self, dict_type: &TypeExpr, values: &HashMap<String, ESExprDecodedValue>) -> Result<syn::Expr, EmitError> {
		let t = self.emit_type_expr(dict_type)?;
		let v = values.iter().map(|(k, v)| {
			let v = self.emit_value(v)?;
			Ok(parse_quote! { (#k, #v) })
		}).collect::<Result<Vec<syn::Expr>, EmitError>>()?;

		let v: syn::Expr = parse_quote! { ::std::collection::HashMap::from([#(#v),*]) };

		Ok(parse_quote! {
			<#t as std::convert::From<_>>::from(#v)
		})
	}

	fn emit_build_from(&self, built_type: &TypeExpr, value: &ESExprDecodedValue) -> Result<syn::Expr, EmitError> {
		let t = self.emit_type_expr(built_type)?;
		let v = self.emit_value(value)?;

		Ok(parse_quote! {
			<#t as std::convert::From<_>>::from(#v)
		})
	}

	fn emit_literal_primitive<T: quote::ToTokens>(&self, t: &TypeExpr, value: T) -> Result<syn::Expr, EmitError> {
		let t = self.emit_type_expr(t)?;

		Ok(parse_quote! {
			<#t as std::convert::From<_>>::from(#value)
		})
	}

	fn emit_literal_int(&self, t: &TypeExpr, value: &BigInt, min: Option<&BigInt>, max: Option<&BigInt>) -> Result<syn::Expr, EmitError> {
		let t = self.emit_type_expr(t)?;

		fn try_as<
			I: num_traits::bounds::Bounded + for<'a> TryFrom<&'a BigInt> + Copy + quote::ToTokens
		>(
			t: &syn::Type,
			value: &BigInt,
			min: Option<&BigInt>,
			max: Option<&BigInt>
		) -> Option<syn::Expr> {
			// Ensure that min/max are within the range of this type.
			I::try_from(min?).ok()?;
			I::try_from(max?).ok()?;

			let value = I::try_from(value).ok()?;

			Some(parse_quote! { <#t as std::convert::From<_>>::from(#value) })
		}

		let try_as_nat = || {
			if !min.is_some_and(|min| min.sign() >= Sign::NoSign) {
				return None;
			}

			let value = BigUint::try_from(value).ok()?;
			let bytes = value.to_bytes_le();

			let literal = syn::Lit::ByteStr(syn::LitByteStr::new(&bytes, proc_macro2::Span::mixed_site()));

			Some(parse_quote! {
				<#t as std::convert::From<::num_bigint::BigUint>>::from(::num_bigint::BigUint::from_bytes_le(#literal))
			})
		};

		let as_bigint = || {
			let (sign, bytes) = value.to_bytes_le();

			let sign_expr: syn::Expr = match sign {
				Sign::Minus => parse_quote! { ::num_bigint::Sign::Minus },
				Sign::NoSign => parse_quote! { ::num_bigint::Sign::NoSign },
				Sign::Plus => parse_quote! { ::num_bigint::Sign::Plus },
			};

			let literal = syn::Lit::ByteStr(syn::LitByteStr::new(&bytes, proc_macro2::Span::mixed_site()));

			parse_quote! {
				<#t as std::convert::From<::num_bigint::BigInt>>::from(::num_bigint::BigInt::from_bytes_le(#sign_expr, #literal))
			}
		};

		Ok(
			try_as::<u8>(&t, value, min, max)
				.or_else(|| try_as::<i8>(&t, value, min, max))
				.or_else(|| try_as::<u16>(&t, value, min, max))
				.or_else(|| try_as::<i16>(&t, value, min, max))
				.or_else(|| try_as::<u32>(&t, value, min, max))
				.or_else(|| try_as::<i32>(&t, value, min, max))
				.or_else(|| try_as::<u64>(&t, value, min, max))
				.or_else(|| try_as::<i64>(&t, value, min, max))
				.or_else(|| try_as::<u128>(&t, value, min, max))
				.or_else(|| try_as::<i128>(&t, value, min, max))
				.or_else(|| try_as_nat())
				.unwrap_or_else(|| as_bigint())
		)
	}

	fn emit_literal_binary(&self, t: &TypeExpr, value: &[u8]) -> Result<syn::Expr, EmitError> {
		let t = self.emit_type_expr(t)?;

		let literal = syn::Lit::ByteStr(syn::LitByteStr::new(&value, proc_macro2::Span::mixed_site()));

		Ok(parse_quote! {
			<#t as std::convert::From<&[u8]>>::from(#literal)
		})
	}

	fn emit_literal_null(&self, t: &TypeExpr) -> Result<syn::Expr, EmitError> {
		let t = self.emit_type_expr(t)?;

		Ok(parse_quote! {
			<#t as ::std::default::Default>::default()
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



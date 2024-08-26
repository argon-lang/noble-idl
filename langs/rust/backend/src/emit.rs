use std::{collections::HashMap, io::{Read, Write}, path::PathBuf, vec};
use esexpr_binary::FixedStringPool;
use itertools::Itertools;
use num_bigint::{BigInt, BigUint, Sign};
use proc_macro2::{Span, TokenStream};
use quote::{quote, format_ident, ToTokens};

use esexpr::ESExprCodec;
use noble_idl_api::*;
use syn::{parse_quote, punctuated::Punctuated, Block};

use crate::{annotations::{RustAnnEnum, RustAnnEnumCase, RustAnnRecord}, RustLanguageOptions};

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

	#[from(ignore)]
	TupleAndUnit(QualifiedName, Option<String>),

	#[from(ignore)]
	UnitWithFields(QualifiedName, Option<String>),
}

pub fn emit(request: NobleIdlGenerationRequest<RustLanguageOptions>) -> Result<NobleIdlGenerationResult, EmitError> {
	let pkg_mapping = get_package_mapping(&request.language_options);

	let definition_map = request.model.definitions
		.iter()
		.map(|dfn| (&*dfn.name, &**dfn))
		.collect::<HashMap<_, _>>();

	let mut emitter = ModEmitter {
		definitions: &request.model.definitions,
		pkg_mapping,
		current_crate: &request.language_options.crate_name,

		output_dir: PathBuf::from(&request.language_options.output_dir),
		output_files: Vec::new(),

		definition_map,
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
	definitions: &'a Vec<Box<DefinitionInfo>>,
	pkg_mapping: HashMap<PackageName, RustModule<'a>>,
	current_crate: &'a str,

	output_dir: PathBuf,
	output_files: Vec<String>,

	definition_map: HashMap<&'a QualifiedName, &'a DefinitionInfo>,
}

impl <'a> ModEmitter<'a> {
	fn emit_modules(&mut self) -> Result<(), EmitError> {
		let mut package_groups = HashMap::new();
		for dfn in self.definitions {
			if dfn.is_library {
				continue;
			}

			let dfns = package_groups.entry(dfn.name.package_name())
				.or_insert_with(|| Vec::new());

			dfns.push(&**dfn);
		}

		for (pkg, dfns) in package_groups {
			let p = self.emit_module(pkg, &dfns)?;
			self.output_files.push(p.as_os_str().to_str().ok_or(EmitError::InvalidFileName)?.to_owned());
		}

		Ok(())
	}

	fn generation_result(self) -> NobleIdlGenerationResult {
		NobleIdlGenerationResult {
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

	fn emit_module(&mut self, package_name: &PackageName, definitions: &[&'a DefinitionInfo]) -> Result<PathBuf, EmitError>  {
		let p = self.build_package_path(package_name)?;

		if let Some(parent) = p.parent() {
			std::fs::create_dir_all(parent)?;
		}

		let mut f = std::fs::File::create(&p)?;

		let defs_code = definitions.iter().map(|dfn| self.emit_definition(dfn)).collect::<Result<TokenStream, _>>()?;

		match syn::parse2::<syn::File>(quote! { #defs_code }) {
			Ok(rust_file) => {
				write!(f, "{}", prettyplease::unparse(&rust_file))?;
			},
			Err(_) => {
				write!(f, "{}", quote! { #defs_code })?;
			},
		};

		Ok(p)
	}


	fn emit_definition(&mut self, dfn: &'a DefinitionInfo) -> Result<TokenStream, EmitError> {
		match dfn.definition.as_ref() {
			Definition::Record(r) => self.emit_record(dfn, r),
			Definition::Enum(e) => self.emit_enum(dfn, e),
			Definition::SimpleEnum(e) => self.emit_simple_enum(dfn, e),
			Definition::ExternType(_) => Ok(quote! {}),
			Definition::Interface(i) => self.emit_interface(dfn, i),
			Definition::ExceptionType(ex) => self.emit_exception_type(dfn, ex),
		}
	}

	fn emit_record(&mut self, dfn: &'a DefinitionInfo, r: &'a RecordDefinition) -> Result<TokenStream, EmitError> {
		let rec_name = convert_id_pascal(dfn.name.name());

		let type_parameters = self.emit_type_parameters(&dfn.type_parameters);

		let is_unit = self.is_record_unit(dfn);
		let is_tuple = self.is_record_tuple(dfn);

		if is_unit && is_tuple {
			return Err(EmitError::TupleAndUnit(dfn.name.as_ref().clone(), None));
		}

		let mut derives = Vec::new();
		let mut attrs = Vec::new();
		self.process_record_ann(dfn, r, &mut derives, &mut attrs)?;
		let attrs = attrs.into_iter().collect::<TokenStream>();

		if is_unit {
			if !r.fields.is_empty() {
				return Err(EmitError::UnitWithFields(dfn.name.as_ref().clone(), None));
			}

			return Ok(quote! {
				#[allow(non_camel_case_types)]
				#[derive(#(#derives),*)]
				#attrs
				pub struct #rec_name #type_parameters;
			});
		}

		let fields = self.emit_fields(true, is_tuple, &r.fields)?;

		let struct_type =
			if is_tuple {
				quote! {
					#[derive(#(#derives),*)]
					#attrs
					pub struct #rec_name #type_parameters(#fields);
				}
			}
			else {
				quote! {
					#[derive(#(#derives),*)]
					#attrs
					pub struct #rec_name #type_parameters {
						#fields
					}
				}
			};

		Ok(struct_type)

	}

	fn process_record_ann(&self, dfn: &DefinitionInfo, r: &RecordDefinition, derives: &mut Vec<TokenStream>, attrs: &mut Vec<TokenStream>) -> Result<(), EmitError>  {
		derives.push(quote! { ::std::fmt::Debug });
		derives.push(quote! { ::std::clone::Clone });
		derives.push(quote! { ::std::cmp::PartialEq });

		for ann in &dfn.annotations {
			if ann.scope != "rust" {
				continue;
			}

			let Ok(ann) = RustAnnRecord::decode_esexpr(ann.value.clone()) else { continue; };

			match ann {
				RustAnnRecord::Derive(derive) => {
					derives.push(syn::parse_str(&derive)?);
				},

				_ => {}
			}
		}

		if let Some(esexpr_options) = &r.esexpr_options {
			derives.push(quote! { ::esexpr::ESExprCodec });

			let name = &esexpr_options.constructor;
			attrs.push(quote! { #[constructor = #name] });
		}

		Ok(())
	}

	fn emit_enum(&mut self, dfn: &'a DefinitionInfo, e: &'a EnumDefinition) -> Result<TokenStream, EmitError> {
		let enum_name = convert_id_pascal(dfn.name.name());

		let type_parameters = self.emit_type_parameters(&dfn.type_parameters);

		let cases: TokenStream = e.cases.iter().map(|c| self.emit_enum_case(dfn, c)).collect::<Result<_, _>>()?;

		let mut derives = Vec::new();
		self.process_enum_ann(dfn, e, &mut derives)?;

		Ok(quote! {
			#[allow(non_camel_case_types)]
			#[derive(#(#derives),*)]
			pub enum #enum_name #type_parameters {
				#cases
			}
		})
	}

	fn process_enum_ann(&self, dfn: &DefinitionInfo, e: &EnumDefinition, derives: &mut Vec<TokenStream>) -> Result<(), EmitError>  {
		derives.push(quote! { ::std::fmt::Debug });
		derives.push(quote! { ::std::clone::Clone });
		derives.push(quote! { ::std::cmp::PartialEq });

		for ann in &dfn.annotations {
			if ann.scope != "rust" {
				continue;
			}

			let Ok(ann) = RustAnnEnum::decode_esexpr(ann.value.clone()) else { continue; };

			match ann {
				RustAnnEnum::Derive(derive) => {
					derives.push(syn::parse_str(&derive)?);
				},
			}
		}

		if e.esexpr_options.is_some() {
			derives.push(quote! { ::esexpr::ESExprCodec });
		}

		Ok(())
	}

	fn emit_enum_case(&mut self, dfn: &'a DefinitionInfo, c: &'a EnumCase) -> Result<TokenStream, EmitError> {
		let case_name = convert_id_pascal(&c.name);

		let is_unit = self.is_enum_case_unit(c);
		let is_tuple = self.is_enum_case_tuple(c);

		if is_unit && is_tuple {
			return Err(EmitError::TupleAndUnit(dfn.name.as_ref().clone(), Some(c.name.clone())));
		}

		let mut attrs = Vec::new();
		self.process_enum_case_ann(c, &mut attrs)?;
		let attrs = attrs.into_iter().collect::<TokenStream>();

		if is_unit {
			if !c.fields.is_empty() {
				return Err(EmitError::UnitWithFields(dfn.name.as_ref().clone(), None));
			}

			return Ok(quote! {
				#attrs
				#case_name,
			})
		}

		let fields = self.emit_fields(false, is_tuple, &c.fields)?;

		if is_tuple {
			Ok(quote! {
				#attrs
				#case_name(#fields),
			})
		}
		else {
			Ok(quote! {
				#attrs
				#case_name {
					#fields
				},
			})
		}
	}

	fn process_enum_case_ann(&self, c: &EnumCase, attrs: &mut Vec<TokenStream>) -> Result<(), EmitError> {
		if let Some(esexpr_options) = &c.esexpr_options {
			match esexpr_options.case_type.as_ref() {
				EsexprEnumCaseType::Constructor(name) => attrs.push(quote! { #[constructor = #name] }),
				EsexprEnumCaseType::InlineValue => attrs.push(quote! { #[inline_value] }),
			}
		}

		Ok(())
	}

	fn emit_simple_enum(&mut self, dfn: &'a DefinitionInfo, e: &'a SimpleEnumDefinition) -> Result<TokenStream, EmitError> {
		let enum_name = convert_id_pascal(dfn.name.name());

		let cases: TokenStream = e.cases.iter().map(|c| {
			let id = convert_id_pascal(&c.name);

			if let Some(esexpr_options) = &c.esexpr_options {
				let name = esexpr_options.name.as_str();
				quote! { #[constructor = #name] #id, }
			}
			else {
				quote! { #id, }
			}

		}).collect();

		let mut derives = Vec::new();
		self.process_simple_enum_ann(dfn, e, &mut derives)?;

		Ok(quote! {
			#[allow(non_camel_case_types)]
			#[derive(#(#derives),*)]
			#[simple_enum]
			pub enum #enum_name {
				#cases
			}
		})
	}

	fn process_simple_enum_ann(&self, dfn: &DefinitionInfo, e: &SimpleEnumDefinition, derives: &mut Vec<TokenStream>) -> Result<(), EmitError>  {
		derives.push(quote! { ::std::fmt::Debug });
		derives.push(quote! { ::std::clone::Clone });
		derives.push(quote! { ::std::marker::Copy });
		derives.push(quote! { ::std::cmp::PartialEq });

		for ann in &dfn.annotations {
			if ann.scope != "rust" {
				continue;
			}

			let Ok(ann) = RustAnnEnum::decode_esexpr(ann.value.clone()) else { continue; };

			match ann {
				RustAnnEnum::Derive(derive) => {
					derives.push(syn::parse_str(&derive)?);
				},
			}
		}

		if e.esexpr_options.is_some() {
			derives.push(quote! { ::esexpr::ESExprCodec });
		}

		Ok(())
	}

	fn emit_interface(&self, dfn: &DefinitionInfo, i: &InterfaceDefinition) -> Result<TokenStream, EmitError> {
		let iface = InterfaceTraitInterfaceEmitter {
			type_emitter: DefaultTypeEmitter(self),
		}.emit_trait(dfn, i)?;
		let erased = InterfaceTraitErasedEmitter {
			type_emitter: ErasedValueTypeEmitter(self),
			trait_type_emitter: DefaultTypeEmitter(self),
		}.emit_trait(dfn, i)?;
		let erased_impl = InterfaceErasedImplEmitter {
			dfn,
			i,
			type_emitter: ErasedValueTypeEmitter(self),
			impl_type_emitter: DefaultTypeEmitter(self),
		}.emit_interface_erased_impl()?;

		let mapper = InterfaceMapperEmitter {
			mod_emitter: self,
			dfn,
			iface: i,
		}.emit_mapper_type()?.into_iter().map(|item| quote! { #item }).collect::<TokenStream>();

		let public_impl = InterfacePublicImplEmitter {
			dfn,
			i,
			type_emitter: DefaultTypeEmitter(self),
		}.emit_interface_public_impl()?;

		let if_name = convert_id_pascal(dfn.name.name());
		let if_name_erased = format_ident!("{}_Erased", if_name);

		let type_parameters = self.emit_type_parameters(&dfn.type_parameters);
		let type_args = self.emit_type_parameters_as_arguments(&dfn.type_parameters);

		Ok(quote! {
			#iface
			#erased
			#erased_impl
			#mapper

			pub struct #if_name #type_parameters {
				erased: ::std::sync::Arc<dyn #if_name_erased #type_args + ::std::marker::Send + ::std::marker::Sync + 'static>,
			}

			impl #type_parameters ::std::clone::Clone for #if_name #type_args {
				fn clone(&self) -> Self {
					#if_name {
						erased: self.erased.clone()
					}
				}
			}

			#public_impl
		})
	}

	fn emit_type_parameters(&self, type_params: &[Box<TypeParameter>]) -> syn::Generics {
		if type_params.is_empty() {
			syn::Generics {
				lt_token: None,
				params: Punctuated::new(),
				gt_token: None,
				where_clause: None,
			}
		}
		else {
			let params: Punctuated<syn::GenericParam, syn::Token![,]> = type_params
				.into_iter()
				.map(|param| match param.as_ref() {
					TypeParameter::Type { name: param, .. } => {
						syn::GenericParam::Type(syn::TypeParam::from(convert_id_pascal(param)))
					},
				})
				.collect();

			syn::Generics {
				lt_token: Some(Default::default()),
				params,
				gt_token: Some(Default::default()),
				where_clause: None,
			}
		}
	}

	fn emit_type_parameters_as_arguments(&self, type_params: &[Box<TypeParameter>]) -> syn::PathArguments {
		if type_params.is_empty() {
			syn::PathArguments::None
		}
		else {
			let args: Punctuated<syn::GenericArgument, syn::Token![,]> = type_params
				.into_iter()
				.map(|param| match param.as_ref() {
					TypeParameter::Type { name: param, .. } => {
						syn::GenericArgument::Type(syn::Type::Path(syn::TypePath {
							qself: None,
							path: syn::Path::from(convert_id_pascal(param)),
						}))
					},
				})
				.collect();

			syn::PathArguments::AngleBracketed(syn::AngleBracketedGenericArguments {
				colon2_token: None,
				lt_token: Default::default(),
				args,
				gt_token: Default::default(),
			})
		}
	}

	fn emit_fields(&mut self, use_pub: bool, is_tuple: bool, fields: &'a [Box<RecordField>]) -> Result<TokenStream, EmitError> {
		fields.iter().map(|f| self.emit_field(use_pub, is_tuple, f)).collect()
	}

	fn emit_field(&mut self, use_pub: bool, is_tuple: bool, field: &'a RecordField) -> Result<TokenStream, EmitError> {
		let field_name = convert_id_snake(&field.name);
		let field_type = self.emit_type_expr(&field.field_type)?;

		let mut attrs = Vec::new();
		self.process_field_ann(field, &mut attrs)?;
		let attrs = attrs.into_iter().collect::<TokenStream>();

		let pub_kw = if use_pub { quote! { pub } } else { quote!{} };

		if is_tuple {
			Ok(quote! {
				#attrs
				#pub_kw #field_type,
			})
		}
		else {
			Ok(quote! {
				#attrs
				#pub_kw #field_name: #field_type,
			})
		}
	}

	fn process_field_ann(&self, field: &RecordField, attrs: &mut Vec<TokenStream>) -> Result<(), EmitError> {
		if let Some(esexpr_options) = &field.esexpr_options {
			match esexpr_options.kind.as_ref() {
				EsexprRecordFieldKind::Positional(mode) => {
					match mode.as_ref() {
						EsexprRecordPositionalMode::Required => {},
						EsexprRecordPositionalMode::Optional(_) => {
							attrs.push(quote! { #[optional] });
						}
					}
				},
				EsexprRecordFieldKind::Keyword(name, mode) => {
					attrs.push(quote! { #[keyword = #name] });

					match mode.as_ref() {
						EsexprRecordKeywordMode::Required => {},

						EsexprRecordKeywordMode::DefaultValue(default_value) => {
							let value = self.emit_value(default_value)?;
							let value_str = value.into_token_stream().to_string();

							attrs.push(quote! { #[default_value = #value_str] });
						}

						EsexprRecordKeywordMode::Optional(_) => {
							attrs.push(quote! { #[optional] });
						}
					}
				},
				EsexprRecordFieldKind::Dict(_) => attrs.push(quote! { #[dict] }),
				EsexprRecordFieldKind::Vararg(_) => attrs.push(quote! { #[vararg] }),
			}
		}

		Ok(())
	}

	fn emit_exception_type(&self, dfn: &DefinitionInfo, ex: &ExceptionTypeDefinition) -> Result<TokenStream, EmitError> {
		let name = convert_id_pascal(dfn.name.name());
		let info = self.emit_type_expr(&ex.information)?;

		Ok(quote! {
			#[derive(::std::fmt::Debug, std::clone::Clone)]
			pub struct #name {
				pub information: #info,
				pub message: ::std::option::Option<::std::string::String>,
				pub backtrace: ::std::sync::Arc<::std::backtrace::Backtrace>,
				pub source: ::std::option::Option<::std::sync::Arc<dyn ::std::error::Error + ::std::marker::Sync + ::std::marker::Send + 'static>>,
			}

			impl ::std::fmt::Display for #name {
				fn fmt(&self, f: &mut ::std::fmt::Formatter<'_>) -> ::std::fmt::Result {
					if let ::std::option::Option::Some(msg) = &self.message {
						writeln!(f, "{}", msg)?;
					}
					writeln!(f, "{}", self.backtrace)?;

					::std::result::Result::Ok(())
				}
			}

			impl ::std::error::Error for #name {
				fn source(&self) -> ::std::option::Option<&(dyn ::std::error::Error + 'static)> {
					self.source.as_deref().map(|s| s as _)
				}
				fn description(&self) -> &::std::primitive::str {
					self.message.as_ref().map(::std::string::String::as_str).unwrap_or("")
				}
			}
		})
	}


	fn emit_type_expr(&self, t: &TypeExpr) -> Result<syn::Type, EmitError> {
		DefaultTypeEmitter(self).emit_type_expr(t)
	}

	fn emit_value(&self, value: &EsexprDecodedValue) -> Result<syn::Expr, EmitError> {
		match value {
			EsexprDecodedValue::Record { t, fields } => self.emit_record_value(t, fields),
			EsexprDecodedValue::Enum { t, case_name, fields } => self.emit_enum_value(t, case_name, fields),
			EsexprDecodedValue::Optional { t, element_type, value } => self.emit_optional_value(t, element_type, value.as_deref()),
			EsexprDecodedValue::Vararg { t, element_type, values } => self.emit_vararg_value(t, element_type, values),
			EsexprDecodedValue::Dict { t, element_type, values } => self.emit_dict_value(t, element_type, values),
			EsexprDecodedValue::BuildFrom { t, from_type, from_value } => self.emit_build_from(t, from_type, &**from_value),
			EsexprDecodedValue::FromBool { t, b } => self.emit_literal_primitive::<bool>(t, parse_quote!(::std::primitive::bool), *b),
			EsexprDecodedValue::FromInt { t, i, min_int, max_int } => self.emit_literal_int(t, i, min_int.as_ref(), max_int.as_ref()),
			EsexprDecodedValue::FromStr { t, s } => self.emit_literal_primitive::<&str>(t, parse_quote!(&'static ::std::primitive::str), s),
			EsexprDecodedValue::FromBinary { t, b } => self.emit_literal_binary(t, &b.0),
			EsexprDecodedValue::FromFloat32 { t, f } => self.emit_literal_primitive::<f32>(t, parse_quote!(::std::primitive::f32), *f),
			EsexprDecodedValue::FromFloat64 { t, f } => self.emit_literal_primitive::<f64>(t, parse_quote!(::std::primitive::f64), *f),
			EsexprDecodedValue::FromNull { t } => self.emit_literal_null(t),
		}
	}

	fn emit_record_value(&self, t: &TypeExpr, field_values: &[Box<EsexprDecodedFieldValue>]) -> Result<syn::Expr, EmitError> {
		let value = syn::Expr::Struct(syn::ExprStruct {
			attrs: vec![],
			qself: None,
			path: DefaultTypeEmitter(self).get_literal_type_path(t)?,
			brace_token: syn::token::Brace::default(),
			fields: self.emit_field_values(field_values)?.into_iter().collect(),
			dot2_token: None,
			rest: None,
		});

		Ok(parse_quote! {
			::std::boxed::Box::new(#value)
		})
	}

	fn emit_enum_value(&self, t: &TypeExpr, case_name: &str, field_values: &[Box<EsexprDecodedFieldValue>]) -> Result<syn::Expr, EmitError> {
		let mut path = DefaultTypeEmitter(self).get_literal_type_path(t)?;
		path.segments.push(syn::PathSegment {
			ident: convert_id_pascal(case_name),
			arguments: syn::PathArguments::None,
		});

		let value = syn::Expr::Struct(syn::ExprStruct {
			attrs: vec![],
			qself: None,
			path,
			brace_token: Default::default(),
			fields: self.emit_field_values(field_values)?.into_iter().collect(),
			dot2_token: None,
			rest: None,
		});

		Ok(parse_quote! {
			::std::boxed::Box::new(#value)
		})
	}

	fn emit_field_values(&self, field_values: &[Box<EsexprDecodedFieldValue>]) -> Result<Vec<syn::FieldValue>, EmitError> {
		field_values.iter().map(|fv| {
			let value = self.emit_value(&fv.value)?;
			Ok(syn::FieldValue {
				attrs: vec![],
				member: syn::Member::Named(convert_id_snake(&fv.name)),
				colon_token: Some(syn::token::Colon::default()),
				expr: value,
			})
		}).collect::<Result<Vec<_>, _>>()
	}

	fn emit_optional_value(&self, optional_type: &TypeExpr, element_type: &TypeExpr, value: Option<&EsexprDecodedValue>) -> Result<syn::Expr, EmitError> {
		let t = self.emit_type_expr(optional_type)?;
		let et = self.emit_type_expr(element_type)?;
		let v = value.map(|v| self.emit_value(v)).transpose()?;
		let v: syn::Expr = match v {
			Some(v) => parse_quote! { std::option::Option::Some(#v) },
			None => parse_quote! { ::std::option::Option::None },
		};

		Ok(parse_quote! {
			<#t as ::std::convert::From<::std::option::Option<#et>>>::from(#v)
		})
	}

	fn emit_vararg_value(&self, vararg_type: &TypeExpr, element_type: &TypeExpr, values: &[Box<EsexprDecodedValue>]) -> Result<syn::Expr, EmitError> {
		let t = self.emit_type_expr(vararg_type)?;
		let et = self.emit_type_expr(element_type)?;
		let v = values.iter().map(|v| self.emit_value(v)).collect::<Result<Vec<_>, _>>()?;
		let v: syn::Expr = parse_quote! { ::std::vec![#(#v),*] };



		Ok(parse_quote! {
			<#t as std::convert::From<std::vec::Vec<#et>>>::from(#v)
		})
	}

	fn emit_dict_value(&self, dict_type: &TypeExpr, element_type: &TypeExpr, values: &HashMap<String, Box<EsexprDecodedValue>>) -> Result<syn::Expr, EmitError> {
		let t = self.emit_type_expr(dict_type)?;
		let et = self.emit_type_expr(element_type)?;
		let v = values.iter().map(|(k, v)| {
			let v = self.emit_value(v)?;
			Ok(parse_quote! { (#k, #v) })
		}).collect::<Result<Vec<syn::Expr>, EmitError>>()?;

		let v: syn::Expr = parse_quote! { ::std::collection::HashMap::from([#(#v),*]) };

		Ok(parse_quote! {
			<#t as ::std::convert::From<::std::collection::HashMap<::std::string::String, #et>>>::from(#v)
		})
	}

	fn emit_build_from(&self, built_type: &TypeExpr, from_type: &TypeExpr, value: &EsexprDecodedValue) -> Result<syn::Expr, EmitError> {
		let t = self.emit_type_expr(built_type)?;
		let ft = self.emit_type_expr(from_type)?;
		let v = self.emit_value(value)?;

		Ok(parse_quote! {
			<#t as ::std::convert::From<#ft>>::from(#v)
		})
	}

	fn emit_literal_primitive<T: quote::ToTokens>(&self, t: &TypeExpr, prim_type: syn::Type, value: T) -> Result<syn::Expr, EmitError> {
		let t = self.emit_type_expr(t)?;

		Ok(parse_quote! {
			<#t as ::std::convert::From<#prim_type>>::from(#value)
		})
	}

	fn emit_literal_int(&self, t: &TypeExpr, value: &BigInt, min: Option<&BigInt>, max: Option<&BigInt>) -> Result<syn::Expr, EmitError> {
		let t = self.emit_type_expr(t)?;

		fn try_as<
			I: num_traits::bounds::Bounded + for<'a> TryFrom<&'a BigInt> + Copy + quote::ToTokens
		>(
			t: &syn::Type,
			prim_type: syn::Type,
			value: &BigInt,
			min: Option<&BigInt>,
			max: Option<&BigInt>
		) -> Option<syn::Expr> {
			// Ensure that min/max are within the range of this type.
			I::try_from(min?).ok()?;
			I::try_from(max?).ok()?;

			let value = I::try_from(value).ok()?;

			Some(parse_quote! { <#t as std::convert::From<#prim_type>>::from(#value) })
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
			try_as::<u8>(&t, parse_quote!(::std::primitive::u8), value, min, max)
				.or_else(|| try_as::<i8>(&t, parse_quote!(::std::primitive::i8), value, min, max))
				.or_else(|| try_as::<u16>(&t, parse_quote!(::std::primitive::u16), value, min, max))
				.or_else(|| try_as::<i16>(&t, parse_quote!(::std::primitive::i16), value, min, max))
				.or_else(|| try_as::<u32>(&t, parse_quote!(::std::primitive::u32), value, min, max))
				.or_else(|| try_as::<i32>(&t, parse_quote!(::std::primitive::i32), value, min, max))
				.or_else(|| try_as::<u64>(&t, parse_quote!(::std::primitive::u64), value, min, max))
				.or_else(|| try_as::<i64>(&t, parse_quote!(::std::primitive::i64), value, min, max))
				.or_else(|| try_as::<u128>(&t, parse_quote!(::std::primitive::u128), value, min, max))
				.or_else(|| try_as::<i128>(&t, parse_quote!(::std::primitive::i128), value, min, max))
				.or_else(|| try_as_nat())
				.unwrap_or_else(|| as_bigint())
		)
	}

	fn emit_literal_binary(&self, t: &TypeExpr, value: &[u8]) -> Result<syn::Expr, EmitError> {
		let t = self.emit_type_expr(t)?;

		let literal = syn::Lit::ByteStr(syn::LitByteStr::new(&value, proc_macro2::Span::mixed_site()));

		Ok(parse_quote! {
			<#t as std::convert::From<&[::std::primitive::u8]>>::from(#literal)
		})
	}

	fn emit_literal_null(&self, t: &TypeExpr) -> Result<syn::Expr, EmitError> {
		let t = self.emit_type_expr(t)?;

		Ok(parse_quote! {
			<#t as ::std::default::Default>::default()
		})
	}


	fn is_record_unit(&self, dfn: &DefinitionInfo) -> bool {
		dfn.annotations.iter()
			.filter(|ann| ann.scope == "rust")
			.filter_map(|ann| RustAnnRecord::decode_esexpr(ann.value.clone()).ok())
			.any(|ann| match ann {
				RustAnnRecord::Unit => true,
				_ => false,
			})
	}

	fn is_record_tuple(&self, dfn: &DefinitionInfo) -> bool {
		dfn.annotations.iter()
			.filter(|ann| ann.scope == "rust")
			.filter_map(|ann| RustAnnRecord::decode_esexpr(ann.value.clone()).ok())
			.any(|ann| match ann {
				RustAnnRecord::Tuple => true,
				_ => false,
			})
	}

	fn is_enum_case_unit(&self, c: &EnumCase) -> bool {
		c.annotations.iter()
			.filter(|ann| ann.scope == "rust")
			.filter_map(|ann| RustAnnEnumCase::decode_esexpr(ann.value.clone()).ok())
			.any(|ann| match ann {
				RustAnnEnumCase::Unit => true,
				_ => false,
			})
	}

	fn is_enum_case_tuple(&self, c: &EnumCase) -> bool {
		c.annotations.iter()
			.filter(|ann| ann.scope == "rust")
			.filter_map(|ann| RustAnnEnumCase::decode_esexpr(ann.value.clone()).ok())
			.any(|ann| match ann {
				RustAnnEnumCase::Tuple => true,
				_ => false,
			})
	}

}


enum TypeBoxing {
	None,
	Box,
}


trait TypeEmitter<'a> {
	fn mod_emitter(&self) -> &ModEmitter<'a>;



	fn emit_type_expr(&self, t: &TypeExpr) -> Result<syn::Type, EmitError> {
		let path = self.get_type_path(t)?;

		let boxing = self.get_type_boxing(t);

		Ok(
			match boxing {
				TypeBoxing::None => {
					syn::Type::Path(syn::TypePath {
						qself: None,
						path,
					})
				},

				TypeBoxing::Box => {
					parse_quote! { ::std::boxed::Box<#path> }
				},
			}
		)
	}

	fn emit_return_type(&self, t: &TypeExpr, throws: Option<&TypeExpr>) -> Result<syn::Type, EmitError> {
		if let Some(throws) = throws {
			let t = self.emit_type_expr(t)?;
			let throws = self.emit_type_expr(throws)?;

			Ok(parse_quote! {
				::std::result::Result<#t, #throws>
			})
		}
		else {
			self.emit_type_expr(t)
		}
	}


	fn get_type_boxing(&self, t: &TypeExpr) -> TypeBoxing {
		match t {
			TypeExpr::DefinedType(name, _) => {
				self.mod_emitter().definition_map.get(name.as_ref())
					.map(|dfn| match dfn.definition.as_ref() {
						Definition::Record(_) | Definition::Enum(_) | Definition::ExceptionType(_) => TypeBoxing::Box,
						Definition::SimpleEnum(_) | Definition::ExternType(_) | Definition::Interface(_) => TypeBoxing::None,
					})
					.unwrap_or(TypeBoxing::None)
			}

			_ => TypeBoxing::None,
		}
	}


	fn get_type_path(&self, t: &TypeExpr) -> Result<syn::Path, EmitError> {
		Ok(match t {
			TypeExpr::DefinedType(name, args) => {

				let mut path = self.get_package_name_path_segments(name.package_name())?;

				let arguments = self.emit_type_arguments(args)?;

				path.segments.push(syn::PathSegment {
					ident: convert_id_pascal(name.name()),
					arguments,
				});

				path
			},

			TypeExpr::TypeParameter { name, owner: TypeParameterOwner::ByMethod } =>
				self.get_method_type_parameter_path(name)?,

			TypeExpr::TypeParameter { name, .. } =>
				self.get_type_type_parameter_path(name)?,
		})
	}

	fn get_method_type_parameter_path(&self, name: &str) -> Result<syn::Path, EmitError> {
		Ok(syn::Path::from(convert_id_pascal(&name)))
	}

	fn get_type_type_parameter_path(&self, name: &str) -> Result<syn::Path, EmitError> {
		Ok(syn::Path::from(convert_id_pascal(&name)))
	}


	fn get_literal_type_path(&self, t: &TypeExpr) -> Result<syn::Path, EmitError> {
		let mut path = self.get_type_path(t)?;
		match path.segments.last_mut() {
			Some(syn::PathSegment { arguments: syn::PathArguments::AngleBracketed(args), .. }) => {
				args.colon2_token = Some(Default::default());
			},

			_ => {},
		}
		Ok(path)
	}


	fn get_package_name_path_segments(&self, package_name: &PackageName) -> Result<syn::Path, EmitError> {
		let rust_mod = self.mod_emitter().get_rust_module(package_name)?;
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

		Ok(syn::Path {
			leading_colon: if rust_mod.crate_name.is_some() { Some(Default::default()) } else { None },
			segments,
		})
	}

	fn emit_type_arguments_of(&self, t: &TypeExpr) -> Result<syn::PathArguments, EmitError> {
		match t {
			TypeExpr::DefinedType(_, args) => self.emit_type_arguments(args),
			TypeExpr::TypeParameter { .. } => Ok(syn::PathArguments::None),
		}
	}

	fn emit_type_arguments(&self, args: &[Box<TypeExpr>]) -> Result<syn::PathArguments, EmitError> {
		if args.is_empty() {
			Ok(syn::PathArguments::None)
		}
		else {
			let args = args.iter()
				.map(|a| Ok(syn::GenericArgument::Type(self.emit_type_expr(a)?)))
				.collect::<Result<Punctuated<_, _>, EmitError>>()?;

			Ok(syn::PathArguments::AngleBracketed(syn::AngleBracketedGenericArguments {
				colon2_token: None,
				lt_token: Default::default(),
				args,
				gt_token: Default::default(),
			}))
		}
	}


	fn emit_type_parameters(&self, type_params: &[Box<TypeParameter>]) -> syn::Generics {
		if type_params.is_empty() {
			syn::Generics {
				lt_token: None,
				params: Punctuated::new(),
				gt_token: None,
				where_clause: None,
			}
		}
		else {
			let params: Punctuated<syn::GenericParam, syn::Token![,]> = type_params
				.into_iter()
				.map(|param| self.emit_type_parameter(param.as_ref()))
				.collect();

			syn::Generics {
				lt_token: Some(Default::default()),
				params,
				gt_token: Some(Default::default()),
				where_clause: None,
			}
		}
	}

	fn emit_type_parameters_bounded(&self, type_params: &[Box<TypeParameter>]) -> syn::Generics {
		let mut generics = self.emit_type_parameters(type_params);
		for tp in &mut generics.params {
			match tp {
				syn::GenericParam::Type(tp) => {
					tp.bounds.push(parse_quote! { ::std::clone::Clone });
					tp.bounds.push(parse_quote! { ::std::marker::Send });
					tp.bounds.push(parse_quote! { ::std::marker::Sync });
					tp.bounds.push(parse_quote! { 'static });
				},
				_ => {},
			}
		}

		generics
	}

	fn emit_type_parameter(&self, tp: &TypeParameter) -> syn::GenericParam {
		match tp {
			TypeParameter::Type { name: param, .. } => {
				syn::GenericParam::Type(syn::TypeParam::from(convert_id_pascal(param)))
			}
		}
	}






	fn get_interface_trait(&self, dfn: &DefinitionInfo) -> Result<syn::Path, EmitError> {
		let if_trait_name = format_ident!("{}_Interface", convert_id_pascal(dfn.name.name()));
		let type_args =  self.emit_type_arguments_of(&dfn_as_type(dfn))?;
		let if_trait = syn::Path::from(syn::PathSegment {
			ident: if_trait_name,
			arguments: type_args.clone(),
		});
		Ok(if_trait)
	}

	fn get_erased_trait(&self, dfn: &DefinitionInfo) -> Result<syn::Path, EmitError> {
		let if_trait_name = format_ident!("{}_Erased", convert_id_pascal(dfn.name.name()));
		let type_args = self.emit_type_arguments_of(&dfn_as_type(dfn))?;
		let if_trait = syn::Path::from(syn::PathSegment {
			ident: if_trait_name,
			arguments: type_args.clone(),
		});
		Ok(if_trait)
	}

}

#[derive(Clone, Copy)]
struct DefaultTypeEmitter<'a>(&'a ModEmitter<'a>);

impl <'a> TypeEmitter<'a> for DefaultTypeEmitter<'a> {
	fn mod_emitter(&self) -> &ModEmitter<'a> {
		&self.0
	}
}


struct ErasedValueTypeEmitter<'a>(&'a ModEmitter<'a>);

impl <'a> TypeEmitter<'a> for ErasedValueTypeEmitter<'a> {
	fn mod_emitter(&self) -> &ModEmitter<'a> {
		&self.0
	}

	fn get_method_type_parameter_path(&self, _name: &str) -> Result<syn::Path, EmitError> {
		Ok(parse_quote! { ::noble_idl_runtime::erasure::ErasedValue })
	}

	fn emit_type_parameter(&self, tp: &TypeParameter) -> syn::GenericParam {
		match tp {
			TypeParameter::Type { name: param, .. } => {
				syn::GenericParam::Lifetime(
					syn::LifetimeParam {
						attrs: vec![],
						lifetime: syn::Lifetime::new(&format!("'{}", &convert_id_snake_str(param)), Span::mixed_site()),
						colon_token: None,
						bounds: Punctuated::new(),
					}
				)
			}
		}
	}
}


trait MethodEmitter<'a> {
	type TE: TypeEmitter<'a>;
	fn type_emitter(&self) -> &Self::TE;
	fn self_arg(&self) -> Result<syn::FnArg, EmitError>;
	fn emit_method_body<'b: 'a>(&self, m: &'b InterfaceMethod) -> Result<Option<Block>, EmitError>;
	fn method_vis(&self) -> syn::Visibility {
		syn::Visibility::Inherited
	}


	fn emit_trait_method<'b: 'a>(&self, m: &'b InterfaceMethod) -> Result<syn::TraitItemFn, EmitError> {
		let method_name = convert_id_snake(&m.name);
		let type_params = self.type_emitter().emit_type_parameters_bounded(&m.type_parameters);
		let params = self.emit_method_parameters(&m.parameters)?;
		let return_type = self.type_emitter().emit_return_type(&m.return_type, m.throws.as_deref())?;
		let body = self.emit_method_body(m)?;

		Ok(syn::TraitItemFn {
			attrs: vec![],
			sig: syn::Signature {
				constness: None,
				asyncness: None,
				unsafety: None,
				abi: None,
				fn_token: Default::default(),
				ident: method_name,
				generics: type_params,
				paren_token: Default::default(),
				inputs: params,
				variadic: None,
				output: syn::ReturnType::Type(Default::default(), Box::new(return_type)),
			},
			default: body,
			semi_token: Some(Default::default()),
		})
	}

	fn emit_impl_method<'b: 'a>(&self, m: &'b InterfaceMethod) -> Result<syn::ImplItemFn, EmitError> {
		let method_name = convert_id_snake(&m.name);
		let type_params = self.type_emitter().emit_type_parameters_bounded(&m.type_parameters);
		let params = self.emit_method_parameters(&m.parameters)?;
		let return_type = self.type_emitter().emit_return_type(&m.return_type, m.throws.as_deref())?;
		let body = self.emit_method_body(m)?;

		Ok(syn::ImplItemFn {
			attrs: vec![],
			sig: syn::Signature {
				constness: None,
				asyncness: None,
				unsafety: None,
				abi: None,
				fn_token: Default::default(),
				ident: method_name,
				generics: type_params,
				paren_token: Default::default(),
				inputs: params,
				variadic: None,
				output: syn::ReturnType::Type(Default::default(), Box::new(return_type)),
			},
			block: body.unwrap(),
			vis: self.method_vis(),
			defaultness: None,
		})
	}

	fn get_parameter_name(&self, name: &str) -> syn::Ident {
		convert_id_snake(name)
	}

	fn emit_method_parameters(&self, parameters: &[Box<InterfaceMethodParameter>]) -> Result<Punctuated<syn::FnArg, syn::Token![,]>, EmitError> {
		let mut args = Punctuated::new();

		args.push(self.self_arg()?);

		for param in parameters {
			let param_name = self.get_parameter_name(&param.name);
			let param_type = self.type_emitter().emit_type_expr(&param.parameter_type)?;

			args.push(syn::FnArg::Typed(syn::PatType {
				attrs: vec![],
				pat: Box::new(syn::Pat::Ident(syn::PatIdent {
					attrs: vec![],
					by_ref: None,
					mutability: None,
					ident: param_name,
					subpat: None,
				})),
				colon_token: Default::default(),
				ty: Box::new(param_type),
			}));
		}

		Ok(args)
	}
}

trait TraitEmitter<'a>: MethodEmitter<'a> {
	type TraitTE: TypeEmitter<'a>;
	fn trait_name_suffix(&self) -> &'static str;
	fn trait_vis(&self) -> syn::Visibility;
	fn trait_type_emitter(&self) -> &Self::TraitTE;

	fn emit_trait<'b: 'a>(&self, dfn: &'b DefinitionInfo, i: &'b InterfaceDefinition) -> Result<syn::Item, EmitError> {
		let if_name = format_ident!("{}{}", convert_id_pascal(dfn.name.name()), self.trait_name_suffix());

		let type_parameters = self.trait_type_emitter().emit_type_parameters(&dfn.type_parameters);

		let methods: Vec<syn::TraitItem> = i.methods
			.iter()
			.map(|m| self.emit_trait_method(m).map(syn::TraitItem::Fn))
			.collect::<Result<_, _>>()?;

		Ok(syn::Item::Trait(syn::ItemTrait {
			attrs: vec![
				parse_quote! {
					#[allow(non_camel_case_types)]
				}
			],

			vis: self.trait_vis(),
			unsafety: None,
			auto_token: None,
			restriction: None,
			trait_token: Default::default(),
			ident: if_name,
			generics: type_parameters,
			colon_token: None,
			supertraits: Punctuated::new(),
			brace_token: Default::default(),
			items: methods,
		}))
	}
}


struct InterfaceTraitInterfaceEmitter<'a> {
	type_emitter: DefaultTypeEmitter<'a>,
}

impl <'a> TraitEmitter<'a> for InterfaceTraitInterfaceEmitter<'a> {
	type TraitTE = Self::TE;
	fn trait_name_suffix(&self) -> &'static str {
		"_Interface"
	}

	fn trait_vis(&self) -> syn::Visibility {
		syn::Visibility::Public(Default::default())
	}

	fn trait_type_emitter(&self) -> &Self::TraitTE {
		self.type_emitter()
	}
}

impl <'a> MethodEmitter<'a> for InterfaceTraitInterfaceEmitter<'a> {
	type TE = DefaultTypeEmitter<'a>;
	fn type_emitter(&self) -> &Self::TE {
		&self.type_emitter
	}

	fn self_arg(&self) -> Result<syn::FnArg, EmitError> {
		Ok(parse_quote! { self: ::std::sync::Arc<Self> })
	}

	fn emit_method_body<'b: 'a>(&self, _m: &'b InterfaceMethod) -> Result<Option<Block>, EmitError> {
		Ok(None)
	}
}


struct InterfaceTraitErasedEmitter<'a> {
	type_emitter: ErasedValueTypeEmitter<'a>,
	trait_type_emitter: DefaultTypeEmitter<'a>,
}

impl <'a> TraitEmitter<'a> for InterfaceTraitErasedEmitter<'a> {
	type TraitTE = DefaultTypeEmitter<'a>;
	fn trait_name_suffix(&self) -> &'static str {
		"_Erased"
	}

	fn trait_vis(&self) -> syn::Visibility {
		syn::Visibility::Inherited
	}

	fn trait_type_emitter(&self) -> &Self::TraitTE {
		&self.trait_type_emitter
	}
}

impl <'a> MethodEmitter<'a> for InterfaceTraitErasedEmitter<'a> {
	type TE = ErasedValueTypeEmitter<'a>;
	fn type_emitter(&self) -> &Self::TE {
		&self.type_emitter
	}

	fn self_arg(&self) -> Result<syn::FnArg, EmitError> {
		Ok(parse_quote! { self: ::std::sync::Arc<Self> })
	}

	fn emit_method_body<'b: 'a>(&self, _m: &'b InterfaceMethod) -> Result<Option<Block>, EmitError> {
		Ok(None)
	}
}


struct InterfaceErasedImplEmitter<'a> {
	dfn: &'a DefinitionInfo,
	i: &'a InterfaceDefinition,
	type_emitter: ErasedValueTypeEmitter<'a>,
	impl_type_emitter: DefaultTypeEmitter<'a>,
}

impl <'a> InterfaceErasedImplEmitter<'a> {
	fn emit_interface_erased_impl(&self) -> Result<syn::ItemImpl, EmitError> {
		let dfn = self.dfn;
		let i = self.i;

		let tp_name = format_ident!("{}_Type", convert_id_pascal(dfn.name.name()));

		let if_trait = self.impl_type_emitter.get_interface_trait(dfn)?;
		let erased_trait = self.impl_type_emitter.get_erased_trait(dfn)?;

		let mut type_parameters = self.impl_type_emitter.emit_type_parameters(&dfn.type_parameters);

		type_parameters.params.push(parse_quote! {
			#[allow(non_camel_case_types)]
			#tp_name: #if_trait
		});



		let methods: Vec<syn::ImplItem> = i.methods
			.iter()
			.map(|m| self.emit_impl_method(m).map(syn::ImplItem::Fn))
			.collect::<Result<_, _>>()?;


		Ok(syn::ItemImpl {
			attrs: vec![],
			defaultness: None,
			unsafety: None,
			impl_token: Default::default(),
			generics: type_parameters,
			trait_: Some((
				None,
				erased_trait,
				Default::default(),
			)),
			self_ty: Box::new(syn::Type::Path(syn::TypePath {
				qself: None,
				path: syn::Path::from(tp_name),
			})),
			brace_token: Default::default(),
			items: methods,
		})
	}
}

impl <'a> MethodEmitter<'a> for InterfaceErasedImplEmitter<'a> {
	type TE = ErasedValueTypeEmitter<'a>;

	fn type_emitter(&self) -> &Self::TE {
		&self.type_emitter
	}

	fn self_arg(&self) -> Result<syn::FnArg, EmitError> {
		Ok(parse_quote! { self: ::std::sync::Arc<Self> })
	}

	fn emit_method_body<'b: 'a>(&self, m: &'b InterfaceMethod) -> Result<Option<Block>, EmitError> {
		let method_name = convert_id_snake(&m.name);

		let impl_expr = syn::Expr::Call(syn::ExprCall {
			attrs: vec![],
			func: Box::new(syn::Expr::Path(syn::ExprPath {
				attrs: vec![],
				qself: Some(syn::QSelf {
					lt_token: Default::default(),
					ty: Box::new(syn::Type::Path(syn::TypePath {
						qself: None,
						path: syn::Path::from(syn::Ident::new("Self", Span::mixed_site())),
					})),
					position: 1,
					as_token: Some(Default::default()),
					gt_token: Default::default(),
				}),
				path: {
					let mut path = self.impl_type_emitter.get_interface_trait(self.dfn)?;
					path.segments.push(syn::PathSegment {
						ident: method_name.clone(),
						arguments:
							if m.type_parameters.is_empty() {
								syn::PathArguments::None
							}
							else {
								syn::PathArguments::AngleBracketed(syn::AngleBracketedGenericArguments {
									colon2_token: Some(Default::default()),
									lt_token: Default::default(),
									args: m.type_parameters.iter()
										.map(|tp| {
											let t = TypeExpr::TypeParameter { name: tp.name().to_owned(), owner: TypeParameterOwner::ByMethod };
											self.type_emitter.emit_type_expr(&t)
												.map(syn::GenericArgument::Type)
										})
										.collect::<Result<_, _>>()?,
									gt_token: Default::default(),
								})
							},
					});
					path
				},
			})),
			paren_token: Default::default(),
			args:
				vec![
					syn::Expr::Path(syn::ExprPath {
						attrs: vec![],
						qself: None,
						path: syn::Path::from(syn::PathSegment {
							ident: syn::Ident::new("self", Span::call_site()),
							arguments: syn::PathArguments::None,
						}),
					})
				].into_iter().chain(
					m.parameters.iter()
					.map(|param| {
						syn::Expr::Path(syn::ExprPath {
							attrs: vec![],
							qself: None,
							path: syn::Path::from(convert_id_snake(&param.name)),
						})
					})
				)
				.collect(),

		});

		Ok(Some(syn::Block {
			brace_token: Default::default(),
			stmts: vec![
				syn::Stmt::Expr(impl_expr, None),
			],
		}))
	}
}


struct InterfacePublicImplEmitter<'a> {
	dfn: &'a DefinitionInfo,
	i: &'a InterfaceDefinition,
	type_emitter: DefaultTypeEmitter<'a>,
}

impl <'a> InterfacePublicImplEmitter<'a> {
	fn emit_interface_public_impl(&self) -> Result<syn::ItemImpl, EmitError> {
		let dfn = self.dfn;
		let i = self.i;

		let type_parameters = self.type_emitter.emit_type_parameters_bounded(&dfn.type_parameters);

		let public_struct = self.type_emitter.get_type_path(&dfn_as_type(dfn))?;

		let interface_type = self.type_emitter.get_interface_trait(dfn)?;

		let mut methods = Vec::new();
		methods.push(parse_quote! {
			pub fn new(instance: impl #interface_type + ::std::marker::Send + ::std::marker::Sync + 'static) -> Self {
				Self {
					erased: ::std::sync::Arc::new(instance)
				}
			}
		});

		methods.extend(
			i.methods.iter()
				.map(|m| self.emit_impl_method(m).map(syn::ImplItem::Fn))
				.collect::<Result<Vec<_>, _>>()?
		);

		Ok(syn::ItemImpl {
			attrs: vec![],
			defaultness: None,
			unsafety: None,
			impl_token: Default::default(),
			generics: type_parameters,
			trait_: None,
			self_ty: Box::new(syn::Type::Path(syn::TypePath {
				qself: None,
				path: public_struct,
			})),
			brace_token: Default::default(),
			items: methods,
		})
	}
}

impl <'a> MethodEmitter<'a> for InterfacePublicImplEmitter<'a> {
	type TE = DefaultTypeEmitter<'a>;

	fn type_emitter(&self) -> &Self::TE {
		&self.type_emitter
	}

	fn self_arg(&self) -> Result<syn::FnArg, EmitError> {
		Ok(parse_quote! { self })
	}

	fn method_vis(&self) -> syn::Visibility {
		syn::Visibility::Public(Default::default())
	}

	fn emit_method_body<'b: 'a>(&self, m: &'b InterfaceMethod) -> Result<Option<Block>, EmitError> {
		let method_name = convert_id_snake(&m.name);

		let mut body = Vec::new();

		let original_args: Vec<syn::Ident> = m.parameters.iter().map(|p| convert_id_snake(&p.name)).collect();

		match &original_args[..] {
			[] => {},
			[original_arg] => {
				let prefixed_arg = format_ident!("arg_{}", original_arg);

				let rebind_arg: syn::Stmt = parse_quote! {
					let #prefixed_arg = #original_arg;
				};

				body.push(rebind_arg);
			},
			_ => {

				let prefixed_args: Vec<syn::Ident> = original_args.iter()
				.map(|ident| format_ident!("arg_{}", ident))
				.collect();

				let rebind_args: syn::Stmt = parse_quote! {
					let (#(#prefixed_args),*) = (#(#original_args),*);
				};

				body.push(rebind_args);
			}
		}




		let write_mapper_usage = |t, name: &str, value: syn::Expr| -> Result<syn::Expr, EmitError> {
			let mapper = TypeMapperExprEmitter {
				type_emitter: self.type_emitter,
			}.emit_type_mapper_expr(t)?;
			let name = format_ident!("{}", name);
			let call_expr = syn::Expr::Call(syn::ExprCall {
				attrs: vec![],
				func: Box::new(syn::Expr::Path(syn::ExprPath {
					attrs: vec![],
					qself: None,
					path: syn::Path {
						leading_colon: Some(Default::default()),
						segments: [
							syn::PathSegment {
								ident: syn::Ident::new("noble_idl_runtime", Span::mixed_site()),
								arguments: syn::PathArguments::None,
							},
							syn::PathSegment {
								ident: syn::Ident::new("ValueMapper", Span::mixed_site()),
								arguments: syn::PathArguments::None,
							},
							syn::PathSegment {
								ident: name,
								arguments: syn::PathArguments::None,
							},
						].into_iter().collect(),
					},
				})),
				paren_token: Default::default(),
				args: [
					syn::Expr::Reference(syn::ExprReference {
						attrs: vec![],
						and_token: Default::default(),
						mutability: None,
						expr: Box::new(mapper),
					}),
					value
				].into_iter().collect(),
			});

			Ok(call_expr)
		};


		let call_expr = syn::Expr::Call(syn::ExprCall {
			attrs: vec![],
			func: Box::new(syn::Expr::Path(syn::ExprPath {
				attrs: vec![],
				qself: None,
				path: {
					let mut path = self.type_emitter.get_erased_trait(self.dfn)?;

					match path.segments.last_mut() {
						Some(syn::PathSegment { arguments: syn::PathArguments::AngleBracketed(args), .. }) => {
							args.colon2_token = Some(Default::default());
						}
						_ => {},
					}

					path.segments.push(syn::PathSegment {
						ident: method_name.clone(),
						arguments: syn::PathArguments::None,
					});
					path
				},
			})),
			paren_token: Default::default(),
			args:
				vec![
					{
						Ok(parse_quote! { ::std::clone::Clone::clone(&self.erased) })
					},
				].into_iter().chain(
					m.parameters.iter()
					.map(|param| {
						write_mapper_usage(
							&param.parameter_type,
							"map",
							syn::Expr::Path(syn::ExprPath {
								attrs: vec![],
								qself: None,
								path: syn::Path::from(format_ident!("arg_{}", convert_id_snake(&param.name))),
							}),
						)
					})
				)
				.collect::<Result<_, _>>()?,
		});

		let impl_expr =
			if let Some(throws_type) = m.throws.as_deref() {
				let map_res = write_mapper_usage(m.return_type.as_ref(), "unmap", parse_quote! { x })?;
				let map_err = write_mapper_usage(throws_type, "unmap", parse_quote! { e })?;

				parse_quote! { (#call_expr).map(|x| #map_res).map_err(|e| #map_err) }
			}
			else {
				write_mapper_usage(m.return_type.as_ref(), "unmap", call_expr)?
			};

		body.push(syn::Stmt::Expr(syn::Expr::Unsafe(syn::ExprUnsafe {
			attrs: vec![],
			unsafe_token: Default::default(),
			block: syn::Block {
				brace_token: Default::default(),
				stmts: vec![ syn::Stmt::Expr(impl_expr, None) ],
			}
		}), None));

		Ok(Some(syn::Block {
			brace_token: Default::default(),
			stmts: body,
		}))
	}
}


struct TypeMapperExprEmitter<TE> {
	type_emitter: TE,
}

impl <'a, TE: TypeEmitter<'a> + Clone> TypeMapperExprEmitter<TE> {

	fn emit_type_mapper_expr(&self, t: &TypeExpr) -> Result<syn::Expr, EmitError> {
		if !self.contains_method_type_parameter(t) {
			let t = self.type_emitter.emit_type_expr(t)?;
			return Ok(parse_quote! { ::noble_idl_runtime::IdentityMapper::<#t>::new() });
		}

		match t {
			TypeExpr::DefinedType(name, args) if !args.is_empty() => {
				let mut path = self.type_emitter.get_package_name_path_segments(name.package_name())?;

				path.segments.push(syn::PathSegment {
					ident: format_ident!("{}_Mapper", convert_id_pascal(name.name())),
					arguments: syn::PathArguments::None,
				});

				Ok(syn::Expr::Call(syn::ExprCall {
					attrs: vec![],
					func: Box::new(syn::Expr::Path(syn::ExprPath {
						attrs: vec![],
						qself: None,
						path,
					})),
					paren_token: Default::default(),
					args: args.iter().map(|arg| self.emit_type_mapper_expr(arg)).collect::<Result<_, _>>()?,
				}))
			},
			TypeExpr::TypeParameter { name, owner: TypeParameterOwner::ByMethod } => {
				let t = convert_id_pascal(name);
				Ok(parse_quote! { ::noble_idl_runtime::ErasedMapper::for_eraser(::noble_idl_runtime::erasure::make_eraser::<#t>()) })
			},
			_ => {
				let t = self.type_emitter.emit_type_expr(t)?;
				Ok(parse_quote! { ::noble_idl_runtime::IdentityMapper::<#t>::new() })
			},
		}
	}

	fn contains_method_type_parameter(&self, t: &TypeExpr) -> bool {
		match t {
			TypeExpr::DefinedType(_, args) => args.iter().any(|arg| self.contains_method_type_parameter(&*arg)),
			TypeExpr::TypeParameter { owner, .. } => *owner == TypeParameterOwner::ByMethod,
		}
	}
}


#[derive(Clone, Copy)]
struct TypeMapperImplExprEmitter<TE> {
	type_emitter: TE,
}


impl <'a, TE: TypeEmitter<'a> + Clone> TypeMapperImplExprEmitter<TE> {

	fn emit_type_mapper_expr_for_impl(&self, t: &TypeExpr) -> Result<syn::Expr, EmitError> {
		match t {
			TypeExpr::DefinedType(name, args) if !args.is_empty() => {
				let mut path = self.type_emitter.get_package_name_path_segments(name.package_name())?;

				path.segments.push(syn::PathSegment {
					ident: format_ident!("{}_Mapper", convert_id_pascal(name.name())),
					arguments: syn::PathArguments::None,
				});

				Ok(syn::Expr::Call(syn::ExprCall {
					attrs: vec![],
					func: Box::new(syn::Expr::Path(syn::ExprPath {
						attrs: vec![],
						qself: None,
						path,
					})),
					paren_token: Default::default(),
					args: args.iter().map(|arg| self.emit_type_mapper_expr_for_impl(arg)).collect::<Result<_, _>>()?,
				}))
			},
			TypeExpr::TypeParameter { name, owner: TypeParameterOwner::ByType } => {
				let t = format_ident!("mapper_{}", convert_id_snake(name));
				Ok(parse_quote! { self.#t })
			},
			_ => {
				let t = self.type_emitter.emit_type_expr(t)?;
				Ok(parse_quote! { ::noble_idl_runtime::IdentityMapper::<#t>::new() })
			},
		}
	}

}



struct InterfaceMapperEmitter<'a> {
	mod_emitter: &'a ModEmitter<'a>,
	dfn: &'a DefinitionInfo,
	iface: &'a InterfaceDefinition,
}

impl <'a> InterfaceMapperEmitter<'a> {
	fn emit_mapper_type(&self) -> Result<Vec<syn::Item>, EmitError> {
		let dfn = self.dfn;

		if dfn.type_parameters.is_empty() {
			return Ok(vec![]);
		}

		let dfn_name = convert_id_pascal(dfn.name.name());

		let mapped_name = format_ident!("{}_Mapper", dfn_name);

		let type_parameters = self.type_parameters();

		let type_args = syn::PathArguments::AngleBracketed(syn::AngleBracketedGenericArguments {
			colon2_token: None,
			lt_token: Default::default(),
			args: dfn.type_parameters.iter()
				.map(|tp| syn::GenericArgument::Type(
					syn::Type::Path(syn::TypePath {
						qself: None,
						path: syn::Path::from(format_ident!("{}_Mapper", convert_id_pascal(tp.name()))),
					})
				))
				.collect(),
			gt_token: Default::default(),
		});

		let fields = syn::Fields::Unnamed(syn::FieldsUnnamed {
			paren_token: Default::default(),
			unnamed: dfn.type_parameters.iter()
			.map(|tp| syn::Field {
				attrs: vec![],
				vis: syn::Visibility::Public(Default::default()),
				mutability: syn::FieldMutability::None,
				ident: None,
				colon_token: None,
				ty: syn::Type::Path(syn::TypePath {
					qself: None,
					path: syn::Path::from(format_ident!("{}_Mapper", convert_id_pascal(tp.name()))),
				}),
			})
			.collect(),
		});

		let conv_type = |name: &str| -> syn::ImplItem {
			let name = syn::Ident::new(name, Span::mixed_site());
			let args = dfn.type_parameters.iter()
				.map(|tp| syn::GenericArgument::Type(
					syn::Type::Path(syn::TypePath {
						qself: None,
						path: syn::Path {
							leading_colon: None,
							segments: [
								syn::PathSegment {
									ident: format_ident!("{}_Mapper", convert_id_pascal(tp.name())),
									arguments: syn::PathArguments::None,
								},
								syn::PathSegment {
									ident: name.clone(),
									arguments: syn::PathArguments::None,
								},
							].into_iter().collect()
						},
					})
				))
				.collect::<Vec<_>>();

			parse_quote! {
				type #name = #dfn_name<#(#args),*>;
			}
		};

		let conv_method = |is_map: bool| -> Result<syn::ImplItem, EmitError> {
			let method_name = syn::Ident::new(if is_map { "map" } else { "unmap" }, Span::mixed_site());
			let arg_name = syn::Ident::new(if is_map { "_from" } else { "_to" }, Span::mixed_site());
			let from_type: syn::Type = parse_quote! { Self::From };
			let to_type: syn::Type = parse_quote! { Self::To };

			let arg_type;
			let res_type;
			let assoc_type_name;
			let opposite_assoc_type_name;
			let parameter_conv_method;
			let result_conv_method;
			if is_map {
				arg_type = from_type;
				res_type = to_type;
				assoc_type_name = "To";
				opposite_assoc_type_name = "From";
				parameter_conv_method = "unmap";
				result_conv_method = "map";
			}
			else {
				arg_type = to_type;
				res_type = from_type;
				assoc_type_name = "From";
				opposite_assoc_type_name = "To";
				parameter_conv_method = "map";
				result_conv_method = "unmap";
			}

			let type_emitter = TypeMapperTypeEmitter {
				mod_emitter: self.mod_emitter,
				assoc_type_name,
			};

			let body = TypeMapperAdapterEmitter {
				instance_expr: syn::Expr::Path(syn::ExprPath {
					attrs: vec![],
					qself: None,
					path: syn::Path::from(arg_name.clone()),
				}),
				mapper_emitter: self,
				type_emitter: type_emitter.clone(),
				opposite_type_emitter: TypeMapperTypeEmitter {
					mod_emitter: self.mod_emitter,
					assoc_type_name: opposite_assoc_type_name,
				},
				mapper_expr_emitter: TypeMapperImplExprEmitter {
					type_emitter: type_emitter,
				},
				parameter_conv_method,
				result_conv_method,
			}.emit_create_adapter()?;

			let arg_pat: syn::PatType = syn::PatType {
				attrs: Vec::new(),
				pat: Box::new(syn::Pat::Ident(syn::PatIdent {
					attrs: Vec::new(),
					by_ref: None,
					mutability: None,
					ident: arg_name,
					subpat: None,
				})),
				colon_token: Default::default(),
				ty: Box::new(arg_type),
			};

			let signature = syn::Signature {
				constness: None,
				asyncness: None,
				unsafety: Some(Default::default()),
				abi: None,
				fn_token: Default::default(),
				ident: method_name,
				generics: Default::default(),
				paren_token: Default::default(),
				inputs: Punctuated::from_iter(vec![
					syn::FnArg::Receiver(syn::Receiver {
						attrs: Vec::new(),
						reference: Some((Default::default(), None)),
						mutability: None,
						self_token: Default::default(),
						colon_token: None,
						ty: parse_quote!(&Self)
					}),
					syn::FnArg::Typed(arg_pat),
				]),
				variadic: None,
				output: syn::ReturnType::Type(Default::default(), Box::new(res_type)),
			};

			Ok(syn::ImplItem::Fn(syn::ImplItemFn {
				attrs: Vec::new(),
				vis: syn::Visibility::Inherited,
				defaultness: None,
				sig: signature,
				block: body,
			}))
		};

		Ok(vec![
			syn::Item::Struct(syn::ItemStruct {
				attrs: vec![
					parse_quote! { #[derive(::std::clone::Clone, ::std::marker::Copy)] },
					parse_quote! { #[allow(non_camel_case_types)] },
				],
				vis: syn::Visibility::Public(Default::default()),
				struct_token: Default::default(),
				ident: mapped_name.clone(),
				generics: type_parameters.clone(),
				fields,
				semi_token: None,
			}),

			syn::Item::Impl(syn::ItemImpl {
				attrs: vec![],
				defaultness: None,
				unsafety: None,
				impl_token: Default::default(),
				generics: type_parameters,
				trait_: Some((
					None,
					parse_quote! { ::noble_idl_runtime::ValueMapper },
					Default::default(),
				)),
				self_ty: Box::new(syn::Type::Path(syn::TypePath {
					qself: None,
					path: syn::Path::from(syn::PathSegment {
						ident: mapped_name,
						arguments: type_args,
					}),
				})),
				brace_token: Default::default(),
				items: vec![
					conv_type("From"),
					conv_type("To"),
					conv_method(true)?,
					conv_method(false)?,
				],
			}),
		])
	}

	fn type_parameters(&self) -> syn::Generics {
		syn::Generics {
			lt_token: Default::default(),
			params: self.dfn.type_parameters.iter()
				.map(|tp| syn::GenericParam::Type(syn::TypeParam {
					attrs: vec![
						parse_quote! { #[allow(non_camel_case_types)] }
					],
					ident: format_ident!("{}_Mapper", convert_id_pascal(tp.name())),
					colon_token: Some(Default::default()),
					bounds: <[syn::TypeParamBound; 4]>::into_iter([
						parse_quote! { ::noble_idl_runtime::ValueMapper },
						parse_quote! { ::std::marker::Send },
						parse_quote! { ::std::marker::Sync },
						parse_quote! { 'static },
					]).into_iter().collect(),
					eq_token: None,
					default: None,
				}))
				.collect(),
			gt_token: Default::default(),
			where_clause: None,
		}
	}
}

#[derive(Clone)]
struct TypeMapperAdapterEmitter<'a, 'b, TE> {
	instance_expr: syn::Expr,

	mapper_emitter: &'b InterfaceMapperEmitter<'a>,

	type_emitter: TE,
	opposite_type_emitter: TE,
	mapper_expr_emitter: TypeMapperImplExprEmitter<TE>,

	parameter_conv_method: &'static str,
	result_conv_method: &'static str,
}

impl <'a, 'b, TE: TypeEmitter<'a> + Clone> TypeMapperAdapterEmitter<'a, 'b, TE> {
	fn emit_create_adapter(&self) -> Result<syn::Block, EmitError> {
		let dfn = self.mapper_emitter.dfn;

		let adapter_name = syn::Ident::new("Adapter", Span::mixed_site());

		let mut fields = Punctuated::new();
		fields.push(syn::Field {
			attrs: vec![],
			vis: syn::Visibility::Inherited,
			mutability: syn::FieldMutability::None,
			ident: Some(syn::Ident::new("instance", Span::mixed_site())),
			colon_token: Some(Default::default()),
			ty: self.opposite_type_emitter.emit_type_expr(&dfn_as_type(dfn))?,
		});
		fields.extend(
			dfn.type_parameters.iter()
				.map(|tp| syn::Field {
					attrs: vec![],
					vis: syn::Visibility::Inherited,
					mutability: syn::FieldMutability::None,
					ident: Some(format_ident!("mapper_{}", convert_id_snake(tp.name()))),
					colon_token: Some(Default::default()),
					ty: syn::Type::Path(syn::TypePath {
						qself: None,
						path: syn::Path::from(format_ident!("{}_Mapper", convert_id_pascal(tp.name()))),
					}),
				})
		);

		let struct_def: syn::ItemStruct = syn::ItemStruct {
			attrs: vec![],
			vis: syn::Visibility::Inherited,
			struct_token: Default::default(),
			ident: adapter_name.clone(),
			generics: self.mapper_emitter.type_parameters(),
			fields: syn::Fields::Named(syn::FieldsNamed {
				brace_token: Default::default(),
				named: fields,
			}),
			semi_token: None,
		};

		let methods = self.mapper_emitter.iface.methods.iter()
			.map(|m|
				self.emit_impl_method(m)
					.map(syn::ImplItem::Fn)
			)
			.collect::<Result<Vec<_>, _>>()?;

		let iface_impl = syn::ItemImpl {
			attrs: vec![],
			defaultness: None,
			unsafety: None,
			impl_token: Default::default(),
			generics: self.mapper_emitter.type_parameters(),
			trait_: Some((
				None,
				self.type_emitter.get_interface_trait(dfn)?,
				Default::default(),
			)),
			self_ty: Box::new(syn::Type::Path(syn::TypePath {
				qself: None,
				path: syn::Path::from(syn::PathSegment {
					ident: adapter_name.clone(),
					arguments: syn::PathArguments::AngleBracketed(syn::AngleBracketedGenericArguments {
						colon2_token: None,
						lt_token: Default::default(),
						args: dfn.type_parameters.iter()
							.map(|tp| syn::GenericArgument::Type(
								syn::Type::Path(syn::TypePath {
									qself: None,
									path: syn::Path::from(format_ident!("{}_Mapper", convert_id_pascal(tp.name()))),
								})
							))
							.collect(),
						gt_token: Default::default(),
					}),
				})
			})),
			brace_token: Default::default(),
			items: methods,
		};

		let mut field_values = Punctuated::new();
		field_values.push(syn::FieldValue {
			attrs: vec![],
			member: syn::Member::Named(syn::Ident::new("instance", Span::mixed_site())),
			colon_token: Some(Default::default()),
			expr: self.instance_expr.clone(),
		});
		field_values.extend(
			dfn.type_parameters.iter()
				.enumerate()
				.map(|(index, tp)| syn::FieldValue {
					attrs: vec![],
					member: syn::Member::Named(format_ident!("mapper_{}", convert_id_snake(tp.name()))),
					colon_token: Some(Default::default()),
					expr: syn::Expr::Field(syn::ExprField {
						attrs: vec![],
						member: syn::Member::Unnamed(syn::Index::from(index)),
						base: Box::new(parse_quote!(self)),
						dot_token: Default::default(),
					}),
				})
		);

		let adapter_expr = syn::Expr::Struct(syn::ExprStruct {
			attrs: vec![],
			qself: None,
			path: syn::Path::from(adapter_name),
			brace_token: Default::default(),
			fields: field_values,
			dot2_token: None,
			rest: None,
		});

		let mut new_path = self.type_emitter.get_literal_type_path(&dfn_as_type(dfn))?;
		new_path.segments.push(syn::PathSegment {
			ident: syn::Ident::new("new", Span::call_site()),
			arguments: syn::PathArguments::None,
		});

		let value_expr = syn::Expr::Call(syn::ExprCall {
			attrs: vec![],
			func: Box::new(syn::Expr::Path(syn::ExprPath {
				attrs: vec![],
				qself: None,
				path: new_path,
			})),
			paren_token: Default::default(),
			args: [
				adapter_expr,
			].into_iter().collect(),
		});


		Ok(syn::Block {
			brace_token: Default::default(),
			stmts: vec![
				syn::Stmt::Item(syn::Item::Struct(struct_def)),
				syn::Stmt::Item(syn::Item::Impl(iface_impl)),
				syn::Stmt::Expr(value_expr, None),
			],
		})
	}
}

impl <'a, 'b, TE: TypeEmitter<'a> + Clone> MethodEmitter<'a> for TypeMapperAdapterEmitter<'a, 'b, TE> {
	type TE = TE;

	fn type_emitter(&self) -> &Self::TE {
		&self.type_emitter
	}

	fn self_arg(&self) -> Result<syn::FnArg, EmitError> {
		Ok(parse_quote! { self: ::std::sync::Arc<Self> })
	}

	fn emit_method_body<'c: 'a>(&self, m: &'c InterfaceMethod) -> Result<Option<Block>, EmitError> {
		let write_mapper_usage = |t, name: &str, value: syn::Expr| -> Result<syn::Expr, EmitError> {
			let mapper = self.mapper_expr_emitter.emit_type_mapper_expr_for_impl(t)?;
			let name = format_ident!("{}", name);
			Ok(syn::Expr::Call(syn::ExprCall {
				attrs: vec![],
				func: Box::new(syn::Expr::Path(syn::ExprPath {
					attrs: vec![],
					qself: None,
					path: syn::Path {
						leading_colon: Some(Default::default()),
						segments: [
							syn::PathSegment {
								ident: syn::Ident::new("noble_idl_runtime", Span::mixed_site()),
								arguments: syn::PathArguments::None,
							},
							syn::PathSegment {
								ident: syn::Ident::new("ValueMapper", Span::mixed_site()),
								arguments: syn::PathArguments::None,
							},
							syn::PathSegment {
								ident: name,
								arguments: syn::PathArguments::None,
							},
						].into_iter().collect(),
					},
				})),
				paren_token: Default::default(),
				args: [
					syn::Expr::Reference(syn::ExprReference {
						attrs: vec![],
						and_token: Default::default(),
						mutability: None,
						expr: Box::new(mapper),
					}),
					value
				].into_iter().collect(),
			}))
		};

		let method_name = convert_id_snake(&m.name);

		let call_expr = syn::Expr::Call(syn::ExprCall {
			attrs: vec![],
			func: Box::new(syn::Expr::Path(syn::ExprPath {
				attrs: vec![],
				qself: None,
				path: {
					let mut path = self.opposite_type_emitter.get_type_path(&dfn_as_type(self.mapper_emitter.dfn))?;

					match path.segments.last_mut() {
						Some(syn::PathSegment { arguments: syn::PathArguments::AngleBracketed(args), .. }) => {
							args.colon2_token = Some(Default::default());
						}
						_ => {},
					}

					path.segments.push(syn::PathSegment {
						ident: method_name.clone(),
						arguments: syn::PathArguments::None,
					});
					path
				},
			})),
			paren_token: Default::default(),
			args:
				vec![
					{
						Ok(parse_quote! { ::std::clone::Clone::clone(&self.instance) })
					},
				].into_iter().chain(
					m.parameters.iter()
					.map(|param| {
						write_mapper_usage(
							&param.parameter_type,
							self.parameter_conv_method,
							syn::Expr::Path(syn::ExprPath {
								attrs: vec![],
								qself: None,
								path: syn::Path::from(convert_id_snake(&param.name)),
							}),
						)
					})
				)
				.collect::<Result<_, _>>()?,
		});

		let impl_expr =
			if let Some(throws_type) = m.throws.as_deref() {
				let map_res = write_mapper_usage(m.return_type.as_ref(), self.result_conv_method, parse_quote! { x })?;
				let map_err = write_mapper_usage(throws_type, self.result_conv_method, parse_quote! { e })?;

				let map_closure = syn::Expr::Closure(syn::ExprClosure {
					attrs: Vec::new(),
					lifetimes: None,
					constness: None,
					asyncness: None,
					movability: None,
					capture: None,
					or1_token: Default::default(),
					inputs: vec![
						syn::Pat::Ident(syn::PatIdent {
							attrs: Vec::new(),
							by_ref: None,
							mutability: None,
							ident: syn::Ident::new("x", Span::call_site()),
							subpat: None,
						}),
					].into_iter().collect(),
					or2_token: Default::default(),
					output: syn::ReturnType::Default,
					body: Box::new(map_res),
				});

				let map_expr = syn::Expr::MethodCall(syn::ExprMethodCall {
					attrs: Vec::new(),
					receiver: Box::new(call_expr),
					dot_token: Default::default(),
					method: syn::Ident::new("map", Span::call_site()),
					turbofish: None,
					paren_token: Default::default(),
					args: vec![map_closure].into_iter().collect(),
				});

				let map_err_closure = syn::Expr::Closure(syn::ExprClosure {
					attrs: Vec::new(),
					lifetimes: None,
					constness: None,
					asyncness: None,
					movability: None,
					capture: None,
					or1_token: Default::default(),
					inputs: vec![
						syn::Pat::Ident(syn::PatIdent {
							attrs: Vec::new(),
							by_ref: None,
							mutability: None,
							ident: syn::Ident::new("e", Span::call_site()),
							subpat: None,
						}),
					].into_iter().collect(),
					or2_token: Default::default(),
					output: syn::ReturnType::Default,
					body: Box::new(map_err),
				});

				let map_err_expr = syn::Expr::MethodCall(syn::ExprMethodCall {
					attrs: Vec::new(),
					receiver: Box::new(map_expr),
					dot_token: Default::default(),
					method: syn::Ident::new("map_err", Span::call_site()),
					turbofish: None,
					paren_token: Default::default(),
					args: vec![map_err_closure].into_iter().collect(),
				});


				map_err_expr
			}
			else {
				write_mapper_usage(m.return_type.as_ref(), self.result_conv_method, call_expr)?
			};

		Ok(Some(syn::Block {
			brace_token: Default::default(),
			stmts: vec![
				syn::Stmt::Expr(syn::Expr::Unsafe(syn::ExprUnsafe {
					attrs: vec![],
					unsafe_token: Default::default(),
					block: syn::Block {
						brace_token: Default::default(),
						stmts: vec![
							syn::Stmt::Expr(impl_expr, None),
						],
					}
				}), None),
			],
		}))
	}
}


#[derive(Clone)]
struct TypeMapperTypeEmitter<'a> {
	mod_emitter: &'a ModEmitter<'a>,
	assoc_type_name: &'static str,
}

impl <'a> TypeEmitter<'a> for TypeMapperTypeEmitter<'a> {
	fn mod_emitter(&self) -> &ModEmitter<'a> {
		self.mod_emitter
	}

	fn get_type_type_parameter_path(&self, name: &str) -> Result<syn::Path, EmitError> {
		let mapper_type = format_ident!("{}_Mapper", convert_id_pascal(&name));
		let assoc_type = syn::Ident::new(self.assoc_type_name, Span::mixed_site());
		Ok(parse_quote! { #mapper_type::#assoc_type })
	}
}


fn dfn_as_type(dfn: &DefinitionInfo) -> TypeExpr {
	TypeExpr::DefinedType(
		dfn.name.clone(),
		dfn.type_parameters.iter()
			.map(|tp| Box::new(
				TypeExpr::TypeParameter {
					name: tp.name().to_owned(),
					owner: TypeParameterOwner::ByType,
				}
			))
			.collect(),
	)
}



fn convert_id_snake_str(s: &str) -> String {
	s.replace("-", "_")
}

fn convert_id_snake(s: &str) -> syn::Ident {
	idstr(&convert_id_snake_str(s))
}

fn convert_id_pascal(s: &str) -> syn::Ident {
	idstr(&s.split("-").map(|seg| {
		let mut seg = seg.to_owned();
		if !seg.is_empty() {
			let start = &mut seg[0..1];

			if start.chars().all(char::is_alphabetic) {
				str::make_ascii_uppercase(start);
			}
			else {
				seg.insert(0, '_');
			}

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



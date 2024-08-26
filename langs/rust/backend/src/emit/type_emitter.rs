use super::{EmitError, ModEmitter, dfn_as_type};
use super::ident::{convert_id_pascal, idstr};

use noble_idl_api::*;

use syn::parse_quote;
use syn::punctuated::Punctuated;
use quote::format_ident;


pub(super) enum TypeBoxing {
	None,
	Box,
}


pub(super) trait TypeEmitter<'a> {
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
				.flat_map(|param| self.emit_type_parameter(param.as_ref()))
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

	fn emit_type_parameter(&self, tp: &TypeParameter) -> Option<syn::GenericParam> {
		match tp {
			TypeParameter::Type { name: param, .. } => {
				Some(syn::GenericParam::Type(syn::TypeParam::from(convert_id_pascal(param))))
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
pub(super) struct DefaultTypeEmitter<'a>(pub &'a ModEmitter<'a>);

impl <'a> TypeEmitter<'a> for DefaultTypeEmitter<'a> {
	fn mod_emitter(&self) -> &ModEmitter<'a> {
		&self.0
	}
}

pub(super) struct ErasedValueTypeEmitter<'a>(pub &'a ModEmitter<'a>);

impl <'a> TypeEmitter<'a> for ErasedValueTypeEmitter<'a> {
	fn mod_emitter(&self) -> &ModEmitter<'a> {
		&self.0
	}

	fn get_method_type_parameter_path(&self, _name: &str) -> Result<syn::Path, EmitError> {
		Ok(parse_quote! { ::noble_idl_runtime::erasure::ErasedValue })
	}

	fn emit_type_parameter(&self, _tp: &TypeParameter) -> Option<syn::GenericParam> {
		None
	}
}







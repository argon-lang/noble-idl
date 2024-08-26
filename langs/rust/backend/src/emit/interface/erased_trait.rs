use noble_idl_api::*;
use proc_macro2::Span;

use crate::emit::type_emitter::*;
use crate::emit::trait_emitter::*;
use crate::emit::method_emitter::MethodEmitter;
use crate::emit::ident::*;
use crate::emit::EmitError;
use syn::parse_quote;
use quote::format_ident;

pub(in super::super) struct InterfaceErasedImplEmitter<'a> {
	pub dfn: &'a DefinitionInfo,
	pub i: &'a InterfaceDefinition,
	pub type_emitter: ErasedValueTypeEmitter<'a>,
	pub impl_type_emitter: DefaultTypeEmitter<'a>,
}

impl <'a> InterfaceErasedImplEmitter<'a> {
	pub fn emit_interface_erased_impl(&self) -> Result<syn::ItemImpl, EmitError> {
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

	fn emit_method_body<'b: 'a>(&self, m: &'b InterfaceMethod) -> Result<Option<syn::Block>, EmitError> {
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



pub(in super::super) struct InterfaceTraitErasedEmitter<'a> {
	pub type_emitter: ErasedValueTypeEmitter<'a>,
	pub trait_type_emitter: DefaultTypeEmitter<'a>,
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

	fn emit_method_body<'b: 'a>(&self, _m: &'b InterfaceMethod) -> Result<Option<syn::Block>, EmitError> {
		Ok(None)
	}
}



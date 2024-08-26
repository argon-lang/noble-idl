use noble_idl_api::*;
use quote::format_ident;
use syn::parse_quote;

use crate::emit::{EmitError, dfn_as_type};
use crate::emit::ident::*;
use crate::emit::type_emitter::*;
use crate::emit::method_emitter::*;
use crate::emit::type_mapper::*;


pub(in super::super) struct InterfacePublicImplEmitter<'a> {
	pub dfn: &'a DefinitionInfo,
	pub i: &'a InterfaceDefinition,
	pub type_emitter: DefaultTypeEmitter<'a>,
}

impl <'a> InterfacePublicImplEmitter<'a> {
	pub fn emit_interface_public_impl(&self) -> Result<syn::ItemImpl, EmitError> {
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

	fn emit_method_body<'b: 'a>(&self, m: &'b InterfaceMethod) -> Result<Option<syn::Block>, EmitError> {
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
								ident: idstr("noble_idl_runtime"),
								arguments: syn::PathArguments::None,
							},
							syn::PathSegment {
								ident: idstr("ValueMapper"),
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




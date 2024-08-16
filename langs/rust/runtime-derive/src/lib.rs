use proc_macro::TokenStream;
use proc_macro2::Span;
use quote::{quote, format_ident};
use syn::TraitBound;
use syn::TraitBoundModifier;
use syn::TypeParam;
use syn::TypeParamBound;
use syn::{
    parse_quote,
    punctuated::Punctuated,
    Data,
    DeriveInput,
    Expr,
    ExprPath,
    Field,
    Fields,
    GenericArgument,
    GenericParam,
    Ident,
    Index,
    Path,
    Token,
    Type,
    TypePath,
};

#[proc_macro_derive(Erasure)]
pub fn derive_erasure(input: TokenStream) -> TokenStream {
    TokenStream::from(derive_erasure_impl(proc_macro2::TokenStream::from(input)))
}

type TokenRes = Result<proc_macro2::TokenStream, proc_macro2::TokenStream>;

fn flatten_token_res(res: TokenRes) -> proc_macro2::TokenStream {
    match res {
        Ok(ts) => ts,
        Err(ts) => ts,
    }
}

fn derive_erasure_impl(input: proc_macro2::TokenStream) -> proc_macro2::TokenStream {
    let input: DeriveInput = parse_quote!(#input);

    let type_name = input.ident;


    let generics_params = input.generics.params.into_iter().collect::<Vec<_>>();

    let generics_lt = input.generics.lt_token;
    let generics_gt = input.generics.gt_token;

    let type_args = generics_params.iter().map(param_to_arg).collect::<Punctuated<GenericArgument, Token![,]>>();
    let type_args_erased = generics_params.iter().map(param_to_arg_erased).collect::<Punctuated<GenericArgument, Token![,]>>();

	if generics_params.is_empty() {
		quote! {
			impl ::noble_idl_runtime::IdentityErasure for #type_name {}
		}
	}
	else {
		let erase = flatten_token_res(get_erase(&type_name, &generics_params, &input.data));
		let unerase = flatten_token_res(get_unerase(&type_name, &generics_params, &input.data));


		let constraints = generics_params.iter()
			.flat_map(|param| {
				match param {
					GenericParam::Type(tp) => {
						let name = &tp.ident;
						let erased_name = format_ident!("{}_Erased", tp.ident.to_string());

						vec![quote! {
							::noble_idl_runtime::ErasureToken<#name, #erased_name> : ::noble_idl_runtime::Erasure<Concrete = #name, Erased = #erased_name>
						}]
					},

					GenericParam::Lifetime(_) => vec![],
					GenericParam::Const(_) => vec![],
				}
			})
			.collect::<Vec<_>>();


		let type_params = {
			let params = generics_params.into_iter()
				.flat_map(|mut p| {
					match &mut p {
						GenericParam::Type(tp) => {
							let erased_name = format_ident!("{}_Erased", tp.ident.to_string());

							tp.colon_token.get_or_insert(Default::default());
							tp.bounds.push(TypeParamBound::Lifetime(syn::Lifetime::new("'static", Span::mixed_site())));

							let tp_erased = TypeParam {
								attrs: vec![],
								ident: erased_name,
								colon_token: Some(Default::default()),
								bounds:
									Punctuated::from_iter([
										TypeParamBound::Lifetime(syn::Lifetime::new("'static", Span::mixed_site()))
									].into_iter()),
								eq_token: None,
								default: None,
							};

							vec![
								p,
								GenericParam::Type(tp_erased),
							]
						},

						GenericParam::Lifetime(_) => vec![p],
						GenericParam::Const(_) => vec![p],
					}
				})
				.collect::<Vec<_>>();

			quote! {
				<#(#params),*>
			}
		};

		quote! {
			impl #type_params ::noble_idl_runtime::Erasure for ::noble_idl_runtime::ErasureToken<#type_name #generics_lt #type_args #generics_gt, #type_name #generics_lt #type_args_erased #generics_gt>
				where #(#constraints),*
			{
				fn erase(self) -> #type_name #generics_lt #type_args_erased #generics_gt {
					#erase
				}

				fn unerase(e: #type_name #generics_lt #type_args_erased #generics_gt) -> Self {
					#unerase
				}
			}
		}
	}

}

fn get_erase(type_name: &Ident, type_params: &[GenericParam], data: &Data) -> TokenRes {
	if type_params.is_empty() {
		Ok(quote! { self })
	}
	else {
		make_erasure_type(type_name, type_params, data, &parse_quote! { self }, &Ident::new("erase", Span::mixed_site()))
	}
}

fn get_unerase(type_name: &Ident, type_params: &[GenericParam], data: &Data) -> TokenRes {
	if type_params.is_empty() {
		Ok(quote! { e })
	}
	else {
		make_erasure_type(type_name, type_params, data, &parse_quote! { e }, &Ident::new("unerase", Span::mixed_site()))
	}
}



fn param_to_arg(p: &GenericParam) -> GenericArgument {
    match p {
        GenericParam::Lifetime(l) => GenericArgument::Lifetime(l.lifetime.clone()),
        GenericParam::Type(t) =>
            GenericArgument::Type(Type::Path(TypePath {
                qself: None,
                path: Path::from(t.ident.clone()),
            })),
        GenericParam::Const(c) =>
            GenericArgument::Const(Expr::Path(ExprPath {
                attrs: vec!(),
                qself: None,
                path: Path::from(c.ident.clone()),
            })),
    }
}

fn param_to_arg_erased(p: &GenericParam) -> GenericArgument {
    match p {
        GenericParam::Lifetime(l) => GenericArgument::Lifetime(l.lifetime.clone()),
        GenericParam::Type(t) =>
            GenericArgument::Type(Type::Path(TypePath {
                qself: None,
                path: Path::from(format_ident!("{}_Erased", t.ident.clone())),
            })),
        GenericParam::Const(c) =>
            GenericArgument::Const(Expr::Path(ExprPath {
                attrs: vec!(),
                qself: None,
                path: Path::from(c.ident.clone()),
            })),
    }
}

fn make_erasure_type(type_name: &Ident, type_params: &[GenericParam], data: &Data, obj: &Expr, op: &Ident) -> TokenRes {
	match data {
		Data::Struct(s) => {
			let make_field_value = |field: &Field, index: usize| -> Expr {
				if let Some(ident) = &field.ident {
					parse_quote! { #obj.#ident }
				} else {
					let index = Index::from(index);
					parse_quote! { #obj.#index }
				}
			};
			Ok(make_erasure_construct(&parse_quote! { #type_name }, type_params, &s.fields, op, make_field_value))
		},
		Data::Enum(e) => {
			let match_arms: Vec<_> = e.variants.iter()
				.map(|variant| {
					let variant_ident = &variant.ident;

					let make_binding_name = |field: &Field, index: usize| {
						if let Some(ident) = &field.ident {
							format_ident!("field_{}", ident)
						} else {
							format_ident!("field_{}", index)
						}
					};

					let make_field_value = |field: &Field, index: usize| -> Expr {
						let binding_name = make_binding_name(field, index);
						parse_quote! { #binding_name }
					};

					let body = make_erasure_construct(
						&parse_quote! { #type_name::#variant_ident },
						type_params,
						&variant.fields,
						op,
						make_field_value
					);

					let make_binding = |field: &Field, index: usize| {
						let binding_name = make_binding_name(field, index);
						if let Some(field_name) = &field.ident {
							quote! { #field_name: #binding_name }
						} else {
							quote! { #binding_name }
						}
					};

					match &variant.fields {
						Fields::Named(fields_named) => {
							let field_bindings: Vec<_> = fields_named.named.iter()
								.enumerate()
								.map(|(i, field)| make_binding(field, i))
								.collect();

							quote! {
								#type_name::#variant_ident { #(#field_bindings),* } => {
									#body
								}
							}
						}
						Fields::Unnamed(fields_unnamed) => {
							let field_bindings: Vec<_> = fields_unnamed.unnamed.iter()
								.enumerate()
								.map(|(i, field)| make_binding(field, i))
								.collect();

							quote! {
								#type_name::#variant_ident(#(#field_bindings),*) => {
									#body
								}
							}
						}
						Fields::Unit => {
							quote! {
								#type_name::#variant_ident => {
									#body
								}
							}
						},
					}
				})
				.collect::<Vec<_>>();

			Ok(quote! {
				match #obj {
					#(#match_arms),*
				}
			})
		},
		Data::Union(_) => Err(quote! { compile_error!("Erasure cannot be derived for union"); })?,
	}
}

fn make_erasure_construct(path: &Path, type_params: &[GenericParam], fields: &Fields, op: &Ident, make_field_value: impl Fn(&Field, usize) -> Expr) -> proc_macro2::TokenStream {
	let make_field = |f: &Field, index: usize| -> proc_macro2::TokenStream {
		let e = make_field_value(f, index);
		let value = make_erasure_call(e, &f.ty, type_params, op);

		if let Some(name) = &f.ident {
			quote! { #name: #value }
		}
		else {
			value
		}
	};

	match fields {
		Fields::Named(fields) => {
			let field_values = fields.named
				.iter()
				.enumerate()
				.map(|(i, f)| make_field(f, i))
				.collect::<Vec<_>>();

			quote! {
				#path { #(#field_values),* }
			}
		},
		Fields::Unnamed(fields) => {
			let field_values = fields.unnamed
				.iter()
				.enumerate()
				.map(|(i, f)| make_field(f, i))
				.collect::<Vec<_>>();

			quote! {
				#path(#(#field_values),*)
			}
		},
		Fields::Unit => quote! { #path },
	}
}

fn make_erasure_call(e: Expr, t: &Type, type_params: &[GenericParam], op: &Ident) -> proc_macro2::TokenStream {
	let mut t_erased = t.clone();
	replace_type_params_with_erased(&mut t_erased, type_params);

	parse_quote! {
		<::noble_idl_runtime::ErasureToken<#t, #t_erased> as ::noble_idl_runtime::Erasure<Concrete = #t, Erased = #t_erased>>::#op(#e)
	}
}

fn replace_type_params_with_erased(ty: &mut Type, params: &[GenericParam]) {
	use syn::visit_mut::VisitMut;

    struct TypeReplacer<'a> {
        params: &'a [GenericParam],
    }

    impl<'a> VisitMut for TypeReplacer<'a> {
        fn visit_type_path_mut(&mut self, type_path: &mut TypePath) {
            if let Some(ident) = type_path.path.get_ident() {
				let param_matches = |param: &GenericParam| match param {
					GenericParam::Lifetime(_) => false,
					GenericParam::Type(param) => param.ident == *ident,
					GenericParam::Const(_) => false,
				};

                if self.params.iter().any(param_matches) {
                    let new_ident = format_ident!("{}_Erased", ident);
                    type_path.path.segments[0].ident = new_ident;
                }
            }
        }
    }

    let mut replacer = TypeReplacer { params };
    replacer.visit_type_mut(ty);
}





use noble_idl_api::*;
use syn::parse_quote;
use syn::punctuated::Punctuated;
use crate::emit::{ModEmitter, EmitError, dfn_as_type};
use crate::emit::ident::*;
use crate::emit::type_emitter::*;
use crate::emit::method_emitter::*;
use quote::format_ident;


pub(in super::super) struct InterfaceMapperEmitter<'a> {
	pub mod_emitter: &'a ModEmitter<'a>,
	pub dfn: &'a DefinitionInfo,
	pub iface: &'a InterfaceDefinition,
}

impl <'a> InterfaceMapperEmitter<'a> {
	pub fn emit_mapper_type(&self) -> Result<Vec<syn::Item>, EmitError> {
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
			let name = idstr(name);
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
			let method_name = idstr(if is_map { "map" } else { "unmap" });
			let arg_name = idstr(if is_map { "_from" } else { "_to" });
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

		let adapter_name = idstr("Adapter");

		let mut fields = Punctuated::new();
		fields.push(syn::Field {
			attrs: vec![],
			vis: syn::Visibility::Inherited,
			mutability: syn::FieldMutability::None,
			ident: Some(idstr("instance")),
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
			member: syn::Member::Named(idstr("instance")),
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
			ident: idstr("new"),
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

	fn emit_method_body<'c: 'a>(&self, m: &'c InterfaceMethod) -> Result<Option<syn::Block>, EmitError> {
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
							ident: idstr("x"),
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
					method: idstr("map"),
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
							ident: idstr("e"),
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
					method: idstr("map_err"),
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
		let assoc_type = idstr(self.assoc_type_name);
		Ok(parse_quote! { #mapper_type::#assoc_type })
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




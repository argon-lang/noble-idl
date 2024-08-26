use noble_idl_api::*;
use super::EmitError;
use super::type_emitter::*;

use super::ident::*;

use syn::parse_quote;
use quote::format_ident;

pub(super) struct TypeMapperExprEmitter<TE> {
	pub type_emitter: TE,
}

impl <'a, TE: TypeEmitter<'a> + Clone> TypeMapperExprEmitter<TE> {

	pub fn emit_type_mapper_expr(&self, t: &TypeExpr) -> Result<syn::Expr, EmitError> {
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





use super::EmitError;
use super::type_emitter::TypeEmitter;
use super::ident::convert_id_snake;
use noble_idl_api::*;
use syn::punctuated::Punctuated;

pub(super) trait MethodEmitter<'a> {
	type TE: TypeEmitter<'a>;
	fn type_emitter(&self) -> &Self::TE;
	fn self_arg(&self) -> Result<syn::FnArg, EmitError>;
	fn emit_method_body<'b: 'a>(&self, m: &'b InterfaceMethod) -> Result<Option<syn::Block>, EmitError>;
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



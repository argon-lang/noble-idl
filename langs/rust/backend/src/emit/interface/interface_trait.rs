
use super::super::type_emitter::*;
use super::super::trait_emitter::*;
use super::super::method_emitter::*;

use super::super::EmitError;

use syn::parse_quote;
use noble_idl_api::*;


pub(in super::super) struct InterfaceTraitInterfaceEmitter<'a> {
	pub type_emitter: DefaultTypeEmitter<'a>,
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

	fn emit_method_body<'b: 'a>(&self, _m: &'b InterfaceMethod) -> Result<Option<syn::Block>, EmitError> {
		Ok(None)
	}
}



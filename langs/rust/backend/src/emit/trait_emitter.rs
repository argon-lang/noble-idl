use super::method_emitter::MethodEmitter;
use super::type_emitter::TypeEmitter;
use noble_idl_api::*;
use syn::punctuated::Punctuated;
use super::EmitError;
use super::ident::convert_id_pascal;

use quote::format_ident;
use syn::parse_quote;

pub(super) trait TraitEmitter<'a>: MethodEmitter<'a> {
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


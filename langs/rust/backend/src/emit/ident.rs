use itertools::Itertools;



pub(super) fn convert_id_snake_str(s: &str) -> String {
	s.replace("-", "_")
}

pub(super) fn convert_id_snake(s: &str) -> syn::Ident {
	idstr(&convert_id_snake_str(s))
}

pub(super) fn convert_id_pascal(s: &str) -> syn::Ident {
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

pub(super) fn idstr(s: &str) -> syn::Ident {
	syn::Ident::new(s, proc_macro2::Span::mixed_site())
}



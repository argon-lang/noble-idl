use esexpr::ESExprCodec;


#[derive(ESExprCodec, PartialEq, Clone, Debug)]
pub enum RustAnnRecord {
	Unit,
	Tuple,
	Derive(String),
}


#[derive(ESExprCodec, PartialEq, Clone, Debug)]
pub enum RustAnnEnum {
	Derive(String),
}


#[derive(ESExprCodec, PartialEq, Clone, Debug)]
pub enum RustAnnSimpleEnum {
	Derive(String),
}

#[derive(ESExprCodec, PartialEq, Clone, Debug)]
pub enum RustAnnEnumCase {
	Unit,
	Tuple,
}



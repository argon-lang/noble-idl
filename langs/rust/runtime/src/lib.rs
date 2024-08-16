use std::{any::Any, collections::{HashMap, HashSet}, marker::PhantomData};

use esexpr::{ESExpr, ESExprCodec, ESExprTag};


pub struct Erased<'a>(Box<dyn Any + 'static>, PhantomData<&'a ()>);

pub trait Erasure where Self: Sized {
	type Concrete;
	type Erased;

	fn erase(c: Self::Concrete) -> Self::Erased;
	fn unerase(e: Self::Erased) -> Self::Concrete;
}

pub struct AnyErasure<'a, A>(PhantomData<A>, PhantomData<&'a ()>);

impl <'a, A: 'static> Erasure for AnyErasure<'a, A> {
	type Concrete = A;
	type Erased = Erased<'a>;

	fn erase(a: A) -> Erased<'a> {
		Erased(Box::new(a), PhantomData {})
	}

	fn unerase(e: Erased<'a>) -> A {
		e.0
			.downcast()
			.map(|s| *s)
			.unwrap()
	}
}

pub struct IdentityErasure<A>(PhantomData<A>);

impl <A> Erasure for IdentityErasure<A> {
	type Concrete = A;
	type Erased = A;

	fn erase(c: A) -> A {
		c
	}

	fn unerase(e: A) -> A {
		e
	}
}


pub struct BoxErasure<A>(PhantomData<A>);
impl <AE: Erasure> Erasure for BoxErasure<AE> {
	type Concrete = Box<AE::Concrete>;
	type Erased = Box<AE::Erased>;

	fn erase(a: Box<AE::Concrete>) -> Box<AE::Erased> {
		Box::new(AE::erase(*a))
	}

	fn unerase(e: Box<AE::Erased>) -> Box<AE::Concrete> {
		Box::new(AE::unerase(*e))
	}
}


pub type Esexpr = ESExpr;

#[allow(non_camel_case_types)]
pub type Esexpr_Erasure = IdentityErasure<Esexpr>;

pub type String = std::string::String;

#[allow(non_camel_case_types)]
pub type String_Erasure = IdentityErasure<String>;

pub type Int = num_bigint::BigInt;

pub type Nat = num_bigint::BigUint;

pub type Bool = bool;

pub type I8 = i8;

pub type U8 = u8;

pub type I16 = i16;

pub type U16 = u16;

pub type I32 = i32;

pub type U32 = u32;

pub type I64 = i64;

pub type U64 = u64;


pub type F32 = f32;

pub type F64 = f64;

pub type Unit = ();

pub type List<A> = Vec<A>;

#[allow(non_camel_case_types)]
pub struct List_Erasure<ElemErasure>(ElemErasure);
impl <AE: Erasure> Erasure for List_Erasure<AE> {
	type Concrete = Vec<AE::Concrete>;
	type Erased = Vec<AE::Erased>;

	fn erase(a: Vec<AE::Concrete>) -> Vec<AE::Erased> {
		a.into_iter()
			.map(AE::erase)
			.collect()
	}

	fn unerase(e: Vec<AE::Erased>) -> Vec<AE::Concrete> {
		e.into_iter()
			.map(AE::unerase)
			.collect()
	}
}


pub type Option<A> = std::option::Option<A>;
pub type OptionalField<A> = std::option::Option<A>;

#[allow(non_camel_case_types)]
pub struct Option_Erasure<ElemErasure>(ElemErasure);
impl <AE: Erasure> Erasure for Option_Erasure<AE> {
	type Concrete = Option<AE::Concrete>;
	type Erased = Option<AE::Erased>;

	fn erase(a: Option<AE::Concrete>) -> Option<AE::Erased> {
		a.map(AE::erase)
	}

	fn unerase(e: Option<AE::Erased>) -> Option<AE::Concrete> {
		e.map(AE::unerase)
	}
}


pub type Dict<A> = HashMap<String, A>;

#[allow(non_camel_case_types)]
pub struct Dict_Erasure<ElemErasure>(ElemErasure);
impl <AE: Erasure> Erasure for Dict_Erasure<AE> {
	type Concrete = Dict<AE::Concrete>;
	type Erased = Dict<AE::Erased>;

	fn erase(a: Dict<AE::Concrete>) -> Dict<AE::Erased> {
		a.into_iter()
			.map(|(k, v)| (k, AE::erase(v)))
			.collect()
	}

	fn unerase(e: Dict<AE::Erased>) -> Dict<AE::Concrete> {
		e.into_iter()
			.map(|(k, v)| (k, AE::unerase(v)))
			.collect()
	}
}


#[derive(Debug, Clone, PartialEq)]
pub struct Binary(pub Vec<u8>);

impl From<Binary> for Vec<u8> {
    fn from(value: Binary) -> Self {
        value.0
    }
}

impl From<Vec<u8>> for Binary {
    fn from(value: Vec<u8>) -> Self {
        Binary(value)
    }
}

impl ESExprCodec for Binary {
    fn tags() -> HashSet<ESExprTag> {
        HashSet::from([ ESExprTag::Binary ])
    }

    fn encode_esexpr(self) -> ESExpr {
        ESExpr::Binary(self.0)
    }

    fn decode_esexpr(expr: ESExpr) -> Result<Self, esexpr::DecodeError> {
        match expr {
            ESExpr::Binary(b) => Ok(Binary(b)),
            _  => Err(esexpr::DecodeError(esexpr::DecodeErrorType::UnexpectedExpr {
                expected_tags: Self::tags(),
                actual_tag: expr.tag()
            }, esexpr::DecodeErrorPath::Current))
        }
    }
}


#[macro_export]
macro_rules! include_noble_idl {
	() => {
		include!(concat!(env!("OUT_DIR"), "/noble_idl/", module_path!(), ".rs"));
	}
}






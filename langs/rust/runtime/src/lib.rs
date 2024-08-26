use std::{collections::{HashMap, HashSet}, marker::PhantomData};

use esexpr::{ESExpr, ESExprCodec, ESExprTag};

pub mod erasure;


include!("noble_idl_runtime.rs");



pub trait ValueMapper: Copy {
	type From: Clone + Send + Sync + 'static;
	type To: Clone + Send + Sync + 'static;

	unsafe fn map(&self, from: Self::From) -> Self::To;
	unsafe fn unmap(&self, to: Self::To) -> Self::From;
}

#[derive(Clone, Copy)]
pub struct ErasedMapper<E> {
	eraser: E,
}

impl <'a, E: erasure::Eraser + 'static> ErasedMapper<E> {
	pub fn for_eraser(eraser: E) -> Self {
		ErasedMapper { eraser }
	}
}

impl <'a, E: erasure::Eraser> ValueMapper for ErasedMapper<E> {
	type From = E::Concrete;
	type To = erasure::ErasedValue;

	unsafe fn map(&self, from: Self::From) -> Self::To {
		self.eraser.erase(from)
	}

	unsafe fn unmap(&self, to: Self::To) -> Self::From {
		self.eraser.unerase(to)
	}
}

pub struct IdentityMapper<A>(PhantomData<* const A>);
impl <A> IdentityMapper<A> {
	pub fn new() -> Self {
		IdentityMapper(PhantomData)
	}
}

unsafe impl <A> Send for IdentityMapper<A> {}
unsafe impl <A> Sync for IdentityMapper<A> {}

impl <A> Clone for IdentityMapper<A> {
	fn clone(&self) -> Self {
		Self(self.0)
	}
}

impl <A> Copy for IdentityMapper<A> {}


impl <A: Clone + Send + Sync + 'static> ValueMapper for IdentityMapper<A> {
	type From = A;
	type To = A;

	unsafe fn map(&self, from: A) -> A {
		from
	}

	unsafe fn unmap(&self, to: A) -> A {
		to
	}
}


pub type Esexpr = ESExpr;

pub type String = std::string::String;

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

#[derive(Clone, Copy)]
#[allow(non_camel_case_types)]
pub struct List_Mapper<AMapper>(pub AMapper);
impl <AMapper: ValueMapper> ValueMapper for List_Mapper<AMapper> {
	type From = List<AMapper::From>;
	type To = List<AMapper::To>;

	unsafe fn map(&self, from: Self::From) -> Self::To {
		from.into_iter().map(|x| self.0.map(x)).collect()
	}

	unsafe fn unmap(&self, to: Self::To) -> Self::From {
		to.into_iter().map(|x| self.0.unmap(x)).collect()
	}
}

impl <A> From<Box<ListRepr<Box<A>>>> for List<Box<A>> {
	fn from(value: Box<ListRepr<Box<A>>>) -> Self {
		value.values
	}
}

pub type Option<A> = std::option::Option<A>;
pub type OptionalField<A> = std::option::Option<A>;

#[derive(Clone, Copy)]
#[allow(non_camel_case_types)]
pub struct Option_Mapper<AMapper>(pub AMapper);
impl <AMapper: ValueMapper> ValueMapper for Option_Mapper<AMapper> {
	type From = Option<AMapper::From>;
	type To = Option<AMapper::To>;

	unsafe fn map(&self, from: Self::From) -> Self::To {
		from.map(|x| self.0.map(x))
	}

	unsafe fn unmap(&self, to: Self::To) -> Self::From {
		to.map(|x| self.0.unmap(x))
	}
}

#[allow(non_camel_case_types)]
pub type OptionalField_Mapper<A> = Option_Mapper<A>;



pub type Dict<A> = HashMap<String, A>;

#[derive(Clone, Copy)]
#[allow(non_camel_case_types)]
pub struct Dict_Mapper<AMapper>(pub AMapper);
impl <AMapper: ValueMapper> ValueMapper for Dict_Mapper<AMapper> {
	type From = Dict<AMapper::From>;
	type To = Dict<AMapper::To>;

	unsafe fn map(&self, from: Self::From) -> Self::To {
		from.into_iter().map(|(k, v)| (k, self.0.map(v))).collect()
	}

	unsafe fn unmap(&self, to: Self::To) -> Self::From {
		to.into_iter().map(|(k, v)| (k, self.0.unmap(v))).collect()
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






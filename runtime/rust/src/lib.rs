use std::collections::{HashMap, HashSet};

use esexpr::{ESExpr, ESExprCodec, ESExprTag};


pub type String = std::string::String;

pub type Int = num_bigint::BigInt;
pub type Nat = num_bigint::BigUint;

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
pub type Option<A> = std::option::Option<A>;
pub type Dict<A> = HashMap<String, A>;


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


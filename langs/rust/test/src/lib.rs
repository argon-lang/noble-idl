
noble_idl_runtime::include_noble_idl!();

#[derive(Debug, Clone, PartialEq)]
pub struct MyExtern;

#[derive(Debug, Clone, PartialEq)]
pub struct MyExtern2<A>(A);


#[cfg(test)]
mod tests {
	use std::collections::HashMap;
	use esexpr::{ESExpr, ESExprCodec};
	use num_bigint::{BigInt, BigUint};

	#[test]
	fn default_values() {
		let v_expr = ESExpr::Constructor { name: "default-values".to_string(), args: vec![], kwargs: HashMap::new() };
		let v = crate::DefaultValues::decode_esexpr(v_expr).unwrap();

		assert_eq!(true, v.bool_true);
		assert_eq!(false, v.bool_false);
		assert_eq!("abc", v.str_value);
		assert_eq!(b"\xab\xcd\xef", &v.binary_value.0[..]);

		assert_eq!(BigInt::from(55), v.int_value55);
		assert_eq!("18446744073709551616".parse::<BigInt>().unwrap(), v.int_beyond_int64);
		assert_eq!(BigInt::from(-55), v.int_minus55);
		assert_eq!("-18446744073709551617".parse::<BigInt>().unwrap(), v.int_beyond_neg_int64);
		assert_eq!(BigUint::from(55u32), v.nat_value55);
		assert_eq!("18446744073709551616".parse::<BigUint>().unwrap(), v.nat_beyond_int64);

		assert_eq!(55i8, v.i8_value55);
		assert_eq!(-55i8, v.i8_minus55);
		assert_eq!(55u8, v.u8_value55);
		assert_eq!(255u8, v.u8_value255);
		assert_eq!(55i16, v.i16_value55);
		assert_eq!(-55i16, v.i16_minus55);
		assert_eq!(55u16, v.u16_value55);
		assert_eq!(u16::MAX, v.u16_value_max);
		assert_eq!(55i32, v.i32_value55);
		assert_eq!(-55i32, v.i32_minus55);
		assert_eq!(55u32, v.u32_value55);
		assert_eq!(u32::MAX, v.u32_value_max);
		assert_eq!(55i64, v.i64_value55);
		assert_eq!(-55i64, v.i64_minus55);
		assert_eq!(55u64, v.u64_value55);
		assert_eq!(u64::MAX, v.u64_value_max);

		assert_eq!(55.0f32, v.f32_value55);
		assert_eq!(-55.0f32, v.f32_minus55);
		assert!((v.f32_nan.is_nan()));
		assert_eq!(f32::INFINITY, v.f32_inf);
		assert_eq!(f32::NEG_INFINITY, v.f32_minus_inf);

		assert_eq!(55.0f64, v.f64_value55);
		assert_eq!(-55.0f64, v.f64_minus55);
		assert!((v.f64_nan.is_nan()));
		assert_eq!(f64::INFINITY, v.f64_inf);
		assert_eq!(f64::NEG_INFINITY, v.f64_minus_inf);

		assert_eq!([1, 2, 3], &v.list_value[..]);

		assert_eq!(Some(4), v.option_some);
		assert_eq!(None, v.option_none);
		assert_eq!(Some(Some(4)), v.option2_some_some);
		assert_eq!(Some(None), v.option2_some_none);
		assert_eq!(None, v.option2_none);

		assert_eq!(Some(4), v.optional_field_some.as_ref().field);
		assert_eq!(None, v.optional_field_none.as_ref().field);

		assert_eq!(HashMap::from([ ("a".to_owned(), 1), ("b".to_owned(), 2) ]), v.dict_value);
		assert_eq!(HashMap::from([ ("a".to_owned(), 1), ("b".to_owned(), 2) ]), v.dict_field_value.field);

	}
}


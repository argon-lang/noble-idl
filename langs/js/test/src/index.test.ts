import { expect, test } from "vitest";
import * as nidlTest from "./index.js";
import { Option } from "@argon-lang/noble-idl-core";

test("Default value", () => {
	const vRes = nidlTest.DefaultValues.codec.decode({
		type: "constructor",
		name: "default-values",
		args: [],
		kwargs: new Map(),
	});

	if(!vRes.success) {
		throw new Error("Could not decode default values");
	}

	const v = vRes.value;


	expect(v.boolTrue).toBe(true);
	expect(v.boolFalse).toBe(false);
	expect(v.strValue).toBe("abc");
	expect(v.binaryValue).toStrictEqual(new Uint8Array([ 0xAB, 0xCD, 0xEF ]));

	expect(v.intValue55).toBe(55n);
	expect(v.intBeyondInt64).toBe(18446744073709551616n);
	expect(v.intMinus55).toBe(-55n);
	expect(v.intBeyondNegInt64).toBe(-18446744073709551617n);
	expect(v.natValue55).toBe(55n);
	expect(v.natBeyondInt64).toBe(18446744073709551616n);

	expect(v.i8Value55).toBe(55);
	expect(v.i8Minus55).toBe(-55);
	expect(v.u8Value55).toBe(55);
	expect(v.u8Value255).toBe(255);
	expect(v.i16Value55).toBe(55);
	expect(v.i16Minus55).toBe(-55);
	expect(v.u16Value55).toBe(55);
	expect(v.u16ValueMax).toBe(65535);
	expect(v.i32Value55).toBe(55);
	expect(v.i32Minus55).toBe(-55);
	expect(v.u32Value55).toBe(55);
	expect(v.u32ValueMax).toBe(4294967295);
	expect(v.i64Value55).toBe(55n);
	expect(v.i64Minus55).toBe(-55n);
	expect(v.u64Value55).toBe(55n);
	expect(v.u64ValueMax).toBe(18446744073709551615n);

	expect(v.f32Value55).toBe(55);
	expect(v.f32Minus55).toBe(-55);
	expect(v.f32Nan).toBeNaN();
	expect(v.f32Inf).toBe(Number.POSITIVE_INFINITY);
	expect(v.f32MinusInf).toBe(Number.NEGATIVE_INFINITY);

	expect(v.f64Value55).toBe(55);
	expect(v.f64Minus55).toBe(-55);
	expect(v.f64Nan).toBeNaN();
	expect(v.f64Inf).toBe(Number.POSITIVE_INFINITY);
	expect(v.f64MinusInf).toBe(Number.NEGATIVE_INFINITY);

	expect(v.listValue).toStrictEqual([ 1, 2, 3 ]);

	expect(v.optionSome).toBe(4);
	expect(v.optionNone).toBe(null);
	expect(v.option2SomeSome).toBe(4);
	expect(v.option2SomeNone).toBe(Option.some(null));
	expect(v.option2None).toBe(null);

	expect(v.optionalFieldSome.field).toBe(4);
	expect(v.optionalFieldNone.field).toBeUndefined();

	expect(v.dictValue).toStrictEqual(new Map([ ["a", 1], ["b", 2] ]));
	expect(v.dictFieldValue.field).toStrictEqual(new Map([ ["a", 1], ["b", 2] ]));

});

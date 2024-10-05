package dev.argon.nobleidl.test.tests;


import dev.argon.esexpr.ESExpr;
import dev.argon.nobleidl.example.DefaultValues;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

import java.math.BigInteger;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class DefaultValueTests {

	@Test
	public void checkDefaultValues() throws Exception {
		var v = DefaultValues.codec().decode(new ESExpr.Constructor("default-values", List.of(), Map.of()));

		assertTrue(v.boolTrue());
		assertFalse(v.boolFalse());
		assertEquals("abc", v.strValue());
		assertArrayEquals(new byte[] { (byte)0xAB, (byte)0xCD, (byte)0xEF }, v.binaryValue());

		assertEquals(BigInteger.valueOf(55), v.intValue55());
		assertEquals(new BigInteger("18446744073709551616"), v.intBeyondInt64());
		assertEquals(BigInteger.valueOf(-55), v.intMinus55());
		assertEquals(new BigInteger("-18446744073709551617"), v.intBeyondNegInt64());
		assertEquals(BigInteger.valueOf(55), v.natValue55());
		assertEquals(new BigInteger("18446744073709551616"), v.natBeyondInt64());

		assertEquals((byte)55, v.i8Value55());
		assertEquals((byte)-55, v.i8Minus55());
		assertEquals((byte)55, v.u8Value55());
		assertEquals((byte)255, v.u8Value255());
		assertEquals((short)55, v.i16Value55());
		assertEquals((short)-55, v.i16Minus55());
		assertEquals((short)55, v.u16Value55());
		assertEquals((short)65535, v.u16ValueMax());
		assertEquals(55, v.i32Value55());
		assertEquals(-55, v.i32Minus55());
		assertEquals(55, v.u32Value55());
		assertEquals((int)4294967295L, v.u32ValueMax());
		assertEquals(55L, v.i64Value55());
		assertEquals(-55L, v.i64Minus55());
		assertEquals(55L, v.u64Value55());
		assertEquals(new BigInteger("18446744073709551615").longValue(), v.u32ValueMax());

		assertEquals(55.0f, v.f32Value55());
		assertEquals(-55.0f, v.f32Minus55());
		assertTrue(Float.isNaN(v.f32Nan()));
		assertEquals(Float.POSITIVE_INFINITY, v.f32Inf());
		assertEquals(Float.NEGATIVE_INFINITY, v.f32MinusInf());

		assertEquals(55.0, v.f64Value55());
		assertEquals(-55.0, v.f64Minus55());
		assertTrue(Double.isNaN(v.f32Nan()));
		assertEquals(Double.POSITIVE_INFINITY, v.f64Inf());
		assertEquals(Double.NEGATIVE_INFINITY, v.f64MinusInf());

		assertEquals(List.of(1, 2, 3), v.listValue());

		assertEquals(Optional.of(4), v.optionSome());
		assertEquals(Optional.empty(), v.optionNone());
		assertEquals(Optional.of(Optional.of(4)), v.option2SomeSome());
		assertEquals(Optional.of(Optional.empty()), v.option2SomeNone());
		assertEquals(Optional.empty(), v.option2None());

		assertEquals(Optional.of(4), v.optionalFieldSome().field());
		assertEquals(Optional.empty(), v.optionalFieldNone().field());

		assertEquals(Map.of("a", 1, "b", 2), v.dictValue().map());
		assertEquals(Map.of("a", 1, "b", 2), v.dictFieldValue().field().map());
	}

}

package nobleidl.test

import esexpr.*
import esexpr.unsigned.*
import zio.*
import zio.stream.*
import zio.test.Assertion.*
import zio.test.{TestExecutor as _, *}
import nobleidl.core.Binary


object DefaultValueTests extends ZIOSpecDefault {
  override def spec: Spec[TestEnvironment & Scope, Any] =
    suite("DefaultValueTests")(
      test("default values") {
        val vExpr = ESExpr.Constructor("default-values", Seq(), Map())
        val v = summon[ESExprCodec[DefaultValues]].decode(vExpr).toTry.get

        assertTrue(v.boolTrue) &&
          assertTrue(!v.boolFalse) &&
          assertTrue(v.strValue == "abc") &&
          assertTrue(v.binaryValue == Binary(IArray(0xAB.toByte, 0xCD.toByte, 0xEF.toByte))) &&
          assertTrue(v.intValue55 == 55) &&
          assertTrue(v.intBeyondInt64 == BigInt("18446744073709551616")) &&
          assertTrue(v.intMinus55 == -55) &&
          assertTrue(v.intBeyondNegInt64 == BigInt("-18446744073709551617")) &&
          assertTrue(v.natValue55 == 55) &&
          assertTrue(v.natBeyondInt64 == BigInt("18446744073709551616")) &&
          assertTrue(v.i8Value55 == 55.toByte) &&
          assertTrue(v.i8Minus55 == -55.toByte) &&
          assertTrue(v.u8Value55 == 55.toUByte) &&
          assertTrue(v.u8Value255 == UByte.MaxValue) &&
          assertTrue(v.i16Value55 == 55.toShort) &&
          assertTrue(v.i16Minus55 == -55.toShort) &&
          assertTrue(v.u16Value55 == 55.toUShort) &&
          assertTrue(v.u16ValueMax == UShort.MaxValue) &&
          assertTrue(v.i32Value55 == 55) &&
          assertTrue(v.i32Minus55 == -55) &&
          assertTrue(v.u32Value55 == 55.toUInt) &&
          assertTrue(v.u32ValueMax == UInt.MaxValue) &&
          assertTrue(v.i64Value55 == 55) &&
          assertTrue(v.i64Minus55 == -55) &&
          assertTrue(v.u64Value55 == 55.toULong) &&
          assertTrue(v.u64ValueMax == ULong.MaxValue) &&
          assertTrue(v.f32Value55 == 55.0f) &&
          assertTrue(v.f32Minus55 == -55.0f) &&
          assertTrue(v.f32Nan.isNaN) &&
          assertTrue(v.f32Inf.isPosInfinity) &&
          assertTrue(v.f32MinusInf.isNegInfinity) &&
          assertTrue(v.f64Value55 == 55.0) &&
          assertTrue(v.f64Minus55 == -55.0) &&
          assertTrue(v.f64Nan.isNaN) &&
          assertTrue(v.f64Inf.isPosInfinity) &&
          assertTrue(v.f64MinusInf.isNegInfinity) &&
          assertTrue(v.listValue == Seq(1, 2, 3)) &&
          assertTrue(v.optionalFieldSome.field.contains(4)) &&
          assertTrue(v.optionalFieldNone.field.isEmpty) &&
          assertTrue(v.dictValue.dict == Map("a" -> 1, "b" -> 2)) &&
          assertTrue(v.dictFieldValue.field.dict == Map("a" -> 1, "b" -> 2))
      }
    )
}

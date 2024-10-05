using System.Numerics;
using ESExpr.Runtime;
using NUnit.Framework.Legacy;

namespace NobleIDL.Tests;


public class DefaultValueTests {

    [Test]
    public void CheckDefaultValues() {
        var vExpr = new Expr.Constructor("default-values", [], default);
        IESExprCodec<DefaultValues> codec = new DefaultValues.Codec();
        var v = codec.Decode(vExpr);

        Assert.Multiple(() => {
            Assert.That(v.BoolTrue, Is.True);
            Assert.That(v.BoolFalse, Is.False);
            Assert.That(v.StrValue, Is.EqualTo("abc"));
            Assert.That(v.BinaryValue.ByteArray, Is.EqualTo(new byte[] { 0xAB, 0xCD, 0xEF }));
            
            Assert.That(v.IntValue55, Is.EqualTo((BigInteger)55));
            Assert.That(v.IntBeyondInt64, Is.EqualTo(BigInteger.Parse("18446744073709551616")));
            Assert.That(v.IntMinus55, Is.EqualTo((BigInteger)(-55)));
            Assert.That(v.IntBeyondNegInt64, Is.EqualTo(BigInteger.Parse("-18446744073709551617")));
            Assert.That(v.NatValue55.BigIntegerValue, Is.EqualTo((BigInteger)55));
            Assert.That(v.NatBeyondInt64.BigIntegerValue, Is.EqualTo(BigInteger.Parse("18446744073709551616")));
            
            Assert.That(v.I8Value55, Is.EqualTo((sbyte)55));
            Assert.That(v.I8Minus55, Is.EqualTo((sbyte)(-55)));
            Assert.That(v.U8Value55, Is.EqualTo((byte)55));
            Assert.That(v.U8Value255, Is.EqualTo(byte.MaxValue));
            Assert.That(v.I16Value55, Is.EqualTo((short)55));
            Assert.That(v.I16Minus55, Is.EqualTo((short)(-55)));
            Assert.That(v.U16Value55, Is.EqualTo((ushort)55));
            Assert.That(v.U16ValueMax, Is.EqualTo(ushort.MaxValue));
            Assert.That(v.I32Value55, Is.EqualTo(55));
            Assert.That(v.I32Minus55, Is.EqualTo(-55));
            Assert.That(v.U32Value55, Is.EqualTo(55U));
            Assert.That(v.U32ValueMax, Is.EqualTo(uint.MaxValue));
            Assert.That(v.I64Value55, Is.EqualTo(55L));
            Assert.That(v.I64Minus55, Is.EqualTo(-55L));
            Assert.That(v.U64Value55, Is.EqualTo(55UL));
            Assert.That(v.U64ValueMax, Is.EqualTo(ulong.MaxValue));
            
            Assert.That(v.F32Value55, Is.EqualTo(55.0f));
            Assert.That(v.F32Minus55, Is.EqualTo(-55.0f));
            Assert.That(v.F32Nan, Is.NaN);
            Assert.That(v.F32Inf, Is.EqualTo(float.PositiveInfinity));
            Assert.That(v.F32MinusInf, Is.EqualTo(float.NegativeInfinity));
            
            Assert.That(v.F64Value55, Is.EqualTo(55.0));
            Assert.That(v.F64Minus55, Is.EqualTo(-55.0));
            Assert.That(v.F64Nan, Is.NaN);
            Assert.That(v.F64Inf, Is.EqualTo(double.PositiveInfinity));
            Assert.That(v.F64MinusInf, Is.EqualTo(double.NegativeInfinity));
            
            Assert.That(v.ListValue, Is.EquivalentTo((VList<int>)[ 1, 2, 3 ]));
            
            Assert.That(v.OptionalFieldSome.Field.TryGetValue(out var value) && value == 4, Is.True);
            Assert.That(v.OptionalFieldNone.Field.IsSome, Is.False);
            
            Assert.That(v.DictValue, Is.EquivalentTo(new Dictionary<string, int> { ["a"] = 1, ["b"] = 2 }));
            Assert.That(v.DictFieldValue.Field, Is.EquivalentTo(new Dictionary<string, int> { ["a"] = 1, ["b"] = 2 }));
        });
    }
    
}
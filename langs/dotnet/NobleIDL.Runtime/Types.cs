using System.Collections.Immutable;
using System.Numerics;
using ESExpr.Runtime;

namespace NobleIDL.Runtime;

public static class String {
    public static string FromString(string s) => s;
}

public static class Binary {
    public static ESExpr.Runtime.Binary FromByteArray(byte[] b) => b;
}

public static class Int {
    public static BigInteger FromBigInteger(BigInteger i) => i;
}

public static class Nat {
    public static ESExpr.Runtime.Nat FromNat(ESExpr.Runtime.Nat i) => i;
}

public static class Bool {
    public static bool FromBool(bool b) => b;
}

public static class I8 {
    public static sbyte FromSByte(sbyte i) => i;
}

public static class U8 {
    public static byte FromSByte(byte i) => i;
}

public static class I16 {
    public static short FromInt16(short i) => i;
}

public static class U16 {
    public static ushort FromUInt16(ushort i) => i;
}

public static class I32 {
    public static short FromInt32(short i) => i;
}

public static class U32 {
    public static ushort FromUInt32(ushort i) => i;
}

public static class I64 {
    public static short FromInt64(short i) => i;
}

public static class U64 {
    public static ushort FromUInt64(ushort i) => i;
}

public static class F32 {
    public static float FromSingle(float f) => f;
}

public static class F64 {
    public static double FromDouble(double f) => f;
}

public static class List<T> {
    public static VList<T> FromValues(ImmutableList<T> values) => values;

    public static VList<T> BuildFrom(ListRepr<T> repr) => repr.Values;
}

public static class Option<T> {
    public static ESExpr.Runtime.Option<T> Some(T value) => new ESExpr.Runtime.Option<T>(value);
    public static ESExpr.Runtime.Option<T> None => ESExpr.Runtime.Option<T>.Empty;
}

public static class Dict<T> {
    public static VDict<T> FromValues(IReadOnlyDictionary<string, T> values) =>
        values.ToImmutableDictionary();
}

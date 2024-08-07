package dev.argon.nobleidl.compiler.api;
@dev.argon.esexpr.ESExprCodecGen
public sealed interface EsexprDecodedValue {
	@dev.argon.esexpr.Constructor("record")
	record Record(
		dev.argon.nobleidl.compiler.api.TypeExpr t,
		@dev.argon.esexpr.Vararg
		java.util.List<dev.argon.nobleidl.compiler.api.EsexprDecodedFieldValue> fields
	) implements dev.argon.nobleidl.compiler.api.EsexprDecodedValue {}
	@dev.argon.esexpr.Constructor("enum")
	record Enum(
		dev.argon.nobleidl.compiler.api.TypeExpr t,
		java.lang.String caseName,
		@dev.argon.esexpr.Vararg
		java.util.List<dev.argon.nobleidl.compiler.api.EsexprDecodedFieldValue> fields
	) implements dev.argon.nobleidl.compiler.api.EsexprDecodedValue {}
	@dev.argon.esexpr.Constructor("optional")
	record Optional(
		dev.argon.nobleidl.compiler.api.TypeExpr t,
		dev.argon.nobleidl.compiler.api.TypeExpr elementType,
		@dev.argon.esexpr.OptionalValue
		java.util.Optional<dev.argon.nobleidl.compiler.api.EsexprDecodedValue> value
	) implements dev.argon.nobleidl.compiler.api.EsexprDecodedValue {}
	@dev.argon.esexpr.Constructor("vararg")
	record Vararg(
		dev.argon.nobleidl.compiler.api.TypeExpr t,
		dev.argon.nobleidl.compiler.api.TypeExpr elementType,
		@dev.argon.esexpr.Vararg
		java.util.List<dev.argon.nobleidl.compiler.api.EsexprDecodedValue> values
	) implements dev.argon.nobleidl.compiler.api.EsexprDecodedValue {}
	@dev.argon.esexpr.Constructor("dict")
	record Dict(
		dev.argon.nobleidl.compiler.api.TypeExpr t,
		dev.argon.nobleidl.compiler.api.TypeExpr elementType,
		@dev.argon.esexpr.Dict
		dev.argon.esexpr.KeywordMapping<dev.argon.nobleidl.compiler.api.EsexprDecodedValue> values
	) implements dev.argon.nobleidl.compiler.api.EsexprDecodedValue {}
	@dev.argon.esexpr.Constructor("build-from")
	record BuildFrom(
		dev.argon.nobleidl.compiler.api.TypeExpr t,
		dev.argon.nobleidl.compiler.api.TypeExpr fromType,
		dev.argon.nobleidl.compiler.api.EsexprDecodedValue fromValue
	) implements dev.argon.nobleidl.compiler.api.EsexprDecodedValue {}
	@dev.argon.esexpr.Constructor("from-bool")
	record FromBool(
		dev.argon.nobleidl.compiler.api.TypeExpr t,
		boolean b
	) implements dev.argon.nobleidl.compiler.api.EsexprDecodedValue {}
	@dev.argon.esexpr.Constructor("from-int")
	record FromInt(
		dev.argon.nobleidl.compiler.api.TypeExpr t,
		java.math.BigInteger i,
		@dev.argon.esexpr.Keyword("min-int")
		@dev.argon.esexpr.OptionalValue
		java.util.Optional<java.math.BigInteger> minInt,
		@dev.argon.esexpr.Keyword("max-int")
		@dev.argon.esexpr.OptionalValue
		java.util.Optional<java.math.BigInteger> maxInt
	) implements dev.argon.nobleidl.compiler.api.EsexprDecodedValue {}
	@dev.argon.esexpr.Constructor("from-str")
	record FromStr(
		dev.argon.nobleidl.compiler.api.TypeExpr t,
		java.lang.String s
	) implements dev.argon.nobleidl.compiler.api.EsexprDecodedValue {}
	@dev.argon.esexpr.Constructor("from-binary")
	record FromBinary(
		dev.argon.nobleidl.compiler.api.TypeExpr t,
		byte[] b
	) implements dev.argon.nobleidl.compiler.api.EsexprDecodedValue {}
	@dev.argon.esexpr.Constructor("from-float32")
	record FromFloat32(
		dev.argon.nobleidl.compiler.api.TypeExpr t,
		float f
	) implements dev.argon.nobleidl.compiler.api.EsexprDecodedValue {}
	@dev.argon.esexpr.Constructor("from-float64")
	record FromFloat64(
		dev.argon.nobleidl.compiler.api.TypeExpr t,
		double f
	) implements dev.argon.nobleidl.compiler.api.EsexprDecodedValue {}
	@dev.argon.esexpr.Constructor("from-null")
	record FromNull(
		dev.argon.nobleidl.compiler.api.TypeExpr t
	) implements dev.argon.nobleidl.compiler.api.EsexprDecodedValue {}
	public static dev.argon.esexpr.ESExprCodec<dev.argon.nobleidl.compiler.api.EsexprDecodedValue> codec() {
		return dev.argon.nobleidl.compiler.api.EsexprDecodedValue_CodecImpl.INSTANCE;
	}
}

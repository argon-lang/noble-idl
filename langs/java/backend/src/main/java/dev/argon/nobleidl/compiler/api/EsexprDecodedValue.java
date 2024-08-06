package dev.argon.nobleidl.compiler.api;

import dev.argon.esexpr.*;

import java.math.BigInteger;
import java.util.List;
import java.util.Optional;

@ESExprCodecGen
public sealed interface EsexprDecodedValue {
	record Record(
		TypeExpr t,

		@dev.argon.esexpr.Vararg
		List<EsexprDecodedFieldValue> fields
	) implements EsexprDecodedValue {}

	record Enum(
		TypeExpr t,
		String caseName,

		@dev.argon.esexpr.Vararg
		List<EsexprDecodedFieldValue> fields
	) implements EsexprDecodedValue {}

	record Optional(
		TypeExpr t,
		TypeExpr elementType,

		@OptionalValue
		java.util.Optional<EsexprDecodedValue> value
	) implements EsexprDecodedValue {}

	record Vararg(
		TypeExpr t,
		TypeExpr elementType,

		@dev.argon.esexpr.Vararg
		List<EsexprDecodedValue> values
	) implements EsexprDecodedValue {}

	record Dict(
		TypeExpr t,
		TypeExpr elementType,

		@dev.argon.esexpr.Dict
		KeywordMapping<EsexprDecodedValue> values
	) implements EsexprDecodedValue {}

	record BuildFrom(
		TypeExpr t,
		TypeExpr fromType,
		EsexprDecodedValue fromValue
	) implements EsexprDecodedValue {}

	record FromBool(
		TypeExpr t,
		boolean b
	) implements EsexprDecodedValue {}

	record FromInt(
		TypeExpr t,
		BigInteger i,

		@Keyword
		@OptionalValue
		java.util.Optional<BigInteger> minInt,

		@Keyword
		@OptionalValue
		java.util.Optional<BigInteger> maxInt
	) implements EsexprDecodedValue {}

	record FromStr(
		TypeExpr t,
		String s
	) implements EsexprDecodedValue {}

	record FromBinary(
		TypeExpr t,
		byte[] b
	) implements EsexprDecodedValue {}

	record FromFloat32(
		TypeExpr t,
		float f
	) implements EsexprDecodedValue {}

	record FromFloat64(
		TypeExpr t,
		double f
	) implements EsexprDecodedValue {}

	record FromNull(
		TypeExpr t
	) implements EsexprDecodedValue {}


	public static ESExprCodec<EsexprDecodedValue> codec() {
		return EsexprDecodedValue_CodecImpl.INSTANCE;
	}
}

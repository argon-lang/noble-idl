package dev.argon.nobleidl.compiler.api;

import dev.argon.esexpr.ESExprCodec;
import dev.argon.esexpr.ESExprCodecGen;

@ESExprCodecGen
public sealed interface EsexprRecordKeywordMode {
	record Required() implements EsexprRecordKeywordMode {}

	record Optional(
		TypeExpr elementType
	) implements EsexprRecordKeywordMode {}

	record DefaultValue(
		EsexprDecodedValue value
	) implements EsexprRecordKeywordMode {}

	public static ESExprCodec<EsexprRecordKeywordMode> codec() {
		return EsexprRecordKeywordMode_CodecImpl.INSTANCE;
	}
}

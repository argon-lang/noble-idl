package dev.argon.nobleidl.compiler.api;

import dev.argon.esexpr.ESExprCodec;
import dev.argon.esexpr.ESExprCodecGen;

@ESExprCodecGen
public sealed interface EsexprRecordPositionalMode {
	record Required() implements EsexprRecordPositionalMode {}

	record Optional(
		TypeExpr elementType
	) implements EsexprRecordPositionalMode {}

	public static ESExprCodec<EsexprRecordPositionalMode> codec() {
		return EsexprRecordPositionalMode_CodecImpl.INSTANCE;
	}
}

package dev.argon.nobleidl.compiler.api;

import dev.argon.esexpr.ESExprCodec;
import dev.argon.esexpr.ESExprCodecGen;

@ESExprCodecGen
public sealed interface EsexprEnumCaseType {
	record Constructor(
		String name
	) implements EsexprEnumCaseType {}

	record InlineValue() implements EsexprEnumCaseType {}

	public static ESExprCodec<EsexprEnumCaseType> codec() {
		return EsexprEnumCaseType_CodecImpl.INSTANCE;
	}
}

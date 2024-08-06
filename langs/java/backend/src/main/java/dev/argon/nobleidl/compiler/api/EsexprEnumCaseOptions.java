package dev.argon.nobleidl.compiler.api;

import dev.argon.esexpr.Constructor;
import dev.argon.esexpr.ESExprCodec;
import dev.argon.esexpr.ESExprCodecGen;

@ESExprCodecGen
@Constructor("enum-case-options")
public record EsexprEnumCaseOptions(
	EsexprEnumCaseType caseType
) {
	public static ESExprCodec<EsexprEnumCaseOptions> codec() {
		return EsexprEnumCaseOptions_CodecImpl.INSTANCE;
	}
}

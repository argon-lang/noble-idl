package dev.argon.nobleidl.compiler.api;

import dev.argon.esexpr.ESExpr;
import dev.argon.esexpr.ESExprCodec;
import dev.argon.esexpr.ESExprCodecGen;

@ESExprCodecGen
public record Annotation(
	String scope,
	ESExpr value
) {
	public static ESExprCodec<Annotation> codec() {
		return Annotation_CodecImpl.INSTANCE;
	}
}

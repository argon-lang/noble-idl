package dev.argon.nobleidl.compiler.api;

import dev.argon.esexpr.ESExprCodec;
import dev.argon.esexpr.ESExprCodecGen;

@ESExprCodecGen
public record EsexprDecodedFieldValue(
	String name,
	EsexprDecodedValue value
) {
	public static ESExprCodec<EsexprDecodedFieldValue> codec() {
		return EsexprDecodedFieldValue_CodecImpl.INSTANCE;
	}
}

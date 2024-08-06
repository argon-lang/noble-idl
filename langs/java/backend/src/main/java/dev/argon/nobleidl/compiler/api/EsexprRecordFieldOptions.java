package dev.argon.nobleidl.compiler.api;

import dev.argon.esexpr.Constructor;
import dev.argon.esexpr.ESExprCodec;
import dev.argon.esexpr.ESExprCodecGen;

@ESExprCodecGen
@Constructor("field-options")
public record EsexprRecordFieldOptions(
	EsexprRecordFieldKind kind
) {
	public static ESExprCodec<EsexprRecordFieldOptions> codec() {
		return EsexprRecordFieldOptions_CodecImpl.INSTANCE;
	}
}

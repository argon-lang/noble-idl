package dev.argon.nobleidl.compiler.api;

import dev.argon.esexpr.Constructor;
import dev.argon.esexpr.ESExprCodec;
import dev.argon.esexpr.ESExprCodecGen;
import dev.argon.esexpr.Keyword;

@ESExprCodecGen
@Constructor("record-options")
public record EsexprRecordOptions(
	@Keyword
	String constructor
) {
	public static ESExprCodec<EsexprRecordOptions> codec() {
		return EsexprRecordOptions_CodecImpl.INSTANCE;
	}
}

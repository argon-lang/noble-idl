package dev.argon.nobleidl.compiler.api;

import dev.argon.esexpr.*;

@ESExprCodecGen
@Constructor("enum-options")
public record EsexprEnumOptions(
	@Keyword
	@DefaultValue("false")
	boolean simpleEnum
) {
	public static ESExprCodec<EsexprEnumOptions> codec() {
		return EsexprEnumOptions_CodecImpl.INSTANCE;
	}
}

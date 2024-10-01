package dev.argon.nobleidl.compiler;

import dev.argon.esexpr.*;

@ESExprCodecGen
public record JavaLanguageOptions(
	@Keyword
	PackageMapping packageMapping
) {
	public static ESExprCodec<JavaLanguageOptions> codec() {
		return JavaLanguageOptions_CodecImpl.INSTANCE;
	}
}

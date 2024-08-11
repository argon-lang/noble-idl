package dev.argon.nobleidl.compiler;

import dev.argon.esexpr.*;
import dev.argon.nobleidl.compiler.format.PackageMapping;

@ESExprCodecGen
public record JavaLanguageOptions(
	@Keyword
	String outputDir,

	@Keyword
	PackageMapping packageMapping
) {
	public static ESExprCodec<JavaLanguageOptions> codec() {
		return JavaLanguageOptions_CodecImpl.INSTANCE;
	}
}

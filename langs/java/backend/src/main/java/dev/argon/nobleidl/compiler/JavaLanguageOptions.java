package dev.argon.nobleidl.compiler;

import dev.argon.esexpr.*;

@ESExprCodecGen
public record JavaLanguageOptions(
	@Keyword
	PackageMapping packageMapping,
	@Keyword
	@DefaultValue("false")
	boolean generateGraalJSAdapters
) {
	public static ESExprCodec<JavaLanguageOptions> codec() {
		return JavaLanguageOptions_CodecImpl.INSTANCE;
	}
}

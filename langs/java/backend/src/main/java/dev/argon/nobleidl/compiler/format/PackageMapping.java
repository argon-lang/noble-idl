package dev.argon.nobleidl.compiler.format;

import dev.argon.esexpr.Dict;
import dev.argon.esexpr.ESExprCodec;
import dev.argon.esexpr.ESExprCodecGen;
import dev.argon.esexpr.KeywordMapping;

@ESExprCodecGen
public record PackageMapping(
	@Dict
	KeywordMapping<String> mapping
) {
	public static ESExprCodec<NobleIdlJarOptions> codec() {
		return NobleIdlJarOptions_CodecImpl.INSTANCE;
	}
}

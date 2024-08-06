package dev.argon.nobleidl.compiler;

import dev.argon.esexpr.*;

@ESExprCodecGen
public record PackageMapping(
	@Dict
	KeywordMapping<String> packageMapping
) {
	public static ESExprCodec<PackageMapping> codec() {
		return PackageMapping_CodecImpl.INSTANCE;
	}
}

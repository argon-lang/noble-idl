package dev.argon.nobleidl.compiler.format;

import dev.argon.esexpr.*;

@ESExprCodecGen
@Constructor("backends")
public record BackendMapping(
	@Dict
	KeywordMapping<BackendOptions> mapping
) {
	public static ESExprCodec<BackendMapping> codec() {
		return BackendMapping_CodecImpl.INSTANCE;
	}
}

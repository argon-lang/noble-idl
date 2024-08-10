package dev.argon.nobleidl.compiler.format;

import dev.argon.esexpr.*;

import java.util.List;

@ESExprCodecGen
@Constructor("backend-options")
public record BackendOptions(
	@Keyword
	KeywordMapping<String> packageMapping
) {
	public static ESExprCodec<BackendOptions> codec() {
		return BackendOptions_CodecImpl.INSTANCE;
	}
}

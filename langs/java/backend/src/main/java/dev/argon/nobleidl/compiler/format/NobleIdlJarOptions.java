package dev.argon.nobleidl.compiler.format;

import dev.argon.esexpr.*;

import java.util.List;

@ESExprCodecGen
@Constructor("noble-idl-options")
public record NobleIdlJarOptions(
	@Keyword
	List<String> backends,

	@Keyword
	List<String> idlFiles,

	@Keyword
	KeywordMapping<String> packageMapping
) {
	public static ESExprCodec<NobleIdlJarOptions> codec() {
		return NobleIdlJarOptions_CodecImpl.INSTANCE;
	}
}

package dev.argon.nobleidl.compiler.format;

import dev.argon.esexpr.*;

import java.util.List;

@ESExprCodecGen
@Constructor("noble-idl-options")
public record NobleIdlJarOptions(
	@Keyword
	List<String> idlFiles,

	@Keyword
	BackendMapping backends
) {
	public static ESExprCodec<NobleIdlJarOptions> codec() {
		return NobleIdlJarOptions_CodecImpl.INSTANCE;
	}
}

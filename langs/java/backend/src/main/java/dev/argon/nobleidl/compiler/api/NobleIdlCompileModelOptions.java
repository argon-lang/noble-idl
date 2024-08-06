package dev.argon.nobleidl.compiler.api;

import dev.argon.esexpr.Constructor;
import dev.argon.esexpr.ESExprCodec;
import dev.argon.esexpr.ESExprCodecGen;
import dev.argon.esexpr.Keyword;

import java.util.List;

@ESExprCodecGen
@Constructor("options")
public record NobleIdlCompileModelOptions(
	@Keyword
	List<String> libraryFiles,

	@Keyword
	List<String> files
) {
	public static ESExprCodec<NobleIdlCompileModelOptions> codec() {
		return NobleIdlCompileModelOptions_CodecImpl.INSTANCE;
	}
}

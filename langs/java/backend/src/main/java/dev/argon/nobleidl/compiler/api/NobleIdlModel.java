package dev.argon.nobleidl.compiler.api;

import dev.argon.esexpr.ESExprCodec;
import dev.argon.esexpr.ESExprCodecGen;
import dev.argon.esexpr.Keyword;

import java.util.List;

@ESExprCodecGen
public record NobleIdlModel(
	@Keyword
	List<DefinitionInfo> definitions
) {
	public static ESExprCodec<NobleIdlModel> codec() {
		return NobleIdlModel_CodecImpl.INSTANCE;
	}
}

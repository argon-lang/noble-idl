package dev.argon.nobleidl.compiler.api;

import dev.argon.esexpr.*;

import java.util.Optional;

@ESExprCodecGen
public record ExternTypeDefinition(
	@Keyword
	@OptionalValue
	Optional<EsexprExternTypeOptions> esexprOptions
) {
	public static ESExprCodec<ExternTypeDefinition> codec() {
		return ExternTypeDefinition_CodecImpl.INSTANCE;
	}
}

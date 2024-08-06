package dev.argon.nobleidl.compiler.api;

import dev.argon.esexpr.*;

import java.util.List;
import java.util.Optional;

@ESExprCodecGen
public record EnumDefinition(
	@Vararg
	List<EnumCase> cases,

	@Keyword
	@OptionalValue
	Optional<EsexprEnumOptions> esexprOptions
) {
	public static ESExprCodec<EnumDefinition> codec() {
		return EnumDefinition_CodecImpl.INSTANCE;
	}
}

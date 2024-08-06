package dev.argon.nobleidl.compiler.api;

import dev.argon.esexpr.*;

import java.util.List;
import java.util.Optional;

@ESExprCodecGen
public record EnumCase(
	String name,

	@Vararg
	List<RecordField> fields,

	@Keyword
	@OptionalValue
	Optional<EsexprEnumCaseOptions> esexprOptions,

	@Keyword
	List<Annotation> annotations
) {
	public static ESExprCodec<EnumCase> codec() {
		return EnumCase_CodecImpl.INSTANCE;
	}
}

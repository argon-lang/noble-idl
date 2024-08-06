package dev.argon.nobleidl.compiler.api;

import dev.argon.esexpr.*;

import java.util.List;
import java.util.Optional;

@ESExprCodecGen
public record RecordDefinition(
	@Vararg
	List<RecordField> fields,

	@Keyword
	@OptionalValue
	Optional<EsexprRecordOptions> esexprOptions
) {
	public static ESExprCodec<RecordDefinition> codec() {
		return RecordDefinition_CodecImpl.INSTANCE;
	}
}

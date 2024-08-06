package dev.argon.nobleidl.compiler.api;

import dev.argon.esexpr.*;

import java.util.List;
import java.util.Optional;

@ESExprCodecGen
public record RecordField(
	String name,
	TypeExpr fieldType,

	@Keyword
	List<Annotation> annotations,

	@Keyword
	@OptionalValue
	Optional<EsexprRecordFieldOptions> esexprOptions
) {
	public static ESExprCodec<RecordField> codec() {
		return RecordField_CodecImpl.INSTANCE;
	}
}

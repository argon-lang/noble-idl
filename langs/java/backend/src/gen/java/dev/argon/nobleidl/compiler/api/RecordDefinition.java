package dev.argon.nobleidl.compiler.api;
@dev.argon.esexpr.ESExprCodecGen
@dev.argon.esexpr.Constructor("record-definition")
public record RecordDefinition(
	@dev.argon.esexpr.Vararg
	java.util.List<dev.argon.nobleidl.compiler.api.RecordField> fields,
	@dev.argon.esexpr.Keyword("esexpr-options")
	@dev.argon.esexpr.OptionalValue
	java.util.Optional<dev.argon.nobleidl.compiler.api.EsexprRecordOptions> esexprOptions
) {
	public static dev.argon.esexpr.ESExprCodec<dev.argon.nobleidl.compiler.api.RecordDefinition> codec() {
		return dev.argon.nobleidl.compiler.api.RecordDefinition_CodecImpl.INSTANCE;
	}
}

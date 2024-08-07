package dev.argon.nobleidl.compiler.api;
@dev.argon.esexpr.ESExprCodecGen
@dev.argon.esexpr.Constructor("record-field")
public record RecordField(
	java.lang.String name,
	dev.argon.nobleidl.compiler.api.TypeExpr fieldType,
	@dev.argon.esexpr.Keyword("annotations")
	java.util.List<dev.argon.nobleidl.compiler.api.Annotation> annotations,
	@dev.argon.esexpr.Keyword("esexpr-options")
	@dev.argon.esexpr.OptionalValue
	java.util.Optional<dev.argon.nobleidl.compiler.api.EsexprRecordFieldOptions> esexprOptions
) {
	public static dev.argon.esexpr.ESExprCodec<dev.argon.nobleidl.compiler.api.RecordField> codec() {
		return dev.argon.nobleidl.compiler.api.RecordField_CodecImpl.INSTANCE;
	}
}

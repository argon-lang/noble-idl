package dev.argon.nobleidl.compiler.api;
@dev.argon.esexpr.ESExprCodecGen
@dev.argon.esexpr.Constructor("record-field")
public record RecordField(
	java.lang.@org.jetbrains.annotations.NotNull String name,
	dev.argon.nobleidl.compiler.api.@org.jetbrains.annotations.NotNull TypeExpr fieldType,
	@dev.argon.esexpr.Keyword("annotations")
	java.util.@org.jetbrains.annotations.NotNull List<dev.argon.nobleidl.compiler.api.Annotation> annotations,
	@dev.argon.esexpr.Keyword("esexpr-options")
	@dev.argon.esexpr.OptionalValue
	java.util.@org.jetbrains.annotations.NotNull Optional<dev.argon.nobleidl.compiler.api.EsexprRecordFieldOptions> esexprOptions
) {
	public static dev.argon.esexpr.ESExprCodec<dev.argon.nobleidl.compiler.api.RecordField> codec() {
		return dev.argon.nobleidl.compiler.api.RecordField_CodecImpl.INSTANCE;
	}
}

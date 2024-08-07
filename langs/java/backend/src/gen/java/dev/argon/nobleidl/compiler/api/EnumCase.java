package dev.argon.nobleidl.compiler.api;
@dev.argon.esexpr.ESExprCodecGen
@dev.argon.esexpr.Constructor("enum-case")
public record EnumCase(
	java.lang.String name,
	@dev.argon.esexpr.Vararg
	java.util.List<dev.argon.nobleidl.compiler.api.RecordField> fields,
	@dev.argon.esexpr.Keyword("esexpr-options")
	@dev.argon.esexpr.OptionalValue
	java.util.Optional<dev.argon.nobleidl.compiler.api.EsexprEnumCaseOptions> esexprOptions,
	@dev.argon.esexpr.Keyword("annotations")
	java.util.List<dev.argon.nobleidl.compiler.api.Annotation> annotations
) {
	public static dev.argon.esexpr.ESExprCodec<dev.argon.nobleidl.compiler.api.EnumCase> codec() {
		return dev.argon.nobleidl.compiler.api.EnumCase_CodecImpl.INSTANCE;
	}
}

package dev.argon.nobleidl.compiler.api;
@dev.argon.esexpr.ESExprCodecGen
@dev.argon.esexpr.Constructor("simple-enum-case")
public record SimpleEnumCase(
	java.lang.@org.jetbrains.annotations.NotNull String name,
	@dev.argon.esexpr.Keyword("esexpr-options")
	@dev.argon.esexpr.OptionalValue
	java.util.@org.jetbrains.annotations.NotNull Optional<dev.argon.nobleidl.compiler.api.EsexprSimpleEnumCaseOptions> esexprOptions,
	@dev.argon.esexpr.Keyword("annotations")
	java.util.@org.jetbrains.annotations.NotNull List<dev.argon.nobleidl.compiler.api.Annotation> annotations
) {
	public static dev.argon.esexpr.ESExprCodec<dev.argon.nobleidl.compiler.api.SimpleEnumCase> codec() {
		return dev.argon.nobleidl.compiler.api.SimpleEnumCase_CodecImpl.INSTANCE;
	}
}

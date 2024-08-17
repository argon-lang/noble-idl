package dev.argon.nobleidl.compiler.api;
@dev.argon.esexpr.ESExprCodecGen
@dev.argon.esexpr.Constructor("simple-enum-definition")
public record SimpleEnumDefinition(
	@dev.argon.esexpr.Vararg
	java.util.@org.jetbrains.annotations.NotNull List<dev.argon.nobleidl.compiler.api.SimpleEnumCase> cases,
	@dev.argon.esexpr.Keyword("esexpr-options")
	@dev.argon.esexpr.OptionalValue
	java.util.@org.jetbrains.annotations.NotNull Optional<dev.argon.nobleidl.compiler.api.EsexprSimpleEnumOptions> esexprOptions
) {
	public static dev.argon.esexpr.ESExprCodec<dev.argon.nobleidl.compiler.api.SimpleEnumDefinition> codec() {
		return dev.argon.nobleidl.compiler.api.SimpleEnumDefinition_CodecImpl.INSTANCE;
	}
}

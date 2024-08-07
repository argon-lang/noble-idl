package dev.argon.nobleidl.compiler.api;
@dev.argon.esexpr.ESExprCodecGen
@dev.argon.esexpr.Constructor("enum-definition")
public record EnumDefinition(
	@dev.argon.esexpr.Vararg
	java.util.List<dev.argon.nobleidl.compiler.api.EnumCase> cases,
	@dev.argon.esexpr.Keyword("esexpr-options")
	@dev.argon.esexpr.OptionalValue
	java.util.Optional<dev.argon.nobleidl.compiler.api.EsexprEnumOptions> esexprOptions
) {
	public static dev.argon.esexpr.ESExprCodec<dev.argon.nobleidl.compiler.api.EnumDefinition> codec() {
		return dev.argon.nobleidl.compiler.api.EnumDefinition_CodecImpl.INSTANCE;
	}
}

package dev.argon.nobleidl.compiler.api;
@dev.argon.esexpr.ESExprCodecGen
@dev.argon.esexpr.Constructor("extern-type-definition")
public record ExternTypeDefinition(
	@dev.argon.esexpr.Keyword("esexpr-options")
	@dev.argon.esexpr.OptionalValue
	java.util.@org.jetbrains.annotations.NotNull Optional<dev.argon.nobleidl.compiler.api.EsexprExternTypeOptions> esexprOptions
) {
	public static dev.argon.esexpr.ESExprCodec<dev.argon.nobleidl.compiler.api.ExternTypeDefinition> codec() {
		return dev.argon.nobleidl.compiler.api.ExternTypeDefinition_CodecImpl.INSTANCE;
	}
}

package dev.argon.nobleidl.compiler.api;
@dev.argon.esexpr.ESExprCodecGen
@dev.argon.esexpr.Constructor("extern-type-options")
public record EsexprExternTypeOptions(
	@dev.argon.esexpr.Keyword("allow-value")
	@dev.argon.esexpr.DefaultValue("dev.argon.nobleidl.runtime.Bool.fromBoolean(false)")
	@org.jetbrains.annotations.NotNull boolean allowValue,
	@dev.argon.esexpr.Keyword("allow-optional")
	@dev.argon.esexpr.OptionalValue
	java.util.@org.jetbrains.annotations.NotNull Optional<dev.argon.nobleidl.compiler.api.TypeExpr> allowOptional,
	@dev.argon.esexpr.Keyword("allow-vararg")
	@dev.argon.esexpr.OptionalValue
	java.util.@org.jetbrains.annotations.NotNull Optional<dev.argon.nobleidl.compiler.api.TypeExpr> allowVararg,
	@dev.argon.esexpr.Keyword("allow-dict")
	@dev.argon.esexpr.OptionalValue
	java.util.@org.jetbrains.annotations.NotNull Optional<dev.argon.nobleidl.compiler.api.TypeExpr> allowDict,
	@dev.argon.esexpr.Keyword("literals")
	dev.argon.nobleidl.compiler.api.@org.jetbrains.annotations.NotNull EsexprExternTypeLiterals literals
) {
	public static dev.argon.esexpr.ESExprCodec<dev.argon.nobleidl.compiler.api.EsexprExternTypeOptions> codec() {
		return dev.argon.nobleidl.compiler.api.EsexprExternTypeOptions_CodecImpl.INSTANCE;
	}
}

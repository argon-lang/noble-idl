package dev.argon.nobleidl.compiler.api;
@dev.argon.esexpr.ESExprCodecGen
@dev.argon.esexpr.Constructor("literals")
public record EsexprExternTypeLiterals(
	@dev.argon.esexpr.Keyword("allow-bool")
	@dev.argon.esexpr.DefaultValue("dev.argon.nobleidl.runtime.Bool.fromBoolean(false)")
	@org.jetbrains.annotations.NotNull boolean allowBool,
	@dev.argon.esexpr.Keyword("allow-int")
	@dev.argon.esexpr.DefaultValue("dev.argon.nobleidl.runtime.Bool.fromBoolean(false)")
	@org.jetbrains.annotations.NotNull boolean allowInt,
	@dev.argon.esexpr.Keyword("min-int")
	@dev.argon.esexpr.OptionalValue
	java.util.@org.jetbrains.annotations.NotNull Optional<java.math.BigInteger> minInt,
	@dev.argon.esexpr.Keyword("max-int")
	@dev.argon.esexpr.OptionalValue
	java.util.@org.jetbrains.annotations.NotNull Optional<java.math.BigInteger> maxInt,
	@dev.argon.esexpr.Keyword("allow-str")
	@dev.argon.esexpr.DefaultValue("dev.argon.nobleidl.runtime.Bool.fromBoolean(false)")
	@org.jetbrains.annotations.NotNull boolean allowStr,
	@dev.argon.esexpr.Keyword("allow-binary")
	@dev.argon.esexpr.DefaultValue("dev.argon.nobleidl.runtime.Bool.fromBoolean(false)")
	@org.jetbrains.annotations.NotNull boolean allowBinary,
	@dev.argon.esexpr.Keyword("allow-float32")
	@dev.argon.esexpr.DefaultValue("dev.argon.nobleidl.runtime.Bool.fromBoolean(false)")
	@org.jetbrains.annotations.NotNull boolean allowFloat32,
	@dev.argon.esexpr.Keyword("allow-float64")
	@dev.argon.esexpr.DefaultValue("dev.argon.nobleidl.runtime.Bool.fromBoolean(false)")
	@org.jetbrains.annotations.NotNull boolean allowFloat64,
	@dev.argon.esexpr.Keyword("allow-null")
	@dev.argon.esexpr.DefaultValue("dev.argon.nobleidl.runtime.Bool.fromBoolean(false)")
	@org.jetbrains.annotations.NotNull boolean allowNull,
	@dev.argon.esexpr.Keyword("null-max-level")
	@dev.argon.esexpr.OptionalValue
	java.util.@org.jetbrains.annotations.NotNull Optional<java.math.@dev.argon.esexpr.Unsigned BigInteger> nullMaxLevel,
	@dev.argon.esexpr.Keyword("build-literal-from")
	@dev.argon.esexpr.OptionalValue
	java.util.@org.jetbrains.annotations.NotNull Optional<dev.argon.nobleidl.compiler.api.TypeExpr> buildLiteralFrom,
	@dev.argon.esexpr.Keyword("build-literal-from-adjust-null")
	@dev.argon.esexpr.DefaultValue("dev.argon.nobleidl.runtime.Bool.fromBoolean(false)")
	@org.jetbrains.annotations.NotNull boolean buildLiteralFromAdjustNull
) {
	public static dev.argon.esexpr.ESExprCodec<dev.argon.nobleidl.compiler.api.EsexprExternTypeLiterals> codec() {
		return dev.argon.nobleidl.compiler.api.EsexprExternTypeLiterals_CodecImpl.INSTANCE;
	}
}

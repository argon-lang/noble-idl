package dev.argon.nobleidl.compiler.api;
@dev.argon.esexpr.ESExprCodecGen
@dev.argon.esexpr.Constructor("literals")
public record EsexprExternTypeLiterals(
	@dev.argon.esexpr.Keyword("allow-bool")
	@dev.argon.esexpr.DefaultValue("dev.argon.nobleidl.runtime.Bool.fromBoolean(false)")
	boolean allowBool,
	@dev.argon.esexpr.Keyword("allow-int")
	@dev.argon.esexpr.DefaultValue("dev.argon.nobleidl.runtime.Bool.fromBoolean(false)")
	boolean allowInt,
	@dev.argon.esexpr.Keyword("min-int")
	@dev.argon.esexpr.OptionalValue
	java.util.Optional<java.math.BigInteger> minInt,
	@dev.argon.esexpr.Keyword("max-int")
	@dev.argon.esexpr.OptionalValue
	java.util.Optional<java.math.BigInteger> maxInt,
	@dev.argon.esexpr.Keyword("allow-str")
	@dev.argon.esexpr.DefaultValue("dev.argon.nobleidl.runtime.Bool.fromBoolean(false)")
	boolean allowStr,
	@dev.argon.esexpr.Keyword("allow-binary")
	@dev.argon.esexpr.DefaultValue("dev.argon.nobleidl.runtime.Bool.fromBoolean(false)")
	boolean allowBinary,
	@dev.argon.esexpr.Keyword("allow-float32")
	@dev.argon.esexpr.DefaultValue("dev.argon.nobleidl.runtime.Bool.fromBoolean(false)")
	boolean allowFloat32,
	@dev.argon.esexpr.Keyword("allow-float64")
	@dev.argon.esexpr.DefaultValue("dev.argon.nobleidl.runtime.Bool.fromBoolean(false)")
	boolean allowFloat64,
	@dev.argon.esexpr.Keyword("allow-null")
	@dev.argon.esexpr.DefaultValue("dev.argon.nobleidl.runtime.Bool.fromBoolean(false)")
	boolean allowNull,
	@dev.argon.esexpr.Keyword("build-literal-from")
	@dev.argon.esexpr.OptionalValue
	java.util.Optional<dev.argon.nobleidl.compiler.api.TypeExpr> buildLiteralFrom
) {
	public static dev.argon.esexpr.ESExprCodec<dev.argon.nobleidl.compiler.api.EsexprExternTypeLiterals> codec() {
		return dev.argon.nobleidl.compiler.api.EsexprExternTypeLiterals_CodecImpl.INSTANCE;
	}
}

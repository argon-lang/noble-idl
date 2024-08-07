package dev.argon.nobleidl.compiler.api;
@dev.argon.esexpr.ESExprCodecGen
@dev.argon.esexpr.Constructor("enum-options")
public record EsexprEnumOptions(
	@dev.argon.esexpr.Keyword("simple-enum")
	@dev.argon.esexpr.DefaultValue("dev.argon.nobleidl.runtime.Bool.fromBoolean(false)")
	boolean simpleEnum
) {
	public static dev.argon.esexpr.ESExprCodec<dev.argon.nobleidl.compiler.api.EsexprEnumOptions> codec() {
		return dev.argon.nobleidl.compiler.api.EsexprEnumOptions_CodecImpl.INSTANCE;
	}
}

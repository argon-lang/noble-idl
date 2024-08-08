package dev.argon.nobleidl.compiler.api;
@dev.argon.esexpr.ESExprCodecGen
@dev.argon.esexpr.Constructor("enum-options")
public record EsexprEnumOptions(

) {
	public static dev.argon.esexpr.ESExprCodec<dev.argon.nobleidl.compiler.api.EsexprEnumOptions> codec() {
		return dev.argon.nobleidl.compiler.api.EsexprEnumOptions_CodecImpl.INSTANCE;
	}
}

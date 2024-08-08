package dev.argon.nobleidl.compiler.api;
@dev.argon.esexpr.ESExprCodecGen
@dev.argon.esexpr.Constructor("simple-enum-options")
public record EsexprSimpleEnumOptions(

) {
	public static dev.argon.esexpr.ESExprCodec<dev.argon.nobleidl.compiler.api.EsexprSimpleEnumOptions> codec() {
		return dev.argon.nobleidl.compiler.api.EsexprSimpleEnumOptions_CodecImpl.INSTANCE;
	}
}

package dev.argon.nobleidl.compiler.api;
@dev.argon.esexpr.ESExprCodecGen
@dev.argon.esexpr.Constructor("simple-enum-case-options")
public record EsexprSimpleEnumCaseOptions(
	java.lang.String name
) {
	public static dev.argon.esexpr.ESExprCodec<dev.argon.nobleidl.compiler.api.EsexprSimpleEnumCaseOptions> codec() {
		return dev.argon.nobleidl.compiler.api.EsexprSimpleEnumCaseOptions_CodecImpl.INSTANCE;
	}
}

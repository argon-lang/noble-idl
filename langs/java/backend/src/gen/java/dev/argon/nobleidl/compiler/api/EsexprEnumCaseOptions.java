package dev.argon.nobleidl.compiler.api;
@dev.argon.esexpr.ESExprCodecGen
@dev.argon.esexpr.Constructor("enum-case-options")
public record EsexprEnumCaseOptions(
	dev.argon.nobleidl.compiler.api.@org.jetbrains.annotations.NotNull EsexprEnumCaseType caseType
) {
	public static dev.argon.esexpr.ESExprCodec<dev.argon.nobleidl.compiler.api.EsexprEnumCaseOptions> codec() {
		return dev.argon.nobleidl.compiler.api.EsexprEnumCaseOptions_CodecImpl.INSTANCE;
	}
}

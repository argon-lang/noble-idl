package dev.argon.nobleidl.compiler.api;
@dev.argon.esexpr.ESExprCodecGen
public sealed interface EsexprEnumCaseType {
	@dev.argon.esexpr.Constructor("constructor")
	record Constructor(
		java.lang.@org.jetbrains.annotations.NotNull String name
	) implements dev.argon.nobleidl.compiler.api.EsexprEnumCaseType {}
	@dev.argon.esexpr.Constructor("inline-value")
	record InlineValue(

	) implements dev.argon.nobleidl.compiler.api.EsexprEnumCaseType {}
	public static dev.argon.esexpr.ESExprCodec<dev.argon.nobleidl.compiler.api.EsexprEnumCaseType> codec() {
		return dev.argon.nobleidl.compiler.api.EsexprEnumCaseType_CodecImpl.INSTANCE;
	}
}

package dev.argon.nobleidl.compiler.api;
@dev.argon.esexpr.ESExprCodecGen
public sealed interface EsexprRecordPositionalMode {
	@dev.argon.esexpr.Constructor("required")
	record Required(

	) implements dev.argon.nobleidl.compiler.api.EsexprRecordPositionalMode {}
	@dev.argon.esexpr.Constructor("optional")
	record Optional(
		dev.argon.nobleidl.compiler.api.@org.jetbrains.annotations.NotNull TypeExpr elementType
	) implements dev.argon.nobleidl.compiler.api.EsexprRecordPositionalMode {}
	public static dev.argon.esexpr.ESExprCodec<dev.argon.nobleidl.compiler.api.EsexprRecordPositionalMode> codec() {
		return dev.argon.nobleidl.compiler.api.EsexprRecordPositionalMode_CodecImpl.INSTANCE;
	}
}

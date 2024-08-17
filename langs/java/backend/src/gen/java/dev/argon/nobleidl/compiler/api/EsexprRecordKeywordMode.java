package dev.argon.nobleidl.compiler.api;
@dev.argon.esexpr.ESExprCodecGen
public sealed interface EsexprRecordKeywordMode {
	@dev.argon.esexpr.Constructor("required")
	record Required(

	) implements dev.argon.nobleidl.compiler.api.EsexprRecordKeywordMode {}
	@dev.argon.esexpr.Constructor("optional")
	record Optional(
		dev.argon.nobleidl.compiler.api.@org.jetbrains.annotations.NotNull TypeExpr elementType
	) implements dev.argon.nobleidl.compiler.api.EsexprRecordKeywordMode {}
	@dev.argon.esexpr.Constructor("default-value")
	record DefaultValue(
		dev.argon.nobleidl.compiler.api.@org.jetbrains.annotations.NotNull EsexprDecodedValue value
	) implements dev.argon.nobleidl.compiler.api.EsexprRecordKeywordMode {}
	public static dev.argon.esexpr.ESExprCodec<dev.argon.nobleidl.compiler.api.EsexprRecordKeywordMode> codec() {
		return dev.argon.nobleidl.compiler.api.EsexprRecordKeywordMode_CodecImpl.INSTANCE;
	}
}

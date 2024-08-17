package dev.argon.nobleidl.compiler.api;
@dev.argon.esexpr.ESExprCodecGen
@dev.argon.esexpr.Constructor("record-options")
public record EsexprRecordOptions(
	@dev.argon.esexpr.Keyword("constructor")
	java.lang.@org.jetbrains.annotations.NotNull String constructor
) {
	public static dev.argon.esexpr.ESExprCodec<dev.argon.nobleidl.compiler.api.EsexprRecordOptions> codec() {
		return dev.argon.nobleidl.compiler.api.EsexprRecordOptions_CodecImpl.INSTANCE;
	}
}

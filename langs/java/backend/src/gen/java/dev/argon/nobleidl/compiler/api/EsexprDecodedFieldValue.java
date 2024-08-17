package dev.argon.nobleidl.compiler.api;
@dev.argon.esexpr.ESExprCodecGen
@dev.argon.esexpr.Constructor("field-value")
public record EsexprDecodedFieldValue(
	java.lang.@org.jetbrains.annotations.NotNull String name,
	dev.argon.nobleidl.compiler.api.@org.jetbrains.annotations.NotNull EsexprDecodedValue value
) {
	public static dev.argon.esexpr.ESExprCodec<dev.argon.nobleidl.compiler.api.EsexprDecodedFieldValue> codec() {
		return dev.argon.nobleidl.compiler.api.EsexprDecodedFieldValue_CodecImpl.INSTANCE;
	}
}

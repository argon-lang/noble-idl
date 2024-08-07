package dev.argon.nobleidl.compiler.api;
@dev.argon.esexpr.ESExprCodecGen
@dev.argon.esexpr.Constructor("field-value")
public record EsexprDecodedFieldValue(
	java.lang.String name,
	dev.argon.nobleidl.compiler.api.EsexprDecodedValue value
) {
	public static dev.argon.esexpr.ESExprCodec<dev.argon.nobleidl.compiler.api.EsexprDecodedFieldValue> codec() {
		return dev.argon.nobleidl.compiler.api.EsexprDecodedFieldValue_CodecImpl.INSTANCE;
	}
}

package dev.argon.nobleidl.compiler.api;
@dev.argon.esexpr.ESExprCodecGen
@dev.argon.esexpr.Constructor("field-options")
public record EsexprRecordFieldOptions(
	dev.argon.nobleidl.compiler.api.EsexprRecordFieldKind kind
) {
	public static dev.argon.esexpr.ESExprCodec<dev.argon.nobleidl.compiler.api.EsexprRecordFieldOptions> codec() {
		return dev.argon.nobleidl.compiler.api.EsexprRecordFieldOptions_CodecImpl.INSTANCE;
	}
}

package dev.argon.nobleidl.compiler.api;
@dev.argon.esexpr.ESExprCodecGen
@dev.argon.esexpr.Constructor("annotation")
public record Annotation(
	java.lang.String scope,
	dev.argon.esexpr.ESExpr value
) {
	public static dev.argon.esexpr.ESExprCodec<dev.argon.nobleidl.compiler.api.Annotation> codec() {
		return dev.argon.nobleidl.compiler.api.Annotation_CodecImpl.INSTANCE;
	}
}
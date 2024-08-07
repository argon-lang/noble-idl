package dev.argon.nobleidl.compiler.api;
@dev.argon.esexpr.ESExprCodecGen
@dev.argon.esexpr.Constructor("qualified-name")
public record QualifiedName(
	dev.argon.nobleidl.compiler.api.PackageName _package,
	java.lang.String name
) {
	public static dev.argon.esexpr.ESExprCodec<dev.argon.nobleidl.compiler.api.QualifiedName> codec() {
		return dev.argon.nobleidl.compiler.api.QualifiedName_CodecImpl.INSTANCE;
	}
}

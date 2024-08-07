package dev.argon.nobleidl.compiler.api;
@dev.argon.esexpr.ESExprCodecGen
@dev.argon.esexpr.Constructor("package-name")
public record PackageName(
	@dev.argon.esexpr.Vararg
	java.util.List<java.lang.String> parts
) {
	public static dev.argon.esexpr.ESExprCodec<dev.argon.nobleidl.compiler.api.PackageName> codec() {
		return dev.argon.nobleidl.compiler.api.PackageName_CodecImpl.INSTANCE;
	}
}

package dev.argon.nobleidl.compiler.format;
@dev.argon.esexpr.ESExprCodecGen
@dev.argon.esexpr.Constructor("backend-options")
public record BackendOptions(
	@dev.argon.esexpr.Keyword("package-mapping")
	dev.argon.nobleidl.compiler.format.@org.jetbrains.annotations.NotNull PackageMapping packageMapping
) {
	public static dev.argon.esexpr.ESExprCodec<dev.argon.nobleidl.compiler.format.BackendOptions> codec() {
		return dev.argon.nobleidl.compiler.format.BackendOptions_CodecImpl.INSTANCE;
	}
}

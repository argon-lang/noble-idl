package dev.argon.nobleidl.compiler.format;
@dev.argon.esexpr.ESExprCodecGen
@dev.argon.esexpr.Constructor("package-mapping")
public record PackageMapping(
	@dev.argon.esexpr.Dict
	dev.argon.esexpr.@org.jetbrains.annotations.NotNull KeywordMapping<java.lang.String> mapping
) {
	public static dev.argon.esexpr.ESExprCodec<dev.argon.nobleidl.compiler.format.PackageMapping> codec() {
		return dev.argon.nobleidl.compiler.format.PackageMapping_CodecImpl.INSTANCE;
	}
}

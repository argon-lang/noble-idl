package dev.argon.nobleidl.compiler.format;
@dev.argon.esexpr.ESExprCodecGen
@dev.argon.esexpr.Constructor("backends")
public record BackendMapping(
	@dev.argon.esexpr.Dict
	dev.argon.esexpr.@org.jetbrains.annotations.NotNull KeywordMapping<dev.argon.nobleidl.compiler.format.BackendOptions> mapping
) {
	public static dev.argon.esexpr.ESExprCodec<dev.argon.nobleidl.compiler.format.BackendMapping> codec() {
		return dev.argon.nobleidl.compiler.format.BackendMapping_CodecImpl.INSTANCE;
	}
}

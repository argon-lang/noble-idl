package dev.argon.nobleidl.compiler.format;
@dev.argon.esexpr.ESExprCodecGen
@dev.argon.esexpr.Constructor("noble-idl-options")
public record NobleIdlJarOptions(
	@dev.argon.esexpr.Keyword("idl-files")
	java.util.@org.jetbrains.annotations.NotNull List<java.lang.String> idlFiles,
	@dev.argon.esexpr.Keyword("backends")
	dev.argon.nobleidl.compiler.format.@org.jetbrains.annotations.NotNull BackendMapping backends
) {
	public static dev.argon.esexpr.ESExprCodec<dev.argon.nobleidl.compiler.format.NobleIdlJarOptions> codec() {
		return dev.argon.nobleidl.compiler.format.NobleIdlJarOptions_CodecImpl.INSTANCE;
	}
}

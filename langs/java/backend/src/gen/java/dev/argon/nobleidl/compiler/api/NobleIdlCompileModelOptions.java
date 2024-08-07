package dev.argon.nobleidl.compiler.api;
@dev.argon.esexpr.ESExprCodecGen
@dev.argon.esexpr.Constructor("options")
public record NobleIdlCompileModelOptions(
	@dev.argon.esexpr.Keyword("library-files")
	java.util.List<java.lang.String> libraryFiles,
	@dev.argon.esexpr.Keyword("files")
	java.util.List<java.lang.String> files
) {
	public static dev.argon.esexpr.ESExprCodec<dev.argon.nobleidl.compiler.api.NobleIdlCompileModelOptions> codec() {
		return dev.argon.nobleidl.compiler.api.NobleIdlCompileModelOptions_CodecImpl.INSTANCE;
	}
}

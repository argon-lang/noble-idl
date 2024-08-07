package dev.argon.nobleidl.compiler.api;
@dev.argon.esexpr.ESExprCodecGen
@dev.argon.esexpr.Constructor("noble-idl-generation-result")
public record NobleIdlGenerationResult(
	@dev.argon.esexpr.Keyword("generated-files")
	java.util.List<java.lang.String> generatedFiles
) {
	public static dev.argon.esexpr.ESExprCodec<dev.argon.nobleidl.compiler.api.NobleIdlGenerationResult> codec() {
		return dev.argon.nobleidl.compiler.api.NobleIdlGenerationResult_CodecImpl.INSTANCE;
	}
}

package dev.argon.nobleidl.compiler.api;
@dev.argon.esexpr.ESExprCodecGen
@dev.argon.esexpr.Constructor("noble-idl-generation-request")
public record NobleIdlGenerationRequest<L>(
	@dev.argon.esexpr.Keyword("language-options")
	@org.jetbrains.annotations.NotNull L languageOptions,
	@dev.argon.esexpr.Keyword("model")
	dev.argon.nobleidl.compiler.api.@org.jetbrains.annotations.NotNull NobleIdlModel model
) {
	public static <L> dev.argon.esexpr.ESExprCodec<dev.argon.nobleidl.compiler.api.NobleIdlGenerationRequest<L>> codec(dev.argon.esexpr.ESExprCodec<L> lCodec) {
		return new dev.argon.nobleidl.compiler.api.NobleIdlGenerationRequest_CodecImpl<L>(lCodec);
	}
}

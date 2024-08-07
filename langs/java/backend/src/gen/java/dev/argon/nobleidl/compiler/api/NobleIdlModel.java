package dev.argon.nobleidl.compiler.api;
@dev.argon.esexpr.ESExprCodecGen
@dev.argon.esexpr.Constructor("noble-idl-model")
public record NobleIdlModel(
	@dev.argon.esexpr.Keyword("definitions")
	java.util.List<dev.argon.nobleidl.compiler.api.DefinitionInfo> definitions
) {
	public static dev.argon.esexpr.ESExprCodec<dev.argon.nobleidl.compiler.api.NobleIdlModel> codec() {
		return dev.argon.nobleidl.compiler.api.NobleIdlModel_CodecImpl.INSTANCE;
	}
}

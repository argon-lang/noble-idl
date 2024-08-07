package dev.argon.nobleidl.compiler.api;
@dev.argon.esexpr.ESExprCodecGen
public sealed interface NobleIdlCompileModelResult {
	@dev.argon.esexpr.Constructor("success")
	record Success(
		dev.argon.nobleidl.compiler.api.NobleIdlModel model
	) implements dev.argon.nobleidl.compiler.api.NobleIdlCompileModelResult {}
	@dev.argon.esexpr.Constructor("failure")
	record Failure(
		@dev.argon.esexpr.Vararg
		java.util.List<java.lang.String> errors
	) implements dev.argon.nobleidl.compiler.api.NobleIdlCompileModelResult {}
	public static dev.argon.esexpr.ESExprCodec<dev.argon.nobleidl.compiler.api.NobleIdlCompileModelResult> codec() {
		return dev.argon.nobleidl.compiler.api.NobleIdlCompileModelResult_CodecImpl.INSTANCE;
	}
}

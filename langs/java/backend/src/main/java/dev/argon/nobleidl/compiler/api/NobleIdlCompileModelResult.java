package dev.argon.nobleidl.compiler.api;

import dev.argon.esexpr.ESExprCodec;
import dev.argon.esexpr.ESExprCodecGen;
import dev.argon.esexpr.Vararg;
import dev.argon.esexpr.VarargCodec;

import java.util.List;

@ESExprCodecGen
public sealed interface NobleIdlCompileModelResult {
	record Success(
		NobleIdlModel model
	) implements NobleIdlCompileModelResult {}

	record Failure(
		@Vararg
		List<String> errors
	) implements NobleIdlCompileModelResult {}

	public static ESExprCodec<NobleIdlCompileModelResult> codec() {
		return NobleIdlCompileModelResult_CodecImpl.INSTANCE;
	}
}

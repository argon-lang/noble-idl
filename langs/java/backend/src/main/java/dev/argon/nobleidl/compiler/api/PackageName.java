package dev.argon.nobleidl.compiler.api;

import dev.argon.esexpr.ESExprCodec;
import dev.argon.esexpr.ESExprCodecGen;
import dev.argon.esexpr.Vararg;
import dev.argon.esexpr.VarargCodec;

import java.util.List;

@ESExprCodecGen
public record PackageName(
	@Vararg
	List<String> parts
) {
	public static ESExprCodec<PackageName> codec() {
		return PackageName_CodecImpl.INSTANCE;
	}
}

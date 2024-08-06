package dev.argon.nobleidl.compiler.api;

import dev.argon.esexpr.ESExprCodec;
import dev.argon.esexpr.ESExprCodecGen;
import dev.argon.esexpr.Keyword;

import java.util.List;

@ESExprCodecGen
public sealed interface TypeParameter {
	record Type(
		String name,

		@Keyword
		List<Annotation> annotations
	) implements TypeParameter {}


	public static ESExprCodec<TypeParameter> codec() {
		return TypeParameter_CodecImpl.INSTANCE;
	}
}

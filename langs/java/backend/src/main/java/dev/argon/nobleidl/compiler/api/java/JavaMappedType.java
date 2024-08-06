package dev.argon.nobleidl.compiler.api.java;

import dev.argon.esexpr.ESExprCodec;
import dev.argon.esexpr.ESExprCodecGen;
import dev.argon.esexpr.InlineValue;
import dev.argon.esexpr.Vararg;

import java.util.List;

@ESExprCodecGen
public sealed interface JavaMappedType {
	@InlineValue
	record TypeName(String name) implements JavaMappedType {}

	record Apply(
		String name,

		@Vararg
		List<JavaMappedType> args
	) implements JavaMappedType {}

	record Annotated(
		JavaMappedType t,

		@Vararg
		List<String> annotations
	) implements JavaMappedType {}

	record TypeParameter(String name) implements JavaMappedType {}

	record Array(
		JavaMappedType elementType
	) implements JavaMappedType {}

	public static ESExprCodec<JavaMappedType> codec() {
		return JavaMappedType_CodecImpl.INSTANCE;
	}
}

package dev.argon.nobleidl.compiler.api;

import dev.argon.esexpr.ESExprCodec;
import dev.argon.esexpr.ESExprCodecGen;
import dev.argon.esexpr.InlineValue;

@ESExprCodecGen
public sealed interface Definition {
	@InlineValue
	record Record(
		RecordDefinition r
	) implements Definition {}

	@InlineValue
	record Enum(
		EnumDefinition e
	) implements Definition {}

	@InlineValue
	record ExternType(
		ExternTypeDefinition et
	) implements Definition {}

	@InlineValue
	record Interface(
		InterfaceDefinition r
	) implements Definition {}

	public static ESExprCodec<Definition> codec() {
		return Definition_CodecImpl.INSTANCE;
	}
}

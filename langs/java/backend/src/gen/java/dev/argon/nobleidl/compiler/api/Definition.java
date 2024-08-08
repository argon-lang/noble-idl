package dev.argon.nobleidl.compiler.api;
@dev.argon.esexpr.ESExprCodecGen
public sealed interface Definition {
	@dev.argon.esexpr.InlineValue
	record Record(
		dev.argon.nobleidl.compiler.api.RecordDefinition r
	) implements dev.argon.nobleidl.compiler.api.Definition {}
	@dev.argon.esexpr.InlineValue
	record Enum(
		dev.argon.nobleidl.compiler.api.EnumDefinition e
	) implements dev.argon.nobleidl.compiler.api.Definition {}
	@dev.argon.esexpr.InlineValue
	record SimpleEnum(
		dev.argon.nobleidl.compiler.api.SimpleEnumDefinition e
	) implements dev.argon.nobleidl.compiler.api.Definition {}
	@dev.argon.esexpr.InlineValue
	record ExternType(
		dev.argon.nobleidl.compiler.api.ExternTypeDefinition et
	) implements dev.argon.nobleidl.compiler.api.Definition {}
	@dev.argon.esexpr.InlineValue
	record Interface(
		dev.argon.nobleidl.compiler.api.InterfaceDefinition iface
	) implements dev.argon.nobleidl.compiler.api.Definition {}
	public static dev.argon.esexpr.ESExprCodec<dev.argon.nobleidl.compiler.api.Definition> codec() {
		return dev.argon.nobleidl.compiler.api.Definition_CodecImpl.INSTANCE;
	}
}

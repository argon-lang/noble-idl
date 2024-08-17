package dev.argon.nobleidl.compiler.api;
@dev.argon.esexpr.ESExprCodecGen
public sealed interface Definition {
	@dev.argon.esexpr.InlineValue
	record Record(
		dev.argon.nobleidl.compiler.api.@org.jetbrains.annotations.NotNull RecordDefinition r
	) implements dev.argon.nobleidl.compiler.api.Definition {}
	@dev.argon.esexpr.InlineValue
	record Enum(
		dev.argon.nobleidl.compiler.api.@org.jetbrains.annotations.NotNull EnumDefinition e
	) implements dev.argon.nobleidl.compiler.api.Definition {}
	@dev.argon.esexpr.InlineValue
	record SimpleEnum(
		dev.argon.nobleidl.compiler.api.@org.jetbrains.annotations.NotNull SimpleEnumDefinition e
	) implements dev.argon.nobleidl.compiler.api.Definition {}
	@dev.argon.esexpr.InlineValue
	record ExternType(
		dev.argon.nobleidl.compiler.api.@org.jetbrains.annotations.NotNull ExternTypeDefinition et
	) implements dev.argon.nobleidl.compiler.api.Definition {}
	@dev.argon.esexpr.InlineValue
	record Interface(
		dev.argon.nobleidl.compiler.api.@org.jetbrains.annotations.NotNull InterfaceDefinition iface
	) implements dev.argon.nobleidl.compiler.api.Definition {}
	public static dev.argon.esexpr.ESExprCodec<dev.argon.nobleidl.compiler.api.Definition> codec() {
		return dev.argon.nobleidl.compiler.api.Definition_CodecImpl.INSTANCE;
	}
}

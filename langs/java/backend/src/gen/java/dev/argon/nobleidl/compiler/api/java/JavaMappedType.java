package dev.argon.nobleidl.compiler.api.java;
@dev.argon.esexpr.ESExprCodecGen
public sealed interface JavaMappedType {
	@dev.argon.esexpr.InlineValue
	record TypeName(
		java.lang.@org.jetbrains.annotations.NotNull String name
	) implements dev.argon.nobleidl.compiler.api.java.JavaMappedType {}
	@dev.argon.esexpr.Constructor("apply")
	record Apply(
		java.lang.@org.jetbrains.annotations.NotNull String name,
		@dev.argon.esexpr.Vararg
		java.util.@org.jetbrains.annotations.NotNull List<dev.argon.nobleidl.compiler.api.java.JavaMappedType> args
	) implements dev.argon.nobleidl.compiler.api.java.JavaMappedType {}
	@dev.argon.esexpr.Constructor("annotated")
	record Annotated(
		dev.argon.nobleidl.compiler.api.java.@org.jetbrains.annotations.NotNull JavaMappedType t,
		@dev.argon.esexpr.Vararg
		java.util.@org.jetbrains.annotations.NotNull List<java.lang.String> annotations
	) implements dev.argon.nobleidl.compiler.api.java.JavaMappedType {}
	@dev.argon.esexpr.Constructor("type-parameter")
	record TypeParameter(
		java.lang.@org.jetbrains.annotations.NotNull String name
	) implements dev.argon.nobleidl.compiler.api.java.JavaMappedType {}
	@dev.argon.esexpr.Constructor("array")
	record Array(
		dev.argon.nobleidl.compiler.api.java.@org.jetbrains.annotations.NotNull JavaMappedType elementType
	) implements dev.argon.nobleidl.compiler.api.java.JavaMappedType {}
	public static dev.argon.esexpr.ESExprCodec<dev.argon.nobleidl.compiler.api.java.JavaMappedType> codec() {
		return dev.argon.nobleidl.compiler.api.java.JavaMappedType_CodecImpl.INSTANCE;
	}
}

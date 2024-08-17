package dev.argon.nobleidl.compiler.api.java;
@dev.argon.esexpr.ESExprCodecGen
public sealed interface JavaAnnExternType {
	@dev.argon.esexpr.Constructor("mapped-to")
	record MappedTo(
		dev.argon.nobleidl.compiler.api.java.@org.jetbrains.annotations.NotNull JavaMappedType javaType
	) implements dev.argon.nobleidl.compiler.api.java.JavaAnnExternType {}
	public static dev.argon.esexpr.ESExprCodec<dev.argon.nobleidl.compiler.api.java.JavaAnnExternType> codec() {
		return dev.argon.nobleidl.compiler.api.java.JavaAnnExternType_CodecImpl.INSTANCE;
	}
}

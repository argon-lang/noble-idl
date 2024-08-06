package dev.argon.nobleidl.compiler.api.java;

import dev.argon.esexpr.ESExprCodec;
import dev.argon.esexpr.ESExprCodecGen;

@ESExprCodecGen
public sealed interface JavaAnnExternType {
	record MappedTo(
		JavaMappedType javaType
	) implements JavaAnnExternType {}

	public static ESExprCodec<JavaAnnExternType> codec() {
		return JavaAnnExternType_CodecImpl.INSTANCE;
	}
}

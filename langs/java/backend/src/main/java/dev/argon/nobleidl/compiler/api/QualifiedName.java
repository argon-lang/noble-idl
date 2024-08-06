package dev.argon.nobleidl.compiler.api;

import dev.argon.esexpr.ESExprCodec;
import dev.argon.esexpr.ESExprCodecGen;

@ESExprCodecGen
public record QualifiedName(
	PackageName packageName,
	String name
) {
	public static ESExprCodec<QualifiedName> codec() {
		return QualifiedName_CodecImpl.INSTANCE;
	}
}

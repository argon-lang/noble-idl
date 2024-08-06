package dev.argon.nobleidl.compiler.api;

import dev.argon.esexpr.ESExprCodec;
import dev.argon.esexpr.ESExprCodecGen;
import dev.argon.esexpr.Keyword;

import java.util.List;

@ESExprCodecGen
public record DefinitionInfo(
	@Keyword
	QualifiedName name,

	@Keyword
	List<TypeParameter> typeParameters,

	@Keyword
	Definition definition,

	@Keyword
	List<Annotation> annotations,

	@Keyword
	boolean isLibrary
) {
	public static ESExprCodec<DefinitionInfo> codec() {
		return DefinitionInfo_CodecImpl.INSTANCE;
	}
}

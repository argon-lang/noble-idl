package dev.argon.nobleidl.compiler.api;

import dev.argon.esexpr.ESExprCodec;
import dev.argon.esexpr.ESExprCodecGen;
import dev.argon.esexpr.Keyword;

import java.util.List;

@ESExprCodecGen
public record InterfaceMethod(
	@Keyword
	String name,

	@Keyword
	List<TypeParameter> typeParameters,

	@Keyword
	List<InterfaceMethodParameter> parameters,

	@Keyword
	TypeExpr returnType,

	@Keyword
	List<Annotation> annotations
) {
	public static ESExprCodec<InterfaceMethod> codec() {
		return InterfaceMethod_CodecImpl.INSTANCE;
	}
}

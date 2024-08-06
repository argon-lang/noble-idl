package dev.argon.nobleidl.compiler.api;

import dev.argon.esexpr.ESExprCodec;
import dev.argon.esexpr.ESExprCodecGen;
import dev.argon.esexpr.Keyword;

import java.util.List;

@ESExprCodecGen
public record InterfaceMethodParameter(
	String name,
	TypeExpr parameterType,

	@Keyword
	List<Annotation> annotations
) {
	public static ESExprCodec<InterfaceMethodParameter> codec() {
		return InterfaceMethodParameter_CodecImpl.INSTANCE;
	}
}

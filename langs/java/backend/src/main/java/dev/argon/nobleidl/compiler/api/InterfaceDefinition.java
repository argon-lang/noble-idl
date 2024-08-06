package dev.argon.nobleidl.compiler.api;

import dev.argon.esexpr.*;

import java.util.List;

@ESExprCodecGen
public record InterfaceDefinition(
	@Vararg
	List<InterfaceMethod> methods
) {
	public static ESExprCodec<InterfaceDefinition> codec() {
		return InterfaceDefinition_CodecImpl.INSTANCE;
	}
}

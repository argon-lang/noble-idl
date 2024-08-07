package dev.argon.nobleidl.compiler.api;
@dev.argon.esexpr.ESExprCodecGen
@dev.argon.esexpr.Constructor("interface-definition")
public record InterfaceDefinition(
	@dev.argon.esexpr.Vararg
	java.util.List<dev.argon.nobleidl.compiler.api.InterfaceMethod> methods
) {
	public static dev.argon.esexpr.ESExprCodec<dev.argon.nobleidl.compiler.api.InterfaceDefinition> codec() {
		return dev.argon.nobleidl.compiler.api.InterfaceDefinition_CodecImpl.INSTANCE;
	}
}

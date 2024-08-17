package dev.argon.nobleidl.compiler.api;
@dev.argon.esexpr.ESExprCodecGen
@dev.argon.esexpr.Constructor("exception-type-definition")
public record ExceptionTypeDefinition(
	dev.argon.nobleidl.compiler.api.@org.jetbrains.annotations.NotNull TypeExpr information
) {
	public static dev.argon.esexpr.ESExprCodec<dev.argon.nobleidl.compiler.api.ExceptionTypeDefinition> codec() {
		return dev.argon.nobleidl.compiler.api.ExceptionTypeDefinition_CodecImpl.INSTANCE;
	}
}

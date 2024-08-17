package dev.argon.nobleidl.compiler.api;
@dev.argon.esexpr.ESExprCodecGen
@dev.argon.esexpr.Constructor("interface-method-parameter")
public record InterfaceMethodParameter(
	java.lang.@org.jetbrains.annotations.NotNull String name,
	dev.argon.nobleidl.compiler.api.@org.jetbrains.annotations.NotNull TypeExpr parameterType,
	@dev.argon.esexpr.Keyword("annotations")
	java.util.@org.jetbrains.annotations.NotNull List<dev.argon.nobleidl.compiler.api.Annotation> annotations
) {
	public static dev.argon.esexpr.ESExprCodec<dev.argon.nobleidl.compiler.api.InterfaceMethodParameter> codec() {
		return dev.argon.nobleidl.compiler.api.InterfaceMethodParameter_CodecImpl.INSTANCE;
	}
}

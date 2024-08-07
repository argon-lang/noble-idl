package dev.argon.nobleidl.compiler.api;
@dev.argon.esexpr.ESExprCodecGen
@dev.argon.esexpr.Constructor("interface-method-parameter")
public record InterfaceMethodParameter(
	java.lang.String name,
	dev.argon.nobleidl.compiler.api.TypeExpr parameterType,
	@dev.argon.esexpr.Keyword("annotations")
	java.util.List<dev.argon.nobleidl.compiler.api.Annotation> annotations
) {
	public static dev.argon.esexpr.ESExprCodec<dev.argon.nobleidl.compiler.api.InterfaceMethodParameter> codec() {
		return dev.argon.nobleidl.compiler.api.InterfaceMethodParameter_CodecImpl.INSTANCE;
	}
}

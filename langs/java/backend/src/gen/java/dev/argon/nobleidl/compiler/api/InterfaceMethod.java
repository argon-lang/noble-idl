package dev.argon.nobleidl.compiler.api;
@dev.argon.esexpr.ESExprCodecGen
@dev.argon.esexpr.Constructor("interface-method")
public record InterfaceMethod(
	@dev.argon.esexpr.Keyword("name")
	java.lang.String name,
	@dev.argon.esexpr.Keyword("type-parameters")
	java.util.List<dev.argon.nobleidl.compiler.api.TypeParameter> typeParameters,
	@dev.argon.esexpr.Keyword("parameters")
	java.util.List<dev.argon.nobleidl.compiler.api.InterfaceMethodParameter> parameters,
	@dev.argon.esexpr.Keyword("return-type")
	dev.argon.nobleidl.compiler.api.TypeExpr returnType,
	@dev.argon.esexpr.Keyword("annotations")
	java.util.List<dev.argon.nobleidl.compiler.api.Annotation> annotations
) {
	public static dev.argon.esexpr.ESExprCodec<dev.argon.nobleidl.compiler.api.InterfaceMethod> codec() {
		return dev.argon.nobleidl.compiler.api.InterfaceMethod_CodecImpl.INSTANCE;
	}
}

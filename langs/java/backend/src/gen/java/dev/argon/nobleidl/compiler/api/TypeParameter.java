package dev.argon.nobleidl.compiler.api;
@dev.argon.esexpr.ESExprCodecGen
public sealed interface TypeParameter {
	@dev.argon.esexpr.Constructor("type")
	record Type(
		java.lang.String name,
		@dev.argon.esexpr.Keyword("annotations")
		java.util.List<dev.argon.nobleidl.compiler.api.Annotation> annotations
	) implements dev.argon.nobleidl.compiler.api.TypeParameter {}
	public static dev.argon.esexpr.ESExprCodec<dev.argon.nobleidl.compiler.api.TypeParameter> codec() {
		return dev.argon.nobleidl.compiler.api.TypeParameter_CodecImpl.INSTANCE;
	}
}

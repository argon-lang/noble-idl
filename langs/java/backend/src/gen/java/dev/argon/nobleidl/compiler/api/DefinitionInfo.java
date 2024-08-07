package dev.argon.nobleidl.compiler.api;
@dev.argon.esexpr.ESExprCodecGen
@dev.argon.esexpr.Constructor("definition-info")
public record DefinitionInfo(
	@dev.argon.esexpr.Keyword("name")
	dev.argon.nobleidl.compiler.api.QualifiedName name,
	@dev.argon.esexpr.Keyword("type-parameters")
	java.util.List<dev.argon.nobleidl.compiler.api.TypeParameter> typeParameters,
	@dev.argon.esexpr.Keyword("definition")
	dev.argon.nobleidl.compiler.api.Definition definition,
	@dev.argon.esexpr.Keyword("annotations")
	java.util.List<dev.argon.nobleidl.compiler.api.Annotation> annotations,
	@dev.argon.esexpr.Keyword("is-library")
	boolean isLibrary
) {
	public static dev.argon.esexpr.ESExprCodec<dev.argon.nobleidl.compiler.api.DefinitionInfo> codec() {
		return dev.argon.nobleidl.compiler.api.DefinitionInfo_CodecImpl.INSTANCE;
	}
}

package dev.argon.nobleidl.compiler.api;
@dev.argon.esexpr.ESExprCodecGen
public sealed interface TypeParameter {
	@dev.argon.esexpr.Constructor("type")
	record Type(
		java.lang.@org.jetbrains.annotations.NotNull String name,
		@dev.argon.esexpr.Keyword("constraints")
		@dev.argon.esexpr.DefaultValue("dev.argon.nobleidl.runtime.List.<dev.argon.nobleidl.compiler.api.TypeParameterTypeConstraint>buildFrom(new dev.argon.nobleidl.runtime.ListRepr<dev.argon.nobleidl.compiler.api.TypeParameterTypeConstraint>(dev.argon.nobleidl.runtime.List.<dev.argon.nobleidl.compiler.api.TypeParameterTypeConstraint>fromValues()))")
		java.util.@org.jetbrains.annotations.NotNull List<dev.argon.nobleidl.compiler.api.TypeParameterTypeConstraint> constraints,
		@dev.argon.esexpr.Keyword("annotations")
		java.util.@org.jetbrains.annotations.NotNull List<dev.argon.nobleidl.compiler.api.Annotation> annotations
	) implements dev.argon.nobleidl.compiler.api.TypeParameter {}
	public static dev.argon.esexpr.ESExprCodec<dev.argon.nobleidl.compiler.api.TypeParameter> codec() {
		return dev.argon.nobleidl.compiler.api.TypeParameter_CodecImpl.INSTANCE;
	}
}

package dev.argon.nobleidl.compiler.api;
@dev.argon.esexpr.ESExprCodecGen
public sealed interface TypeExpr {
	@dev.argon.esexpr.Constructor("defined-type")
	record DefinedType(
		dev.argon.nobleidl.compiler.api.@org.jetbrains.annotations.NotNull QualifiedName name,
		@dev.argon.esexpr.Vararg
		java.util.@org.jetbrains.annotations.NotNull List<dev.argon.nobleidl.compiler.api.TypeExpr> args
	) implements dev.argon.nobleidl.compiler.api.TypeExpr {}
	@dev.argon.esexpr.Constructor("type-parameter")
	record TypeParameter(
		java.lang.@org.jetbrains.annotations.NotNull String name,
		@dev.argon.esexpr.Keyword("owner")
		dev.argon.nobleidl.compiler.api.@org.jetbrains.annotations.NotNull TypeParameterOwner owner
	) implements dev.argon.nobleidl.compiler.api.TypeExpr {}
	public static dev.argon.esexpr.ESExprCodec<dev.argon.nobleidl.compiler.api.TypeExpr> codec() {
		return dev.argon.nobleidl.compiler.api.TypeExpr_CodecImpl.INSTANCE;
	}
}

package dev.argon.nobleidl.compiler.api;
@dev.argon.esexpr.ESExprCodecGen
public sealed interface TypeExpr {
	@dev.argon.esexpr.Constructor("defined-type")
	record DefinedType(
		dev.argon.nobleidl.compiler.api.QualifiedName name,
		@dev.argon.esexpr.Vararg
		java.util.List<dev.argon.nobleidl.compiler.api.TypeExpr> args
	) implements dev.argon.nobleidl.compiler.api.TypeExpr {}
	@dev.argon.esexpr.Constructor("type-parameter")
	record TypeParameter(
		java.lang.String name
	) implements dev.argon.nobleidl.compiler.api.TypeExpr {}
	public static dev.argon.esexpr.ESExprCodec<dev.argon.nobleidl.compiler.api.TypeExpr> codec() {
		return dev.argon.nobleidl.compiler.api.TypeExpr_CodecImpl.INSTANCE;
	}
}
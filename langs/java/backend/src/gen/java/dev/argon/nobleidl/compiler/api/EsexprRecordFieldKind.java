package dev.argon.nobleidl.compiler.api;
@dev.argon.esexpr.ESExprCodecGen
public sealed interface EsexprRecordFieldKind {
	@dev.argon.esexpr.Constructor("positional")
	record Positional(
		dev.argon.nobleidl.compiler.api.EsexprRecordPositionalMode mode
	) implements dev.argon.nobleidl.compiler.api.EsexprRecordFieldKind {}
	@dev.argon.esexpr.Constructor("keyword")
	record Keyword(
		java.lang.String name,
		dev.argon.nobleidl.compiler.api.EsexprRecordKeywordMode mode
	) implements dev.argon.nobleidl.compiler.api.EsexprRecordFieldKind {}
	@dev.argon.esexpr.Constructor("dict")
	record Dict(
		dev.argon.nobleidl.compiler.api.TypeExpr elementType
	) implements dev.argon.nobleidl.compiler.api.EsexprRecordFieldKind {}
	@dev.argon.esexpr.Constructor("vararg")
	record Vararg(
		dev.argon.nobleidl.compiler.api.TypeExpr elementType
	) implements dev.argon.nobleidl.compiler.api.EsexprRecordFieldKind {}
	public static dev.argon.esexpr.ESExprCodec<dev.argon.nobleidl.compiler.api.EsexprRecordFieldKind> codec() {
		return dev.argon.nobleidl.compiler.api.EsexprRecordFieldKind_CodecImpl.INSTANCE;
	}
}

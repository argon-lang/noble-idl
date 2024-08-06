package dev.argon.nobleidl.compiler.api;

import dev.argon.esexpr.ESExprCodec;
import dev.argon.esexpr.ESExprCodecGen;

@ESExprCodecGen
public sealed interface EsexprRecordFieldKind {
	record Positional(
		EsexprRecordPositionalMode mode
	) implements EsexprRecordFieldKind {}

	record Keyword(
		String name,
		EsexprRecordKeywordMode mode
	) implements EsexprRecordFieldKind {}

	record Vararg(
		TypeExpr elementType
	) implements EsexprRecordFieldKind {}

	record Dict(
		TypeExpr elementType
	) implements EsexprRecordFieldKind {}

	public static ESExprCodec<EsexprRecordFieldKind> codec() {
		return EsexprRecordFieldKind_CodecImpl.INSTANCE;
	}
}

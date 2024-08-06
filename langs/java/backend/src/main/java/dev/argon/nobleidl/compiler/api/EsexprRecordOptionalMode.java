package dev.argon.nobleidl.compiler.api;

import dev.argon.esexpr.ESExprCodecGen;

@ESExprCodecGen
public sealed interface EsexprRecordOptionalMode {
	record Required() implements EsexprRecordOptionalMode {}

	record Optional(
		TypeExpr elementType
	) implements EsexprRecordOptionalMode {}

	record DefaultValue(
		EsexprDecodedValue value
	) implements EsexprRecordOptionalMode {}
}

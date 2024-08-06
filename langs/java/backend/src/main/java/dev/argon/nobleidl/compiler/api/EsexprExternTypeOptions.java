package dev.argon.nobleidl.compiler.api;

import dev.argon.esexpr.*;

import java.util.Optional;

@ESExprCodecGen
@Constructor("extern-type-options")
public record EsexprExternTypeOptions(
	@Keyword
	@DefaultValue("false")
	boolean allowValue,

	@Keyword
	@OptionalValue
	Optional<TypeExpr> allowOptional,


	@Keyword
	@OptionalValue
	Optional<TypeExpr> allowVararg,


	@Keyword
	@OptionalValue
	Optional<TypeExpr> allowDict,


	@Keyword
	EsexprExternTypeLiterals literals
) {
	public static ESExprCodec<EsexprExternTypeOptions> codec() {
		return EsexprExternTypeOptions_CodecImpl.INSTANCE;
	}
}

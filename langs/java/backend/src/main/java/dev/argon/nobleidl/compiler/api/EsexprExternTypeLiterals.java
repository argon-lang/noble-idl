package dev.argon.nobleidl.compiler.api;

import dev.argon.esexpr.*;

import java.math.BigInteger;
import java.util.Optional;

@ESExprCodecGen
@Constructor("literals")
public record EsexprExternTypeLiterals(
	@Keyword
	@DefaultValue("false")
	boolean allowBool,

	@Keyword
	@DefaultValue("false")
	boolean allowInt,

	@Keyword
	@OptionalValue
	Optional<BigInteger> minInt,

	@Keyword
	@OptionalValue
	Optional<BigInteger> maxInt,


	@Keyword
	@DefaultValue("false")
	boolean allowStr,

	@Keyword
	@DefaultValue("false")
	boolean allowBinary,

	@Keyword
	@DefaultValue("false")
	boolean allowFloat32,

	@Keyword
	@DefaultValue("false")
	boolean allowFloat64,

	@Keyword
	@DefaultValue("false")
	boolean allowNull,

	@Keyword
	@OptionalValue
	Optional<TypeExpr> buildLiteralFrom
) {
	public static ESExprCodec<EsexprExternTypeLiterals> codec() {
		return EsexprExternTypeLiterals_CodecImpl.INSTANCE;
	}
}

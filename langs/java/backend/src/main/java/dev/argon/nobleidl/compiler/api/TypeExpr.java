package dev.argon.nobleidl.compiler.api;

import dev.argon.esexpr.ESExprCodec;
import dev.argon.esexpr.ESExprCodecGen;
import dev.argon.esexpr.Vararg;
import dev.argon.esexpr.VarargCodec;

import java.util.List;

@ESExprCodecGen
public sealed interface TypeExpr {
	record DefinedType(
		QualifiedName name,

		@Vararg
		List<TypeExpr> args
	) implements TypeExpr {}

	record TypeParameter(
		String name
	) implements TypeExpr {}

	public static ESExprCodec<TypeExpr> codec() {
		return TypeExpr_CodecImpl.INSTANCE;
	}
}

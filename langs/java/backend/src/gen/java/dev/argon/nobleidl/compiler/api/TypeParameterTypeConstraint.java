package dev.argon.nobleidl.compiler.api;
@dev.argon.esexpr.ESExprCodecGen
public sealed interface TypeParameterTypeConstraint {
	@dev.argon.esexpr.Constructor("exception")
	record Exception(

	) implements dev.argon.nobleidl.compiler.api.TypeParameterTypeConstraint {}
	public static dev.argon.esexpr.ESExprCodec<dev.argon.nobleidl.compiler.api.TypeParameterTypeConstraint> codec() {
		return dev.argon.nobleidl.compiler.api.TypeParameterTypeConstraint_CodecImpl.INSTANCE;
	}
}

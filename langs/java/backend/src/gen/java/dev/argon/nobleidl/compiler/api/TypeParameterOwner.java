package dev.argon.nobleidl.compiler.api;
@dev.argon.esexpr.ESExprCodecGen
public enum TypeParameterOwner {
	@dev.argon.esexpr.Constructor("by-type")
	BY_TYPE,
	@dev.argon.esexpr.Constructor("by-method")
	BY_METHOD,
	;
	public static dev.argon.esexpr.ESExprCodec<dev.argon.nobleidl.compiler.api.TypeParameterOwner> codec() {
		return dev.argon.nobleidl.compiler.api.TypeParameterOwner_CodecImpl.INSTANCE;
	}
}

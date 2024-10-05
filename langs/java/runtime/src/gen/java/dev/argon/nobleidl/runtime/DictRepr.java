package dev.argon.nobleidl.runtime;
@dev.argon.esexpr.ESExprCodecGen
@dev.argon.esexpr.Constructor("dict")
public record DictRepr<A>(
	@dev.argon.esexpr.Dict
	dev.argon.esexpr.@org.jetbrains.annotations.NotNull KeywordMapping<A> values
) {
	public static <A> dev.argon.esexpr.ESExprCodec<dev.argon.nobleidl.runtime.DictRepr<A>> codec(dev.argon.esexpr.ESExprCodec<A> aCodec) {
		return new dev.argon.nobleidl.runtime.DictRepr_CodecImpl<A>(aCodec);
	}
}

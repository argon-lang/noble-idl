package dev.argon.nobleidl.runtime;
@dev.argon.esexpr.ESExprCodecGen
@dev.argon.esexpr.Constructor("list")
public record ListRepr<A>(
	@dev.argon.esexpr.Vararg
	java.util.@org.jetbrains.annotations.NotNull List<A> values
) {
	public static <A> dev.argon.esexpr.ESExprCodec<dev.argon.nobleidl.runtime.ListRepr<A>> codec(dev.argon.esexpr.ESExprCodec<A> aCodec) {
		return new dev.argon.nobleidl.runtime.ListRepr_CodecImpl<A>(aCodec);
	}
}

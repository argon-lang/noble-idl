package dev.argon.nobleidl.runtime;

public class List {
	private List() {}

	@SafeVarargs
	public static <A> java.util.List<A> fromValues(A... values) {
		return java.util.List.of(values);
	}

	public static <A> java.util.List<A> buildFrom(ListRepr<A> repr) {
		return repr.values();
	}
}

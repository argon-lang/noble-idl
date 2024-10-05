package dev.argon.nobleidl.runtime;

import java.util.Optional;

public final class Option {
	private Option() {}

	public static <A> Optional<A> buildFrom(A value) {
		return Optional.of(value);
	}

	public static  <A> Optional<A> fromNull() {
		return Optional.empty();
	}
}

package dev.argon.nobleidl.runtime;

import java.util.Optional;

public class OptionalField {
	private OptionalField() {}

	public static <A> Optional<A> fromElement(A value) {
		return Optional.of(value);
	}

	public static <A> Optional<A> empty() {
		return Optional.empty();
	}
}

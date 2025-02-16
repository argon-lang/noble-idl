package dev.argon.nobleidl.runtime;

import dev.argon.nobleidl.runtime.graaljsInterop.JSAdapter;

import java.math.BigInteger;

public final class Nat {
	private Nat() {}

	public static BigInteger fromBigInteger(BigInteger value) {
		return value;
	}

	public static JSAdapter<BigInteger> jsAdapter() {
		return Int.jsAdapter();
	}
}

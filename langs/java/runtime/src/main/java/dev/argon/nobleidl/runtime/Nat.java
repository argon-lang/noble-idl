package dev.argon.nobleidl.runtime;

import java.math.BigInteger;

public class Nat {
	private Nat() {}

	public static BigInteger fromBigInteger(BigInteger value) {
		return value;
	}
}

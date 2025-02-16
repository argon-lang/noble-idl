package dev.argon.nobleidl.runtime;

import dev.argon.nobleidl.runtime.graaljsInterop.JSAdapter;
import dev.argon.nobleidl.runtime.graaljsInterop.JSExecutor;
import org.graalvm.polyglot.Context;
import org.graalvm.polyglot.Value;

import java.math.BigInteger;

public final class Int {
	private Int() {}

	public static BigInteger fromBigInteger(BigInteger value) {
		return value;
	}

	public static JSAdapter<BigInteger> jsAdapter() {
		return new JSAdapter<>() {
			@Override
			public BigInteger fromJS(Context context, JSExecutor executor, Value value) {
				return value.asBigInteger();
			}

			@Override
			public Value toJS(Context context, JSExecutor executor, BigInteger value) {
				return context.eval("js", "BigInt").execute(value);
			}
		};
	}
}

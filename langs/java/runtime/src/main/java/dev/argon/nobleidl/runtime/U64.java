package dev.argon.nobleidl.runtime;

import dev.argon.nobleidl.runtime.graaljsInterop.JSAdapter;
import dev.argon.nobleidl.runtime.graaljsInterop.JSExecutor;
import org.graalvm.polyglot.Context;
import org.graalvm.polyglot.Value;

import java.math.BigInteger;

public final class U64 {
	private U64() {}

	public static long fromUnsignedLong(long value) {
		return value;
	}

	public static JSAdapter<Long> jsAdapter() {
		return new JSAdapter<>() {
			@Override
			public Long fromJS(Context context, JSExecutor executor, Value value) {
				return value.asBigInteger().longValue();
			}

			@Override
			public Value toJS(Context context, JSExecutor executor, Long value) {
				var bigInt = BigInteger.valueOf(value >>> 1).shiftLeft(1).or(BigInteger.valueOf(value & 0x1L));
				return context.eval("js", "BigInt").execute(bigInt);
			}
		};
	}
}

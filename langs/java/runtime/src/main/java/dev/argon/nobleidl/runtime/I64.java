package dev.argon.nobleidl.runtime;

import dev.argon.nobleidl.runtime.graaljsInterop.JSAdapter;
import dev.argon.nobleidl.runtime.graaljsInterop.JSExecutor;
import org.graalvm.polyglot.Context;
import org.graalvm.polyglot.Value;

public final class I64 {
	private I64() {}

	public static long fromLong(long value) {
		return value;
	}

	public static JSAdapter<Long> jsAdapter() {
		return new JSAdapter<>() {
			@Override
			public Long fromJS(Context context, JSExecutor executor, Value value) {
				return value.asBigInteger().longValueExact();
			}

			@Override
			public Value toJS(Context context, JSExecutor executor, Long value) {
				return context.eval("js", "BigInt").execute(value);
			}
		};
	}
}

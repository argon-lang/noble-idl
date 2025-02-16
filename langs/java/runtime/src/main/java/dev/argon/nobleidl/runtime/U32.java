package dev.argon.nobleidl.runtime;

import dev.argon.nobleidl.runtime.graaljsInterop.JSAdapter;
import dev.argon.nobleidl.runtime.graaljsInterop.JSExecutor;
import org.graalvm.polyglot.Context;
import org.graalvm.polyglot.Value;

public final class U32 {
	private U32() {}

	public static int fromUnsignedInt(int value) {
		return value;
	}

	public static JSAdapter<Integer> jsAdapter() {
		return new JSAdapter<>() {
			@Override
			public Integer fromJS(Context context, JSExecutor executor, Value value) {
				return (int)value.asLong();
			}

			@Override
			public Value toJS(Context context, JSExecutor executor, Integer value) {
				return context.asValue(Integer.toUnsignedLong(value));
			}
		};
	}
}

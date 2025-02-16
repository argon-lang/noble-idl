package dev.argon.nobleidl.runtime;

import dev.argon.nobleidl.runtime.graaljsInterop.JSAdapter;
import dev.argon.nobleidl.runtime.graaljsInterop.JSExecutor;
import org.graalvm.polyglot.Context;
import org.graalvm.polyglot.Value;

public final class I32 {
	private I32() {}

	public static int fromInt(int value) {
		return value;
	}

	public static JSAdapter<Integer> jsAdapter() {
		return new JSAdapter<>() {
			@Override
			public Integer fromJS(Context context, JSExecutor executor, Value value) {
				return value.asInt();
			}

			@Override
			public Value toJS(Context context, JSExecutor executor, Integer value) {
				return context.asValue(value);
			}
		};
	}
}

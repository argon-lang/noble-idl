package dev.argon.nobleidl.runtime;

import dev.argon.nobleidl.runtime.graaljsInterop.JSAdapter;
import dev.argon.nobleidl.runtime.graaljsInterop.JSExecutor;
import org.graalvm.polyglot.Context;
import org.graalvm.polyglot.Value;

public final class F32 {
	private F32() {}

	public static float fromFloat(float value) {
		return value;
	}

	public static JSAdapter<Float> jsAdapter() {
		return new JSAdapter<>() {
			@Override
			public Float fromJS(Context context, JSExecutor executor, Value value) {
				return (float)value.asDouble();
			}

			@Override
			public Value toJS(Context context, JSExecutor executor, Float value) {
				return context.asValue(value);
			}
		};
	}
}

package dev.argon.nobleidl.runtime;

import dev.argon.nobleidl.runtime.graaljsInterop.JSAdapter;
import dev.argon.nobleidl.runtime.graaljsInterop.JSExecutor;
import org.graalvm.polyglot.Context;
import org.graalvm.polyglot.Value;

public final class F64 {
	private F64() {}

	public static double fromDouble(double value) {
		return value;
	}

	public static JSAdapter<Double> jsAdapter() {
		return new JSAdapter<>() {
			@Override
			public Double fromJS(Context context, JSExecutor executor, Value value) {
				return value.asDouble();
			}

			@Override
			public Value toJS(Context context, JSExecutor executor, Double value) {
				return context.asValue(value);
			}
		};
	}
}

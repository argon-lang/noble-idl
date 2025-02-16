package dev.argon.nobleidl.runtime;

import dev.argon.nobleidl.runtime.graaljsInterop.JSAdapter;
import dev.argon.nobleidl.runtime.graaljsInterop.JSExecutor;
import org.graalvm.polyglot.Context;
import org.graalvm.polyglot.Value;

public final class I16 {
	private I16() {}

	public static short fromShort(short value) {
		return value;
	}

	public static JSAdapter<Short> jsAdapter() {
		return new JSAdapter<>() {
			@Override
			public Short fromJS(Context context, JSExecutor executor, Value value) {
				return value.asShort();
			}

			@Override
			public Value toJS(Context context, JSExecutor executor, Short value) {
				return context.asValue(value);
			}
		};
	}
}

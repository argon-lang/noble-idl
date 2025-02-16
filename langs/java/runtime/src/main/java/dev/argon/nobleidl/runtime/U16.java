package dev.argon.nobleidl.runtime;

import dev.argon.nobleidl.runtime.graaljsInterop.JSAdapter;
import dev.argon.nobleidl.runtime.graaljsInterop.JSExecutor;
import org.graalvm.polyglot.Context;
import org.graalvm.polyglot.Value;

public final class U16 {
	private U16() {}

	public static short fromUnsignedShort(short value) {
		return value;
	}

	public static JSAdapter<Short> jsAdapter() {
		return new JSAdapter<>() {
			@Override
			public Short fromJS(Context context, JSExecutor executor, Value value) {
				return (short)value.asInt();
			}

			@Override
			public Value toJS(Context context, JSExecutor executor, Short value) {
				return context.asValue(Short.toUnsignedInt(value));
			}
		};
	}
}

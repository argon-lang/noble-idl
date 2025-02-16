package dev.argon.nobleidl.runtime;

import dev.argon.nobleidl.runtime.graaljsInterop.JSAdapter;
import dev.argon.nobleidl.runtime.graaljsInterop.JSExecutor;
import org.graalvm.polyglot.Context;
import org.graalvm.polyglot.Value;

public final class I8 {
	private I8() {}

	public static byte fromByte(byte value) {
		return value;
	}

	public static JSAdapter<Byte> jsAdapter() {
		return new JSAdapter<>() {
			@Override
			public Byte fromJS(Context context, JSExecutor executor, Value value) {
				return value.asByte();
			}

			@Override
			public Value toJS(Context context, JSExecutor executor, Byte value) {
				return context.asValue(value);
			}
		};
	}
}

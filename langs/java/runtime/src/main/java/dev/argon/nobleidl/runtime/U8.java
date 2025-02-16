package dev.argon.nobleidl.runtime;

import dev.argon.nobleidl.runtime.graaljsInterop.JSAdapter;
import dev.argon.nobleidl.runtime.graaljsInterop.JSExecutor;
import org.graalvm.polyglot.Context;
import org.graalvm.polyglot.Value;

public final class U8 {
	private U8() {}

	public static byte fromUnsignedByte(byte value) {
		return value;
	}

	public static JSAdapter<Byte> jsAdapter() {
		return new JSAdapter<>() {
			@Override
			public Byte fromJS(Context context, JSExecutor executor, Value value) {
				return (byte)value.asInt();
			}

			@Override
			public Value toJS(Context context, JSExecutor executor, Byte value) {
				return context.asValue(Byte.toUnsignedInt(value));
			}
		};
	}
}

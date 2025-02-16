package dev.argon.nobleidl.runtime;

import dev.argon.nobleidl.runtime.graaljsInterop.JSAdapter;
import dev.argon.nobleidl.runtime.graaljsInterop.JSExecutor;
import org.graalvm.polyglot.Context;
import org.graalvm.polyglot.Value;

public final class Binary {
	private Binary() {}

	public static byte[] fromByteArray(byte[] value) {
		return value;
	}

	public static JSAdapter<byte[]> jsAdapter() {
		return new JSAdapter<>() {
			@Override
			public byte[] fromJS(Context context, JSExecutor executor, Value value) {
				long len = value.getArraySize();
				if(len > Integer.MAX_VALUE) {
					throw new IllegalArgumentException("Could not convert byte[] from JS. Length too long.");
				}

				byte[] b = new byte[(int)len];
				for(int i = 0; i < b.length; ++i) {
					b[i] = (byte)value.getArrayElement(i).asInt();
				}
				return b;
			}

			@Override
			public Value toJS(Context context, JSExecutor executor, byte[] value) {
				Value arr = context.eval("js", "n => new Uint8Array(n)").execute(value.length);

				for(int i = 0; i < value.length; ++i) {
					arr.setArrayElement(i, value[i]);
				}

				return arr;
			}
		};
	}
}

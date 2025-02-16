package dev.argon.nobleidl.runtime;

import dev.argon.nobleidl.runtime.graaljsInterop.JSAdapter;
import dev.argon.nobleidl.runtime.graaljsInterop.JSExecutor;
import org.graalvm.polyglot.Context;
import org.graalvm.polyglot.Value;

public final class String {
	private String() {}

	public static java.lang.String fromString(java.lang.String value) {
		return value;
	}

	public static JSAdapter<java.lang.String> jsAdapter() {
		return new JSAdapter<>() {
			@Override
			public java.lang.String fromJS(Context context, JSExecutor executor, Value value) {
				return value.asString();
			}

			@Override
			public Value toJS(Context context, JSExecutor executor, java.lang.String value) {
				return context.asValue(value);
			}
		};
	}
}

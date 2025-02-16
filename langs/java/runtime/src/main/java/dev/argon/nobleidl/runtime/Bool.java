package dev.argon.nobleidl.runtime;

import dev.argon.nobleidl.runtime.graaljsInterop.JSAdapter;
import dev.argon.nobleidl.runtime.graaljsInterop.JSExecutor;
import org.graalvm.polyglot.Context;
import org.graalvm.polyglot.Value;

public final class Bool {
	private Bool() {}

	public static boolean fromBoolean(boolean value) {
		return value;
	}



	public static JSAdapter<Boolean> jsAdapter() {
		return new JSAdapter<>() {
			@Override
			public Boolean fromJS(Context context, JSExecutor executor, Value value) {
				return value.asBoolean();
			}

			@Override
			public Value toJS(Context context, JSExecutor executor, Boolean value) {
				return context.asValue(value);
			}
		};
	}
}

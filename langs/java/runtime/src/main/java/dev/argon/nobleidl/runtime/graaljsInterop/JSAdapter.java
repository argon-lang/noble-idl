package dev.argon.nobleidl.runtime.graaljsInterop;

import org.graalvm.polyglot.Context;
import org.graalvm.polyglot.Value;

public interface JSAdapter<T> {
	T fromJS(Context context, JSExecutor executor, Value value);
	Value toJS(Context context, JSExecutor executor, T value);


	static <T> JSAdapter<T> identity() {
		return new IdentityJSAdapter<>();
	}

	static JSAdapter<Value> VALUE_ADAPTER = new JSAdapter<Value>() {
		@Override
		public Value fromJS(Context context, JSExecutor executor, Value value) {
			return value;
		}

		@Override
		public Value toJS(Context context, JSExecutor executor, Value value) {
			return value;
		}
	};

}

package dev.argon.nobleidl.runtime.graaljsInterop;

import org.graalvm.polyglot.Context;
import org.graalvm.polyglot.Value;
import org.graalvm.polyglot.proxy.Proxy;

class IdentityJSAdapter<A> implements JSAdapter<A> {
	@Override
	public A fromJS(Context context, JSExecutor executor, Value value) {
		Object result = value.as(Object.class);

		if(result instanceof WrappedValue wv) {
			result = wv.value();
		}

		@SuppressWarnings("unchecked")
		A convValue = (A)result;

		return convValue;
	}

	@Override
	public Value toJS(Context context, JSExecutor executor, A value) {
		if(
			value instanceof Proxy ||
				value instanceof Value ||
				value instanceof Byte ||
				value instanceof Short ||
				value instanceof Integer ||
				value instanceof Long ||
				value instanceof Float ||
				value instanceof Double ||
				value instanceof Character
		) {
			return context.asValue(new WrappedValue(value));
		}
		else {
			return context.asValue(value);
		}
	}

	private record WrappedValue(Object value) {}
}

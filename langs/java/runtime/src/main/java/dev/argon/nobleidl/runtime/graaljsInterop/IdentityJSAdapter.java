package dev.argon.nobleidl.runtime.graaljsInterop;

import org.graalvm.polyglot.Context;
import org.graalvm.polyglot.Value;
import org.graalvm.polyglot.proxy.Proxy;

class IdentityJSAdapter<A> implements JSAdapter<A> {
	@Override
	public A fromJS(Context context, JSExecutor executor, Value value) {
		Object result;

		var metaObject = value.getMetaObject();

		if(metaObject.isHostObject() && metaObject.asHostObject() instanceof Class<?> cls) {
			if(cls == Byte.class) {
				result = value.asByte();
			}
			else if(cls == Short.class) {
				result = value.asShort();
			}
			else if(cls == Integer.class) {
				result = value.asInt();
			}
			else if(cls == Long.class) {
				result = value.asLong();
			}
			else if(cls == Float.class) {
				result = value.asFloat();
			}
			else if(cls == Double.class) {
				result = value.asDouble();
			}
			else if(cls == Boolean.class) {
				result = value.asBoolean();
			}
			else if(cls == Character.class) {
				result = value.as(Character.class);
			}
			else {
				result = value.asHostObject();
			}
		}
		else {
			result = value.asHostObject();
		}

		if(result instanceof WrappedValue wv) {
			result = wv.value();
		}

		@SuppressWarnings("unchecked")
		A convValue = (A)result;

		return convValue;
	}

	@Override
	public Value toJS(Context context, JSExecutor executor, A value) {
		if(value instanceof Proxy || value instanceof Value) {
			return context.asValue(new WrappedValue(value));
		}
		else {
			return context.asValue(value);
		}
	}

	private record WrappedValue(Object value) {}
}

package dev.argon.nobleidl.runtime.graaljsInterop;

import org.graalvm.polyglot.Context;
import org.graalvm.polyglot.Value;
import org.jetbrains.annotations.Nullable;

public class ObjectWrapUtil {
	private ObjectWrapUtil() {}

	public static void putJavaObject(Context context, Value jsObject, Object javaObject) {
		context.eval("js", "(js, java) => js[Symbol.for('nobleidl-java-wrapper')] = java").executeVoid(jsObject, javaObject);
	}

	public static @Nullable Object getJavaObject(Context context, Value jsObject) {
		var value = context.eval("js", "o => o[Symbol.for('nobleidl-java-wrapper')]").execute(jsObject);
		if(value.isNull()) {
			return null;
		}

		return value.asHostObject();
	}

	public static @Nullable Value getJSObject(Object javaObject) {
		if(javaObject instanceof WrappedJavaScriptObject o) {
			return o._getAsJSValue();
		}

		return null;
	}
}

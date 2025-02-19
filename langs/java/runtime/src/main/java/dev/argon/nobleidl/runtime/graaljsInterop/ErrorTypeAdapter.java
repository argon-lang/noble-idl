package dev.argon.nobleidl.runtime.graaljsInterop;

import dev.argon.nobleidl.runtime.ErrorType;
import org.graalvm.polyglot.Context;
import org.graalvm.polyglot.PolyglotException;
import org.graalvm.polyglot.Value;
import org.graalvm.polyglot.proxy.ProxyExecutable;
import org.jetbrains.annotations.Nullable;

public class ErrorTypeAdapter {
	private ErrorTypeAdapter() {}

	public static ErrorType<Value, Throwable> fromJS(Context context, JSExecutor executor, Value jsErrorChecker) {
		return new ErrorType<Value, Throwable>() {
			@Override
			public @Nullable Value tryFromObject(Object obj) throws InterruptedException {
				return executor.runOnJSThreadWithoutError(() -> {
					Value jsError = new IdentityJSAdapter<>().toJS(context, executor, obj);

					if(!jsErrorChecker.invokeMember("isInstance", jsError).asBoolean()) {
						return null;
					}

					return jsError;
				}).get();
			}

			@Override
			public @Nullable Value tryFromThrowable(Throwable ex) throws InterruptedException {
				return executor.runOnJSThreadWithoutError(() -> {
					Value jsError = ExceptionUtil.throwableToValue(context, ex);

					if(!jsErrorChecker.invokeMember("isInstance", jsError).asBoolean()) {
						return null;
					}

					return jsError;
				}).get();
			}

			@Override
			public Throwable toThrowable(Value error) throws InterruptedException {
				return executor.runOnJSThreadWithoutError(() -> {
					return ExceptionUtil.valueToThrowable(context, error);
				}).get();
			}
		};
	}

	public static <T, E extends Throwable> Value toJS(Context context, JSExecutor executor, ErrorType<T, E> errorType) {
		var obj = ObjectWrapUtil.getJSObject(errorType);
		if(obj != null) {
			return obj;
		}


		var isInstance = CallUtil.makeJavaScriptFunction(arguments -> {
			var e = arguments[0];
			return errorType.objectIsError(new IdentityJSAdapter<>().fromJS(context, executor, e));
		});

		return context.eval("js", "isInstance => ({ isInstance })").execute(isInstance);
	}
}

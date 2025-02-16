package dev.argon.nobleidl.runtime.graaljsInterop;

import dev.argon.nobleidl.runtime.ErrorType;
import dev.argon.nobleidl.runtime.NobleIDLException;
import org.graalvm.polyglot.Context;
import org.graalvm.polyglot.PolyglotException;
import org.graalvm.polyglot.Value;
import org.graalvm.polyglot.proxy.ProxyExecutable;

public class ErrorTypeAdapter {
	private ErrorTypeAdapter() {}

	public static ErrorType<?> fromJS(Context context, JSExecutor executor, Value jsErrorChecker) {
		var obj = ObjectWrapUtil.getJavaObject(context, jsErrorChecker);
		if(obj != null) {
			return (ErrorType<?>)obj;
		}

		class ErrorTypeImpl implements ErrorType<Throwable>, WrappedJavaScriptObject {
			@Override
			public Value _getAsJSValue() {
				return jsErrorChecker;
			}

			@Override
			public Throwable tryFromThrowable(Throwable ex) {
				if(!(ex instanceof PolyglotException pex)) {
					return null;
				}

				return ExceptionUtil.getFutureResultUnchecked(executor.runOnJSThread(() -> {
					Throwable javaException;
					Value jsError;
					if(pex.isHostException()) {
						javaException = pex.asHostException();
						jsError = context.asValue(javaException);
					}
					else {
						javaException = pex;
						jsError = pex.getGuestObject();
					}

					if(jsError == null) {
						return null;
					}

					if(!jsErrorChecker.invokeMember("isInstance", jsError).asBoolean()) {
						return null;
					}

					return javaException;
				}));
			}
		}

		return new ErrorTypeImpl();
	}

	public static <E extends Throwable> Value toJS(Context context, JSExecutor executor, ErrorType<E> errorType) {
		var obj = ObjectWrapUtil.getJSObject(errorType);
		if(obj != null) {
			return obj;
		}


		var isInstance = new ProxyExecutable() {
			@Override
			public Object execute(Value... arguments) {
				var e = arguments[0];
				return e.isHostObject() && e.asHostObject() instanceof Throwable ex && errorType.isError(ex);
			}
		};

		var errorChecker = context.eval("js", "isInstance => ({ isInstance })").execute(isInstance);
		ObjectWrapUtil.putJavaObject(context, obj, errorChecker);
		return errorChecker;
	}
}

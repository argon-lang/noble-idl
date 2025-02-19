package dev.argon.nobleidl.runtime.graaljsInterop;

import dev.argon.nobleidl.runtime.ErrorType;
import dev.argon.nobleidl.runtime.FutureWithoutError;
import org.graalvm.polyglot.Context;
import org.graalvm.polyglot.PolyglotException;
import org.graalvm.polyglot.Value;
import org.graalvm.polyglot.proxy.ProxyExecutable;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Future;

public class JSPromiseAdapter<T> {
	public JSPromiseAdapter(JSAdapter<T> tAdapter) {
		this.tAdapter = tAdapter;
	}

	private final JSAdapter<T> tAdapter;

	public FutureWithoutError<T> fromJS(Context context, JSExecutor executor, Value value) throws InterruptedException {
		var future = new CompletableFuture<T>();

		value.invokeMember("then",
			new ProxyExecutable() {
				@Override
				public Object execute(Value... arguments) {
					if(arguments.length < 1) {
						throw new IllegalArgumentException("Expected one argument for then resolve callback");
					}

					T javaValue = tAdapter.fromJS(context, executor, arguments[0]);
					future.complete(javaValue);

					return context.eval("js", "void 0");
				}
			},
			new ProxyExecutable() {
				@Override
				public Object execute(Value... arguments) {
					if(arguments.length < 1) {
						throw new IllegalArgumentException("Expected one argument for then reject callback");
					}

					Throwable ex = ExceptionUtil.valueToThrowable(context, arguments[0]);
					future.completeExceptionally(ex);

					return context.eval("js", "void 0");
				}
			}
		);

		return FutureWithoutError.fromFutureUnsafe(future);
	}

	public Value toJS(Context context, JSExecutor executor, FutureWithoutError<T> value) {
		return context.eval("js", "executor => new Promise(executor)").execute(
			new ProxyExecutable() {
				@Override
				public Object execute(Value... arguments) {
					if(arguments.length < 2) {
						throw new IllegalArgumentException("Expected two arguments for promise executor");
					}

					Value resolve = arguments[0];
					Value reject = arguments[1];

					executor.offloadJavaWithoutError(() -> {
						T javaValue;
						try {
							javaValue = value.get();
						}
						catch(Throwable ex) {
							executor.runOnJSThreadWithoutError(() -> {
								reject.executeVoid(ExceptionUtil.throwableToValue(context, ex));
								return null;
							}).get();
							return null;
						}

						executor.runOnJSThreadWithoutError(() -> {
							Value jsValue = tAdapter.toJS(context, executor, javaValue);
							resolve.executeVoid(jsValue);
							return null;
						}).get();

						return null;
					});

					return context.eval("js", "void 0");
				}
			}
		);
	}
}

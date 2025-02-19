package dev.argon.nobleidl.runtime.graaljsInterop;

import dev.argon.nobleidl.runtime.ErrorType;
import dev.argon.nobleidl.runtime.FutureWithError;
import dev.argon.nobleidl.runtime.FutureWithoutError;
import org.graalvm.polyglot.Context;
import org.graalvm.polyglot.PolyglotException;
import org.graalvm.polyglot.Value;
import org.graalvm.polyglot.proxy.ProxyExecutable;

import java.util.concurrent.CompletableFuture;

public class JSPromiseWithErrorAdapter<T, E> {
	public JSPromiseWithErrorAdapter(JSAdapter<T> tAdapter, JSAdapter<E> eAdapter) {
		this.tAdapter = tAdapter;
		this.eAdapter = eAdapter;
	}

	private final JSAdapter<T> tAdapter;
	private final JSAdapter<E> eAdapter;

	public <EX extends Throwable> FutureWithError<T, EX> fromJS(Context context, JSExecutor executor, ErrorType<E, ? extends EX> errorType, Value errorChecker, Value value) {
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

					Throwable ex;
					try {
						ex = ExceptionUtil.valueToThrowable(context, executor, eAdapter, errorType, errorChecker, arguments[0]);
					}
					catch(Throwable ex2) {
						ex = ex2;
					}

					future.completeExceptionally(ex);

					return context.eval("js", "void 0");
				}
			}
		);

		return FutureWithError.fromFutureUnsafe(future);
	}

	public <EX extends Throwable> Value toJS(Context context, JSExecutor executor, ErrorType<E, ? extends EX> errorType, FutureWithError<T, EX> value) {
		return context.eval("js", "executor => new Promise(executor)").execute(
			new ProxyExecutable() {
				@Override
				public Object execute(Value... arguments) {
					if(arguments.length < 2) {
						throw new IllegalArgumentException("Expected two arguments for promise executor");
					}

					Value resolve = arguments[0];
					Value reject = arguments[1];

					executor.offloadJavaWithError(() -> {
						T javaValue;
						try {
							javaValue = value.get();
						}
						catch(Throwable ex) {
							executor.runOnJSThreadWithoutError(() -> {
								reject.executeVoid(ExceptionUtil.throwableToValue(context, executor, eAdapter, errorType, ex));
								return null;
							}).get();
							return null;
						}

						executor.runOnJSThreadWithError(() -> {
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

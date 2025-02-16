package dev.argon.nobleidl.runtime.graaljsInterop;

import org.graalvm.polyglot.Context;
import org.graalvm.polyglot.PolyglotException;
import org.graalvm.polyglot.Value;
import org.graalvm.polyglot.proxy.ProxyExecutable;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Future;

public class JSPromiseAdapter<T> implements JSAdapter<Future<T>> {
	public JSPromiseAdapter(JSAdapter<T> tAdapter) {
		this.tAdapter = tAdapter;
	}

	private final JSAdapter<T> tAdapter;

	@Override
	public Future<T> fromJS(Context context, JSExecutor executor, Value value) {
		return executor.offloadJava(() -> {
			var future = new CompletableFuture<T>();

			ExceptionUtil.getFutureResult(
				executor.runOnJSThread(() -> {
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
									ex = context.eval("js", "ex => { throw ex; }").execute(arguments[0]).throwException();
								}
								catch(Throwable ex2) {
									ex = ex2;
								}

								future.completeExceptionally(ex);

								T javaValue = tAdapter.fromJS(context, executor, arguments[0]);
								future.complete(javaValue);

								return context.eval("js", "void 0");
							}
						}
					);

					return null;
				})
			);

			return ExceptionUtil.getFutureResult(future);
		});
	}

	@Override
	public Value toJS(Context context, JSExecutor executor, Future<T> value) {
		return context.eval("js", "executor => new Promise(executor)").execute(
			new ProxyExecutable() {
				@Override
				public Object execute(Value... arguments) {
					if(arguments.length < 2) {
						throw new IllegalArgumentException("Expected two arguments for promise executor");
					}

					Value resolve = arguments[0];
					Value reject = arguments[1];

					executor.offloadJava(() -> {
						T javaValue;
						try {
							javaValue = ExceptionUtil.getFutureResult(value);
						}
						catch(PolyglotException ex) {
							ExceptionUtil.getFutureResult(executor.runOnJSThread(() -> {
								reject.executeVoid(ex.getGuestObject());
								return null;
							}));
							return null;
						}
						catch(Throwable ex) {
							ExceptionUtil.getFutureResult(executor.runOnJSThread(() -> {
								reject.executeVoid(ex);
								return null;
							}));
							return null;
						}

						ExceptionUtil.getFutureResult(executor.runOnJSThread(() -> {
							Value jsValue = tAdapter.toJS(context, executor, javaValue);
							resolve.executeVoid(jsValue);
							return null;
						}));

						return null;
					});

					return context.eval("js", "void 0");
				}
			}
		);
	}
}

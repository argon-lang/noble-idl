package dev.argon.nobleidl.runtime.graaljsInterop;

import dev.argon.nobleidl.runtime.Unit;
import org.graalvm.polyglot.Context;
import org.graalvm.polyglot.PolyglotException;
import org.graalvm.polyglot.Value;

import java.util.concurrent.Future;
import java.util.function.Supplier;

public class CallUtil {

	public static <T> T callJSFunction(Context context, JSExecutor executor, JSAdapter<T> tAdapter, Supplier<Value> f) {
		return ExceptionUtil.getFutureResultUnchecked(ExceptionUtil.getFutureResultUnchecked(
			executor.runOnJSThread(() -> {
				Value jsPromise = ExceptionUtil.runUnwrappingExceptions(f);
				return new JSPromiseAdapter<>(tAdapter).fromJS(context, executor, jsPromise);
			})
		));
	}

	public static <T, E extends Throwable> T callJSFunction(Context context, JSExecutor executor, JSAdapter<T> tAdapter, String errorTypeName, JSAdapter<E> eAdapter, Supplier<Value> f) throws E, InterruptedException {
		return ExceptionUtil.<T, E>getFutureResult(ExceptionUtil.<Future<T>, E>getFutureResult(
			executor.runOnJSThread(() -> {
				Value jsPromise = ExceptionUtil.runUnwrappingExceptions(context, executor, errorTypeName, eAdapter, f);
				return new JSPromiseAdapter<>(tAdapter)
					.fromJS(context, executor, jsPromise);
			})
		));
	}

	public static <T> Value callJavaFunction(Context context, JSExecutor executor, JSAdapter<T> tAdapter, JSExecutor.Callback<T> f) {
		return new JSPromiseAdapter<>(tAdapter).toJS(context, executor, executor.offloadJava(f));
	}

	public static <T, E extends Throwable> Value callJavaFunction(Context context, JSExecutor executor, JSAdapter<T> tAdapter, Class<E> exceptionClass, JSAdapter<E> eAdapter, JSExecutor.Callback<T> f) {
		return new JSPromiseAdapter<>(tAdapter).toJS(context, executor, executor.offloadJava(() -> {
			try {
				return f.run();
			}
			catch(Throwable ex) {
				if(!exceptionClass.isInstance(ex)) {
					throw ex;
				}

				E e = exceptionClass.cast(ex);

				throw ExceptionUtil.getFutureResult(executor.runOnJSThread(() -> {
					var jsError = eAdapter.toJS(context, executor, e);
					return context.eval("js", "e => { throw e; }").execute(jsError).throwException();
				}));
			}
		}));
	}

	public static <T> Value callJavaFunction(Context context, JSExecutor executor, JSAdapter<Unit> tAdapter, JavaCallableVoid f) {
		return callJavaFunction(context, executor, tAdapter, () -> {
			f.call();
			return Unit.INSTANCE;
		});
	}

	public static <E extends Throwable> Value callJavaFunction(Context context, JSExecutor executor, JSAdapter<Unit> tAdapter, Class<E> exceptionClass, JSAdapter<E> eAdapter, JavaCallableVoid f) {
		return callJavaFunction(context, executor, tAdapter, exceptionClass, eAdapter, () -> {
			f.call();
			return Unit.INSTANCE;
		});
	}

	public interface JavaCallableVoid {
		void call() throws Throwable;
	}
}

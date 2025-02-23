package dev.argon.nobleidl.runtime.graaljsInterop;

import dev.argon.nobleidl.runtime.ErrorType;
import dev.argon.nobleidl.runtime.Unit;
import org.graalvm.polyglot.Context;
import org.graalvm.polyglot.PolyglotException;
import org.graalvm.polyglot.Value;
import org.graalvm.polyglot.proxy.ProxyExecutable;

import java.util.concurrent.Future;
import java.util.function.Supplier;

public class CallUtil {

	public static <T> T callJSFunction(Context context, JSExecutor executor, JSAdapter<T> tAdapter, Supplier<Value> f) throws InterruptedException {
		return executor.runOnJSThreadWithoutError(() -> {
			Value jsPromise = ExceptionUtil.unwrapJavaScriptException(context, f);
			return new JSPromiseAdapter<>(tAdapter).fromJS(context, executor, jsPromise);
		}).get().get();
	}

	public static <T, E, EX extends Throwable> T callJSFunction(Context context, JSExecutor executor, JSAdapter<T> tAdapter, JSAdapter<E> eAdapter, ErrorType<E, ? extends EX> errorType, Supplier<Value> errorCheckerSupplier, Supplier<Value> f) throws EX, InterruptedException {
		return executor.runOnJSThreadWithError(() -> {
			Value errorChecker = errorCheckerSupplier.get();
			Value jsPromise = ExceptionUtil.unwrapJavaScriptException(context, executor, eAdapter, errorType, errorChecker, f);
			return new JSPromiseWithErrorAdapter<>(tAdapter, eAdapter).fromJS(context, executor, errorType, errorChecker, jsPromise);
		}).get().get();
	}

	public static <T> Value callJavaFunction(Context context, JSExecutor executor, JSAdapter<T> tAdapter, JSExecutor.CallbackWithoutError<T> f) {
		return new JSPromiseAdapter<>(tAdapter).toJS(context, executor, executor.offloadJavaWithoutError(f));
	}

	public static <T, E, EX extends Throwable> Value callJavaFunction(Context context, JSExecutor executor, JSAdapter<T> tAdapter, JSAdapter<E> eAdapter, ErrorType<E, ? extends EX> errorType, JSExecutor.CallbackWithError<T, EX> f) {
		return new JSPromiseWithErrorAdapter<>(tAdapter, eAdapter).toJS(context, executor, errorType, executor.offloadJavaWithError(f));
	}

	public static <T> Value callJavaFunction(Context context, JSExecutor executor, JSAdapter<Unit> tAdapter, JavaCallableVoid f) {
		return callJavaFunction(context, executor, tAdapter, () -> {
			f.call();
			return Unit.INSTANCE;
		});
	}

	public static <E, EX extends Throwable> Value callJavaFunction(Context context, JSExecutor executor, JSAdapter<Unit> tAdapter, JSAdapter<E> eAdapter, ErrorType<E, ? extends EX> errorType, JavaCallableVoidWithError<EX> f) {
		return CallUtil.<Unit, E, EX>callJavaFunction(context, executor, tAdapter, eAdapter, errorType, () -> {
			f.call();
			return Unit.INSTANCE;
		});
	}

	public static Object makeJavaScriptFunction(JavaScriptFunction f) {
		return new ProxyExecutable() {
			@Override
			public Object execute(Value... arguments) {
				try {
					return f.call(arguments);
				}
				catch(InterruptedException ex) {
					throw unsafeThrowAs(ex);
				}
			}
		};
	}

	@FunctionalInterface
	public interface JavaCallableVoid {
		void call() throws InterruptedException;
	}

	@FunctionalInterface
	public interface JavaCallableVoidWithError<E extends Throwable> {
		void call() throws InterruptedException, E;
	}

	@FunctionalInterface
	public interface JavaScriptFunction {
		Object call(Value... arguments) throws InterruptedException;
	}

	private static <E extends Throwable> RuntimeException unsafeThrowAs(Throwable ex) throws E {
		@SuppressWarnings("unchecked")
		E e = (E)ex;

		throw e;
	}
}

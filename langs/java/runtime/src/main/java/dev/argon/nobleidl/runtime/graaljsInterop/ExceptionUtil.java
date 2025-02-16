package dev.argon.nobleidl.runtime.graaljsInterop;

import dev.argon.nobleidl.runtime.ErrorType;
import org.graalvm.polyglot.Context;
import org.graalvm.polyglot.PolyglotException;
import org.graalvm.polyglot.Value;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.function.Supplier;

public class ExceptionUtil {
	private ExceptionUtil() {}

	public static Value runUnwrappingExceptions(Supplier<Value> f) {
		try {
			return f.get();
		}
		catch(PolyglotException pex) {
			Throwable ex = unwrapPolyglotException(pex);
			throw unsafeThrowAs(ex);
		}
	}

	public static <E extends Throwable> Value runUnwrappingExceptions(Context context, JSExecutor executor, String errorTypeName, JSAdapter<E> eAdapter, Supplier<Value> f) throws E {
		try {
			return f.get();
		}
		catch(PolyglotException pex) {
			Throwable ex = unwrapPolyglotException(context, executor, errorTypeName, eAdapter, pex);
			throw ExceptionUtil.<E>unsafeThrowAs(ex);
		}
	}

	private static Throwable unwrapPolyglotException(PolyglotException ex) {
		if(ex.isHostException()) {
			return ex.asHostException();
		}

		var jsError = ex.getGuestObject();
		if(jsError == null) {
			return ex;
		}

		return unwrapJSException(jsError);
	}

	private static <E extends Throwable> Throwable unwrapPolyglotException(Context context, JSExecutor executor, String errorTypeName, JSAdapter<E> eAdapter, PolyglotException ex) {
		if(ex.isHostException()) {
			return ex.asHostException();
		}

		var jsError = ex.getGuestObject();
		if(jsError == null) {
			return ex;
		}

		var checkError = context.eval("js", "(errorName, e) => { const sym = Symbol.for('nobleidl-error-type'); return e instanceof Error && sym in e && e[sym] == errorName");
		if(checkError.execute(errorTypeName, jsError).asBoolean()) {
			return eAdapter.fromJS(context, executor, jsError);
		}

		return unwrapJSException(jsError);
	}

	public static Throwable unwrapJSException(Value value) {
		if(value == null || value.isNull()) {
			return null;
		}

		if(value.isHostObject() && value.<Object>asHostObject() instanceof Throwable ex) {
			return ex;
		}

		try {
			return value.throwException();
		}
		catch(Throwable ex) {
			return ex;
		}
	}

	public static Value wrapJSException(Context context, Throwable ex) {
		if(ex instanceof PolyglotException pex) {
			Value obj = pex.getGuestObject();
			if(obj != null) {
				return obj;
			}
		}

		return context.asValue(ex);
	}

	public static <A, E extends Throwable> A getFutureResult(Future<A> future) throws InterruptedException, E {
		try {
			return future.get();
		}
		catch(ExecutionException ex) {
			if(ex.getCause() == null) {
				throw ExceptionUtil.<E>unsafeThrowAs(ex);
			}

			throw ExceptionUtil.<E>unsafeThrowAs(ex.getCause());
		}
	}

	public static <A, E extends Throwable> A getFutureResultUnchecked(Future<A> future) {
		try {
			return getFutureResult(future);
		}
		catch(Throwable ex) {
			throw ExceptionUtil.<RuntimeException>unsafeThrowAs(ex);
		}
	}

	private static <E extends Throwable> RuntimeException unsafeThrowAs(Throwable ex) throws E {
		@SuppressWarnings("unchecked")
		E e = (E)ex;

		throw e;
	}
}

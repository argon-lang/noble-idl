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

	static <E, EX extends Throwable> Value unwrapJavaScriptException(Context context, Supplier<Value> f) throws InterruptedException {
		try {
			return f.get();
		}
		catch(PolyglotException pex) {
			if(pex.isHostException()) {
				throw unsafeThrowAs(pex.asHostException());
			}

			Value guestObj = pex.getGuestObject();
			if(guestObj != null) {
				throw unsafeThrowAs(valueToThrowable(context, guestObj));
			}

			throw pex;
		}
	}

	static <E, EX extends Throwable> Value unwrapJavaScriptException(Context context, JSExecutor executor, JSAdapter<E> adapter, ErrorType<E, ? extends EX> errorType, Value errorChecker, Supplier<Value> f) throws InterruptedException, EX {
		try {
			return f.get();
		}
		catch(PolyglotException pex) {
			if(pex.isHostException()) {
				throw unsafeThrowAs(pex.asHostException());
			}

			Value guestObj = pex.getGuestObject();
			if(guestObj != null) {
				throw unsafeThrowAs(valueToThrowable(context, executor, adapter, errorType, errorChecker, guestObj));
			}

			throw pex;
		}
	}

	public static Value throwableToValue(Context context, Throwable ex) {
		if(ex == null) {
			return context.asValue(null);
		}

		if(ex instanceof PolyglotException pex) {
			if(pex.isHostException()) {
				return throwableToValue(context, pex.asHostException());
			}

			Value guestObj = pex.getGuestObject();
			if(guestObj != null) {
				return guestObj;
			}

		}

		return context.asValue(ex);
	}

	static <E, EX extends Throwable> Value throwableToValue(Context context, JSExecutor executor, JSAdapter<E> adapter, ErrorType<E, EX> errorType, Throwable ex) throws InterruptedException {
		E e = errorType.tryFromThrowable(ex);
		if(e != null) {
			return adapter.toJS(context, executor, e);
		}

		if(ex instanceof PolyglotException pex) {
			if(pex.isHostException()) {
				return throwableToValue(context, pex.asHostException());
			}

			Value guestObj = pex.getGuestObject();
			if(guestObj != null) {
				return guestObj;
			}

		}

		return context.asValue(ex);
	}

	public static Throwable valueToThrowable(Context context, Value error) {
		if(error == null) {
			return null;
		}

		if(error.isHostObject() && error.<Object>asHostObject() instanceof Throwable ex) {
			return ex;
		}

		if(error.isException()) {
			try {
				return error.throwException();
			}
			catch(Throwable ex) {
				return ex;
			}
		}

		try {
			return context.eval("js", "ex => { throw ex; }").execute(error).throwException();
		}
		catch(Throwable ex) {
			return ex;
		}
	}

	static <E, EX extends Throwable> Throwable valueToThrowable(Context context, JSExecutor executor, JSAdapter<E> adapter, ErrorType<E, EX> errorType, Value errorChecker, Value error) throws InterruptedException {
		if(errorChecker.invokeMember("isInstance", error).asBoolean()) {
			return errorType.toThrowable(adapter.fromJS(context, executor, error));
		}

		if(error.isHostObject() && error.<Object>asHostObject() instanceof Throwable ex) {
			return ex;
		}

		if(error.isException()) {
			try {
				return error.throwException();
			}
			catch(Throwable ex) {
				return ex;
			}
		}

		try {
			return context.eval("js", "ex => { throw ex; }").execute(error).throwException();
		}
		catch(Throwable ex) {
			return ex;
		}
	}

	private static <E extends Throwable> RuntimeException unsafeThrowAs(Throwable ex) throws E {
		@SuppressWarnings("unchecked")
		E e = (E)ex;

		throw e;
	}
}

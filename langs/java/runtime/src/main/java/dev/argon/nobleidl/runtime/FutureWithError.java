package dev.argon.nobleidl.runtime;

import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

public class FutureWithError<T, E extends Throwable> {
	private FutureWithError(Future<T> future) {
		this.future = future;
	}

	private final Future<T> future;


	public void cancel() {
		future.cancel(true);
	}

	public T get() throws InterruptedException, E {
		try {
			return future.get();
		}
		catch(CancellationException ex) {
			throw new InterruptedException(ex.getMessage());
		} catch(ExecutionException ex) {
			Throwable cause = ex.getCause();
			if(cause == null) {
				cause = new RuntimeException(ex.getMessage());
			}

			throw unsafeThrowAs(cause);
		}
	}

	public static <T, E extends Throwable> FutureWithError<T, E> fromFutureUnsafe(Future<T> future) {
		return new FutureWithError<>(future);
	}

	private static <E extends Throwable> RuntimeException unsafeThrowAs(Throwable ex) throws E {
		@SuppressWarnings("unchecked")
		E e = (E)ex;

		throw e;
	}
}

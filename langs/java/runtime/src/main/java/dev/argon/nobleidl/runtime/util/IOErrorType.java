package dev.argon.nobleidl.runtime.util;

import dev.argon.nobleidl.runtime.ErrorType;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.io.InterruptedIOException;

public abstract class IOErrorType<T, E extends IOException> implements ErrorType<T, E> {

	public <A, E2 extends Throwable> A catchingIO(TryFunctionIO<A, E> f, HandlerFunctionRethrow<A, T, E2> handler) throws InterruptedException, E2 {
		try {
			return f.run();
		}
		catch(InterruptedIOException e) {
			var ex = new InterruptedException(e.getMessage());
			ex.setStackTrace(e.getStackTrace());
			throw ex;
		}

		catch(IOException ex) {
			T error = tryFromThrowable(ex);
			if(error != null) {
				return handler.handle(error);
			}

			throw unsafeThrowAs(ex);
		}
	}

	public <E2 extends Throwable> void catchingIO(TryFunctionIOVoid<E> f, HandlerFunctionRethrowVoid<T, E2> handler) throws InterruptedException, E2 {
		try {
			f.run();
		}
		catch(InterruptedIOException e) {
			var ex = new InterruptedException(e.getMessage());
			ex.setStackTrace(e.getStackTrace());
			throw ex;
		}

		catch(IOException ex) {
			T error = tryFromThrowable(ex);
			if(error != null) {
				handler.handle(error);
				return;
			}

			throw unsafeThrowAs(ex);
		}
	}

	public <A, E2 extends Throwable> A catchingIOWithInterruptIO(TryFunctionIO<A, E> f, HandlerFunctionIOInterruptRethrow<A, T, E2> handler) throws InterruptedIOException, E2 {
		try {
			return f.run();
		}
		catch(InterruptedIOException e) {
			throw e;
		}

		catch(IOException ex) {
			T error;
			try {
				error = tryFromThrowable(ex);
			}
			catch(InterruptedException ie) {
				var ieio = new InterruptedIOException(ie.getMessage());
				ieio.setStackTrace(ie.getStackTrace());
				throw ieio;
			}
			if(error != null) {
				return handler.handle(error);
			}

			throw unsafeThrowAs(ex);
		}
	}

	public <E2 extends Throwable> void catchingIOWithInterruptIO(TryFunctionIOVoid<E> f, HandlerFunctionIOInterruptRethrowVoid<T, E2> handler) throws InterruptedIOException, E2 {
		try {
			f.run();
		}
		catch(InterruptedIOException e) {
			throw e;
		}

		catch(IOException ex) {
			T error;
			try {
				error = tryFromThrowable(ex);
			}
			catch(InterruptedException ie) {
				var ieio = new InterruptedIOException(ie.getMessage());
				ieio.setStackTrace(ie.getStackTrace());
				throw ieio;
			}
			if(error != null) {
				handler.handle(error);
				return;
			}

			throw unsafeThrowAs(ex);
		}
	}


	public static <T> IOErrorType<T, ?> fromErrorType(ErrorType<T, ?> errorType) {
		return new IOErrorTypeImpl<>(errorType);
	}

	private static final class WrappedErrorIOException extends IOException {
		public WrappedErrorIOException(Object error) {
			super(error instanceof Throwable cause ? cause : null);
			this.error = error;
		}

		private final Object error;

		public Object getError() {
			return error;
		}
	}

	private static final class IOErrorTypeImpl<T> extends IOErrorType<T, WrappedErrorIOException> {
		public IOErrorTypeImpl(ErrorType<T, ?> errorType) {
			this.errorType = errorType;
		}

		private final ErrorType<T, ?> errorType;

		@Override
		public @Nullable T tryFromObject(Object obj) throws InterruptedException {
			return errorType.tryFromObject(obj);
		}

		@Override
		public @Nullable T tryFromThrowable(Throwable ex) throws InterruptedException {
			if(ex instanceof WrappedErrorIOException wex) {
				return tryFromObject(wex.getError());
			}
			else {
				return null;
			}
		}

		@Override
		public WrappedErrorIOException toThrowable(T error) throws InterruptedException {
			return new WrappedErrorIOException(error);
		}
	}


	@FunctionalInterface
	public interface TryFunctionIO<A, E extends IOException> {
		A run() throws InterruptedIOException, E;
	}

	@FunctionalInterface
	public interface TryFunctionIOVoid<E extends IOException> {
		void run() throws InterruptedIOException, E;
	}

	@FunctionalInterface
	public interface HandlerFunctionIOInterruptRethrow<A, T, E2 extends Throwable> {
		A handle(T error) throws InterruptedIOException, E2;
	}

	@FunctionalInterface
	public interface HandlerFunctionIOInterruptRethrowVoid<T, E2 extends Throwable> {
		void handle(T error) throws InterruptedIOException, E2;
	}

	private static <E extends Throwable> RuntimeException unsafeThrowAs(Throwable ex) throws E {
		@SuppressWarnings("unchecked")
		E e = (E)ex;

		throw e;
	}
}

package dev.argon.nobleidl.runtime;

import org.jetbrains.annotations.Nullable;

public interface ErrorType<T, E extends Throwable> {
	@Nullable T tryFromObject(Object obj) throws InterruptedException;
	@Nullable T tryFromThrowable(Throwable ex) throws InterruptedException;
	E toThrowable(T error) throws InterruptedException;

	default boolean objectIsError(Object obj) throws InterruptedException {
		return tryFromObject(obj) != null;
	}

	default boolean exceptionIsError(Throwable ex) throws InterruptedException {
		return tryFromThrowable(ex) != null;
	}

	default <A, E2 extends Throwable> A catching(TryFunction<A, E> f, HandlerFunctionRethrow<A, T, E2> handler) throws InterruptedException, E2 {
		try {
			return f.run();
		}
		catch(Throwable ex) {
			T error = tryFromThrowable(ex);
			if(error != null) {
				return handler.handle(error);
			}

			throw unsafeThrowAs(ex);
		}
	}

	default <E2 extends Throwable> void catching(TryFunctionVoid<E> f, HandlerFunctionRethrowVoid<T, E2> handler) throws InterruptedException, E2 {
		try {
			f.run();
		}
		catch(Throwable ex) {
			T error = tryFromThrowable(ex);
			if(error != null) {
				handler.handle(error);
				return;
			}

			throw unsafeThrowAs(ex);
		}
	}

	static <E extends Throwable> ErrorType<E, E> fromClass(Class<E> cls) {
		return new ErrorType<>() {
			@Override
			public @Nullable E tryFromObject(Object obj) {
				if(cls.isInstance(obj)) {
					return cls.cast(obj);
				}
				else {
					return null;
				}
			}

			@Override
			public E tryFromThrowable(Throwable ex) {
				return tryFromObject(ex);
			}

			@Override
			public E toThrowable(E error) {
				return error;
			}

			@Override
			public boolean objectIsError(Object obj) {
				return cls.isInstance(obj);
			}

			@Override
			public boolean exceptionIsError(Throwable ex) {
				return objectIsError(ex);
			}
		};
	}

	@FunctionalInterface
	interface TryFunction<A, E extends Throwable> {
		A run() throws InterruptedException, E;
	}

	@FunctionalInterface
	interface TryFunctionVoid<E extends Throwable> {
		void run() throws InterruptedException, E;
	}

	@FunctionalInterface
	interface HandlerFunctionRethrow<A, T, E2 extends Throwable> {
		A handle(T error) throws InterruptedException, E2;
	}

	@FunctionalInterface
	interface HandlerFunctionRethrowVoid<T, E2 extends Throwable> {
		void handle(T error) throws InterruptedException, E2;
	}

	private static <E extends Throwable> RuntimeException unsafeThrowAs(Throwable ex) throws E {
		@SuppressWarnings("unchecked")
		E e = (E)ex;

		throw e;
	}
}

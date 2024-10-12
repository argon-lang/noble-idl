package dev.argon.nobleidl.runtime;

public interface ErrorType<E extends Throwable> {
	E tryFromThrowable(Throwable ex);

	default boolean isError(Throwable ex) {
		return tryFromThrowable(ex) != null;
	}

	static <E extends Throwable> ErrorType<E> fromClass(Class<E> cls) {
		return new ErrorType<E>() {
			@Override
			public E tryFromThrowable(Throwable ex) {
				if(cls.isInstance(ex)) {
					@SuppressWarnings("unchecked")
					E e = (E)ex;

					return e;
				}
				else {
					return null;
				}
			}

			@Override
			public boolean isError(Throwable ex) {
				return cls.isInstance(ex);
			}
		};
	}
}

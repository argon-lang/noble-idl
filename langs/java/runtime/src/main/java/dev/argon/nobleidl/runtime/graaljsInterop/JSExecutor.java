package dev.argon.nobleidl.runtime.graaljsInterop;

import dev.argon.nobleidl.runtime.FutureWithError;
import dev.argon.nobleidl.runtime.FutureWithoutError;

public interface JSExecutor {
	<A, E extends Throwable> FutureWithError<A, E> runOnJSThreadWithError(CallbackWithError<A, E> callback);
	<A> FutureWithoutError<A> runOnJSThreadWithoutError(CallbackWithoutError<A> callback);
	<A, E extends Throwable> FutureWithError<A, E> offloadJavaWithError(CallbackWithError<A, E> callback);
	<A> FutureWithoutError<A> offloadJavaWithoutError(CallbackWithoutError<A> callback);

	interface CallbackWithError<A, E extends Throwable> {
		A run() throws InterruptedException, E;
	}

	interface CallbackWithoutError<A> {
		A run() throws InterruptedException;
	}
}


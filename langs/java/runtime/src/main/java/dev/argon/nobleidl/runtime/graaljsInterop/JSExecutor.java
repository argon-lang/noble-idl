package dev.argon.nobleidl.runtime.graaljsInterop;

import dev.argon.nobleidl.runtime.FutureWithError;
import dev.argon.nobleidl.runtime.FutureWithoutError;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

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

	public static JSExecutor fromExecutors(Executor jsExecutor, Executor javaExecutor) {
		return new JSExecutor() {
			@Override
			public <A, E extends Throwable> FutureWithError<A, E> runOnJSThreadWithError(CallbackWithError<A, E> callback) {
				var future = new CompletableFuture<A>();
				jsExecutor.execute(() -> {
					A result;
					try {
						result = callback.run();
					}
					catch(Throwable ex) {
						future.completeExceptionally(ex);
						return;
					}

					future.complete(result);
				});

				return FutureWithError.fromFutureUnsafe(future);
			}

			@Override
			public <A> FutureWithoutError<A> runOnJSThreadWithoutError(CallbackWithoutError<A> callback) {
				var future = new CompletableFuture<A>();
				jsExecutor.execute(() -> {
					A result;
					try {
						result = callback.run();
					}
					catch(Throwable ex) {
						future.completeExceptionally(ex);
						return;
					}

					future.complete(result);
				});

				return FutureWithoutError.fromFutureUnsafe(future);
			}

			@Override
			public <A, E extends Throwable> FutureWithError<A, E> offloadJavaWithError(CallbackWithError<A, E> callback) {
				var future = new CompletableFuture<A>();
				javaExecutor.execute(() -> {
					A result;
					try {
						result = callback.run();
					}
					catch(Throwable ex) {
						future.completeExceptionally(ex);
						return;
					}

					future.complete(result);
				});

				return FutureWithError.fromFutureUnsafe(future);
			}

			@Override
			public <A> FutureWithoutError<A> offloadJavaWithoutError(CallbackWithoutError<A> callback) {
				var future = new CompletableFuture<A>();
				javaExecutor.execute(() -> {
					A result;
					try {
						result = callback.run();
					}
					catch(Throwable ex) {
						future.completeExceptionally(ex);
						return;
					}

					future.complete(result);
				});

				return FutureWithoutError.fromFutureUnsafe(future);
			}
		};
	}
}


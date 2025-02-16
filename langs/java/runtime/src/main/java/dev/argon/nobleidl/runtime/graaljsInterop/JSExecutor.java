package dev.argon.nobleidl.runtime.graaljsInterop;

import org.graalvm.polyglot.Value;

import java.util.concurrent.Future;

public interface JSExecutor {
	<A> Future<A> runOnJSThread(Callback<A> callback);
	<A> Future<A> offloadJava(Callback<A> callback);

	interface Callback<A> {
		A run() throws Throwable;
	}
}


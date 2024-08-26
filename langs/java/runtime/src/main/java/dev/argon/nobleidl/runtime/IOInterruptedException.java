package dev.argon.nobleidl.runtime;

import java.io.IOException;

public class IOInterruptedException extends IOException implements WrappedInterruptedException {
	public IOInterruptedException(InterruptedException cause) {
		super(cause);
	}

	@Override
	public synchronized InterruptedException getCause() {
		return (InterruptedException)super.getCause();
	}
}

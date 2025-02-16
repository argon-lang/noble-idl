package dev.argon.nobleidl.runtime;

public abstract class NobleIDLException extends Exception {
	public NobleIDLException() {}
	public NobleIDLException(java.lang.String message) {
		super(message);
	}
	public NobleIDLException(Throwable cause) {
		super(cause);
	}
	public NobleIDLException(java.lang.String message, Throwable cause) {
		super(message, cause);
	}
}

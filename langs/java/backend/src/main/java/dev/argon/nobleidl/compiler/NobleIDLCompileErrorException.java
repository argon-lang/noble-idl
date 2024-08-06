package dev.argon.nobleidl.compiler;

public class NobleIDLCompileErrorException extends Exception {
	public NobleIDLCompileErrorException() {}

	public NobleIDLCompileErrorException(String message) {
		super(message);
	}

	public NobleIDLCompileErrorException(Throwable cause) {
		super(cause);
	}

	public NobleIDLCompileErrorException(String message, Throwable cause) {
		super(message, cause);
	}
}

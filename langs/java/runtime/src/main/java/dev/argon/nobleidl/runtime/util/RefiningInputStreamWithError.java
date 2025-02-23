package dev.argon.nobleidl.runtime.util;

import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.io.InputStream;
import java.io.InterruptedIOException;

public abstract class RefiningInputStreamWithError<E extends IOException> extends InputStreamWithError<E> {
	public RefiningInputStreamWithError(InputStream is) {
		this.is = is;
	}

	public static InputStreamWithError<IOException> forIOException(InputStream is) {
		return new RefiningInputStreamWithError<>(is) {
			@Override
			protected RuntimeException wrapIOException(IOException ex) throws IOException, InterruptedIOException {
				throw ex;
			}
		};
	}

	private final InputStream is;

	protected abstract RuntimeException wrapIOException(IOException ex) throws E, InterruptedIOException;

	@Override
	public int read() throws E, InterruptedIOException {
		try {
			return is.read();
		}
		catch(InterruptedIOException ex) {
			throw ex;
		}
		catch(IOException ex) {
			throw wrapIOException(ex);
		}
	}

	@Override
	public int read(byte @NotNull [] b) throws E, InterruptedIOException {
		try {
			return is.read(b);
		}
		catch(InterruptedIOException ex) {
			throw ex;
		}
		catch(IOException ex) {
			throw wrapIOException(ex);
		}
	}

	@Override
	public int read(byte @NotNull [] b, int off, int len) throws E, InterruptedIOException {
		try {
			return is.read(b, off, len);
		}
		catch(InterruptedIOException ex) {
			throw ex;
		}
		catch(IOException ex) {
			throw wrapIOException(ex);
		}
	}

	@Override
	public void close() throws E, InterruptedIOException {
		try {
			is.close();
		}
		catch(InterruptedIOException ex) {
			throw ex;
		}
		catch(IOException ex) {
			throw wrapIOException(ex);
		}
	}
}

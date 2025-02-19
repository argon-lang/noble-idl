package dev.argon.nobleidl.runtime.util;

import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.io.InputStream;
import java.io.InterruptedIOException;
import java.io.OutputStream;
import java.util.Objects;
import java.util.function.Function;

public abstract class InputStreamWithError<E extends IOException> extends InputStream {

	@Override
	public abstract int read() throws E, InterruptedIOException;

	@Override
	public int read(byte @NotNull[] b) throws E, InterruptedIOException {
		return read(b, 0, b.length);
	}

	@Override
	public int read(byte @NotNull[] b, int off, int len) throws E, InterruptedIOException {
		Objects.checkFromIndexSize(off, len, b.length);

		if(len == 0) {
			return 0;
		}

		int i;
		for(i = 0; i < len; ++i) {
			int value = read();
			if(value < 0) {
				break;
			}

			b[off + i] = (byte)value;
		}

		return i;
	}

	@Override
	public void close() throws E, InterruptedIOException {}



	public final <T, U, E2 extends IOException> InputStreamWithError<E2> convertError(IOErrorType<T, E> source, Function<T, U> t2u, IOErrorType<U, E2> result) {
		return new InputStreamErrorConverter<>(source, t2u, result);
	}

	private class InputStreamErrorConverter<T, U, E2 extends IOException> extends InputStreamWithError<E2> {
		public InputStreamErrorConverter(IOErrorType<T, E> source, Function<T, U> t2u, IOErrorType<U, E2> result) {
			this.source = source;
			this.t2u = t2u;
			this.result = result;
		}

		private final IOErrorType<T, E> source;
		private final Function<T, U> t2u;
		private final IOErrorType<U, E2> result;

		@Override
		public int read() throws E2, InterruptedIOException {
			return source.<Integer, E2>catchingIOWithInterruptIO(
				() -> InputStreamWithError.this.read(),
				t -> { throw convertError(t); }
			);
		}

		@Override
		public int read(byte @NotNull [] b) throws E2, InterruptedIOException {
			return source.<Integer, E2>catchingIOWithInterruptIO(
				() -> InputStreamWithError.this.read(b),
				t -> { throw convertError(t); }
			);
		}

		@Override
		public int read(byte @NotNull [] b, int off, int len) throws E2, InterruptedIOException {
			return source.<Integer, E2>catchingIOWithInterruptIO(
				() -> InputStreamWithError.this.read(b),
				t -> { throw convertError(t); }
			);
		}

		@Override
		public void close() throws E2, InterruptedIOException {
			source.catchingIOWithInterruptIO(
				InputStreamWithError.this::close,
				t -> { throw convertError(t); }
			);
		}

		private E2 convertError(T error) throws InterruptedIOException {
			try {
				return result.toThrowable(t2u.apply(error));
			}
			catch(InterruptedException ie) {
				var ieio = new InterruptedIOException(ie.getMessage());
				ieio.setStackTrace(ie.getStackTrace());
				throw ieio;
			}
		}
	}
}

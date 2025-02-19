package dev.argon.nobleidl.runtime.util;

import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.io.InterruptedIOException;
import java.io.OutputStream;
import java.util.Objects;

public abstract class OutputStreamWithError<E extends IOException> extends OutputStream {
	@Override
	public abstract void write(int b) throws InterruptedIOException, E;

	@Override
	public void write(byte @NotNull[] b) throws InterruptedIOException, E {
		write(b, 0, b.length);
	}

	@Override
	public void write(byte @NotNull[] b, int off, int len) throws InterruptedIOException, E {
		Objects.checkFromIndexSize(off, len, b.length);

		for(int i = 0; i < len; ++i) {
			write(b[off + i]);
		}
	}

	@Override
	public void flush() throws InterruptedIOException, E {}

	@Override
	public void close() throws InterruptedIOException, E {}
}

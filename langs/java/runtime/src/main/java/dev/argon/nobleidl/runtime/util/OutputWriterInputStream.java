package dev.argon.nobleidl.runtime.util;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.*;
import java.util.Objects;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

public abstract class OutputWriterInputStream<E extends IOException> extends InputStreamWithError<E> {

	public OutputWriterInputStream() {
		writerThread = Thread.startVirtualThread(this::writerThreadFunc);
	}

	private final Thread writerThread;
	private final ReentrantLock lock = new ReentrantLock();
	private final Condition readReadyCond = lock.newCondition();
	private final Condition writeReadyCond = lock.newCondition();

	private boolean isReaderClosed = false;
	private boolean isWriterClosed = false;
	private @Nullable Throwable thrownError = null;
	private byte @Nullable[] data = null;
	private int dataOffset;
	private int dataLength;

	protected abstract void outputWriter(@NotNull OutputStreamWithError<E> os) throws InterruptedIOException, E;

	@Override
	public int read() throws E, InterruptedIOException {
		byte[] b = new byte[1];
		int bytesRead;
		do {
			bytesRead = read(b);
			if(bytesRead < 0) {
				return -1;
			}
		} while(read(b) < 1);
		return Byte.toUnsignedInt(b[0]);
	}

	@Override
	public int read(byte @NotNull[] b, int off, int len) throws E, InterruptedIOException {
		Objects.checkFromIndexSize(off, len, b.length);

		try {
			lock.lockInterruptibly();
			try {
				while(true) {
					if(isReaderClosed) {
						return -1;
					}

					if(thrownError != null) {
						var ex = thrownError;
						thrownError = null;
						throw throwUnsafe(ex);
					}

					if(data != null) {
						if(len < dataLength) {
							System.arraycopy(data, dataOffset, b, off, len);
							dataOffset += len;
							dataLength -= len;
							return len;
						}
						else {
							System.arraycopy(data, dataOffset, b, off, dataLength);
							data = null;
							writeReadyCond.signalAll();
							return dataLength;
						}
					}

					if(isWriterClosed || !writerThread.isAlive()) {
						return -1;
					}

					readReadyCond.await();
				}
			}
			finally {
				lock.unlock();
			}
		}
		catch(InterruptedException _) {
			throw new InterruptedIOException();
		}
	}

	@Override
	public void close() throws E, InterruptedIOException {
		try {
			lock.lockInterruptibly();
			try {
				if(!isReaderClosed) {
					isReaderClosed = true;
					writeReadyCond.signalAll();
					readReadyCond.signalAll();
				}
			}
			finally {
				lock.unlock();
			}
		}
		catch(InterruptedException _) {
			throw new InterruptedIOException();
		}

		writerThread.interrupt();
	}

	private final class ConsumingOutputStream extends OutputStreamWithError<E> {
		@Override
		public void write(int b) throws InterruptedIOException, E {
			write(new byte[] { (byte)b });
		}

		@Override
		public void write(byte @NotNull[] b, int off, int len) throws InterruptedIOException, E {
			try {
				lock.lockInterruptibly();
				try {

					if(isWriterClosed || isReaderClosed) {
						throw new InterruptedIOException();
					}

					data = b;
					dataOffset = off;
					dataLength = len;

					readReadyCond.signalAll();

					while(data != null) {
						writeReadyCond.await();
						if(isReaderClosed || isWriterClosed) {
							throw new InterruptedIOException();
						}
					}
				}
				finally {
					lock.unlock();
				}
			}
			catch(InterruptedException _) {
				throw new InterruptedIOException();
			}
		}

		@Override
		public void close() throws InterruptedIOException, E {

			try {
				lock.lockInterruptibly();
				try {
					if(!isWriterClosed) {
						isWriterClosed = true;
						writeReadyCond.signalAll();
						readReadyCond.signalAll();
					}
				}
				finally {
					lock.unlock();
				}
			}
			catch(InterruptedException _) {
				throw new InterruptedIOException();
			}
		}
	}

	private void writerThreadFunc() {
		try {
			lock.lockInterruptibly();
			try {
				writeReadyCond.await();
			}
			finally {
				lock.unlock();
			}


			try {
				try(var os = new ConsumingOutputStream()) {
					outputWriter(os);
				}
			}
			catch(Throwable ex) {
				lock.lockInterruptibly();
				try {
					readReadyCond.signalAll();
				}
				finally {
					lock.unlock();
				}
			}
		}
		catch(InterruptedException e) {
			throw new RuntimeException(e);
		}
	}


	private <E2 extends Throwable> E throwUnsafe(Throwable ex) throws E2 {
		@SuppressWarnings("unchecked")
		E2 e = (E2)ex;
		throw e;
	}

}

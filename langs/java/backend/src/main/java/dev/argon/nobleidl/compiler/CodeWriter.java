package dev.argon.nobleidl.compiler;

import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.io.Writer;

class CodeWriter extends Writer {

	public CodeWriter(Writer out) {
		this.out = out;
	}

	private final Writer out;

	private int indentLevel = 0;
	private boolean atLineStart = true;

	@Override
	public void write(String str, int off, int len) throws IOException {
		if(atLineStart && len > 0) {
			atLineStart = false;
			writeIndent();
		}

		out.write(str, off, len);
	}

	@Override
	public void write(char[] cbuf, int off, int len) throws IOException {
		if(atLineStart && len > 0) {
			atLineStart = false;
			writeIndent();
		}

		out.write(cbuf, off, len);
	}

	public void print(String s) throws IOException {
		write(s);
	}

	public void println(String s) throws IOException {
		print(s);
		println();
	}

	public void println() throws IOException {
		atLineStart = true;
		out.write(System.lineSeparator());
	}


	private void writeIndent() throws IOException {
		for(int i = 0; i < indentLevel; ++i) {
			out.write("\t");
		}
	}

	public void indent() {
		++indentLevel;
	}

	public void dedent() {
		if(indentLevel <= 0) {
			throw new IllegalStateException();
		}

		--indentLevel;
	}

	@Override
	public void flush() throws IOException {
		out.flush();
	}

	@Override
	public void close() throws IOException {
		out.close();
	}

}

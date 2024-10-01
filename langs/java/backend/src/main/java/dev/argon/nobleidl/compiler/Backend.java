package dev.argon.nobleidl.compiler;

import dev.argon.nobleidl.compiler.api.NobleIdlGenerationResult;

import java.io.IOException;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

public interface Backend {
	Stream<FileGenerator> emit() throws NobleIDLCompileErrorException;

	default void emitWith(EmitHandler emitHandler) throws NobleIDLCompileErrorException, IOException {
		try(var stream = emit()) {
			var iter = stream.iterator();
			while(iter.hasNext()) {
				var generator = iter.next();
				emitHandler.emitFile(generator);
			}
		}
	}

	interface FileGenerator {
		Path getPath() throws NobleIDLCompileErrorException;
		void generate(Writer writer) throws NobleIDLCompileErrorException, IOException;
	}

	interface EmitHandler {
		void emitFile(FileGenerator generator) throws NobleIDLCompileErrorException, IOException;
	}

	final class PathEmitHandler implements EmitHandler {
		public PathEmitHandler(Path outputDir) {
			this.outputDir = outputDir;
		}

		private final Path outputDir;
		private final List<String> outputFiles = new ArrayList<>();

		@Override
		public void emitFile(FileGenerator generator) throws NobleIDLCompileErrorException, IOException {
			var path = outputDir.resolve(generator.getPath());
			outputFiles.add(path.toString());
			Files.createDirectories(path.getParent());
			try(var writer = Files.newBufferedWriter(path, StandardCharsets.UTF_8)) {
				generator.generate(writer);
			}
		}

		public NobleIdlGenerationResult result() {
			return new NobleIdlGenerationResult(new ArrayList<>(outputFiles));
		}
	}

}

package dev.argon.nobleidl.compiler;

import java.io.Closeable;
import java.io.IOException;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

import dev.argon.esexpr.DecodeException;
import dev.argon.esexpr.ESExpr;
import dev.argon.esexpr.ESExprBinaryReader;
import dev.argon.esexpr.SyntaxException;
import dev.argon.nobleidl.compiler.format.NobleIdlJarOptions;

class LibraryAnalyzer implements Closeable {
	private LibraryAnalyzer(FileSystem fs, Path libraryPath) {

		this.fs = fs;
		this.libraryPath = libraryPath;
	}

	private final FileSystem fs;
	private final Path libraryPath;

	public final Map<String, String> packageMapping = new HashMap<>();
	public final List<String> sourceFiles = new ArrayList<>();

	public static LibraryAnalyzer fromPath(Path libraryPath) throws IOException {
		if(Files.isDirectory(libraryPath)) {
			return new LibraryAnalyzer(null, libraryPath);
		}
		else if(libraryPath.getFileName().toString().endsWith(".jar")) {
			var zipFS = FileSystems.newFileSystem(libraryPath);
			var rootPath = zipFS.getRootDirectories().iterator().next();
			return new LibraryAnalyzer(zipFS, rootPath);
		}
		else {
			throw new RuntimeException("Unknown library type. Expected directory or jar.");
		}
	}



	public void scan() throws IOException {
		var optionsEsxFile = libraryPath.resolve("nobleidl-options.esxb");
		if(!Files.exists(optionsEsxFile)) {
			return;
		}

		ESExpr optionsExpr;
		try(var is = fs.provider().newInputStream(optionsEsxFile)) {
			var optionsExprOpt = ESExprBinaryReader.readEmbeddedStringTable(is).findFirst();
			if(optionsExprOpt.isEmpty()) {
				return;
			}

			optionsExpr = optionsExprOpt.get();
		}
		catch(SyntaxException ex) {
			System.err.println("Warning: could not parse nobleidl-options.esx: " + ex);
			return;
		}

		NobleIdlJarOptions options;
		try {
			options = NobleIdlJarOptions.codec().decode(optionsExpr);
		}
		catch(DecodeException ex) {
			System.err.println("Warning: could not decode nobleidl-options.esx: " + ex);
			return;
		}

		var javaOptions = options.backends().mapping().map().get("java");

		if(javaOptions == null) {
			return;
		}

		packageMapping.putAll(javaOptions.packageMapping().mapping().map());
		for(var path : options.idlFiles()) {
			var subPath = libraryPath.resolve(path);
			if(!subPath.toAbsolutePath().startsWith(libraryPath.toAbsolutePath())) {
				throw new RuntimeException("Invalid source path in nobleidl-options.esx. Paths must be within the library.");
			}

			String sourceCode = Files.readString(subPath);
			sourceFiles.add(sourceCode);
		}
	}



	@Override
	public void close() throws IOException {
		if(fs != null) {
			fs.close();
		}
	}
}

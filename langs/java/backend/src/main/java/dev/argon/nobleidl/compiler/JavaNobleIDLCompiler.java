package dev.argon.nobleidl.compiler;

import dev.argon.esexpr.ESExprBinaryWriter;
import dev.argon.esexpr.KeywordMapping;
import dev.argon.esexpr.codecs.ListCodec;
import dev.argon.nobleidl.compiler.api.*;

import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.*;

import dev.argon.nobleidl.compiler.format.NobleIdlJarOptions;
import org.apache.commons.cli.*;

public class JavaNobleIDLCompiler {
	private JavaNobleIDLCompiler() {}

	public static NobleIdlGenerationResult compile(JavaIDLCompilerOptions options) throws NobleIDLCompileErrorException, IOException {
		var modelOptions = new NobleIdlCompileModelOptions(
			options.libraryFileData(),
			options.inputFileData()
		);

		NobleIdlCompileModelResult result;
		try(var compiler = new NobleIDLCompiler()) {
			result = compiler.loadModel(modelOptions);
		}

		var model = switch(result) {
			case NobleIdlCompileModelResult.Success success -> success.model();
			case NobleIdlCompileModelResult.Failure(var errors) -> {
				throw new NobleIDLCompileErrorException(String.join("\n", errors));
			}
		};

		var backend = new JavaBackend(new NobleIdlGenerationRequest<>(options.languageOptions(), model));

		backend.emit();

		return backend.result();
	}

	public static void main(String[] args) throws IOException, NobleIDLCompileErrorException {
		Options options = new Options();

		Option input = new Option("i", "input", true, "input directory");
		input.setRequired(false);
		input.setArgs(Option.UNLIMITED_VALUES);
		options.addOption(input);

		Option sourceOutput = new Option("o", "output", true, "output directory");
		input.setRequired(true);
		options.addOption(sourceOutput);

		Option resourceOutput = new Option("r", "resource-output", true, "resource output directory");
		input.setRequired(true);
		options.addOption(resourceOutput);

		Option javaLibrary = new Option("d", "java-library", true, "path to java library");
		javaLibrary.setRequired(false);
		javaLibrary.setArgs(Option.UNLIMITED_VALUES);
		options.addOption(javaLibrary);

		Option packageMappingOpt = new Option("p", "package-mapping", true, "Map IDL packages to Java packages");
		packageMappingOpt.setRequired(false);
		packageMappingOpt.setArgs(Option.UNLIMITED_VALUES);
		options.addOption(packageMappingOpt);

		CommandLineParser parser = new DefaultParser();
		HelpFormatter formatter = new HelpFormatter();
		CommandLine cmd;

		try {
			cmd = parser.parse(options, args);
		} catch (ParseException e) {
			System.out.println(e.getMessage());
			formatter.printHelp("java-noble-idl-compiler", options);

			System.exit(1);
			return;
		}

		String[] inputDirPaths = cmd.getOptionValues("input");
		String outputDirPath = cmd.getOptionValue("output");
		Path resourceOutputDirPath = Path.of(cmd.getOptionValue("resource-output"));
		String[] javaLibraryPaths = cmd.getOptionValues("java-library");
		String[] packageMappingPairs = cmd.getOptionValues("package-mapping");

		List<String> librarySourceFiles = new ArrayList<>();
		Map<String, String> packageMapping = new HashMap<>();
		Map<String, String> currentPackageMapping = new HashMap<>();

		if(javaLibraryPaths != null) {
			for(var lib : javaLibraryPaths) {
				try(var analyzer = LibraryAnalyzer.fromPath(Path.of(lib))) {
					analyzer.scan();
					librarySourceFiles.addAll(analyzer.sourceFiles);
					packageMapping.putAll(analyzer.packageMapping);
				}
			}
		}


		List<String> inputFiles = new ArrayList<>();
		List<String> libraryPaths = new ArrayList<>();
		for(var dir : inputDirPaths) {
			var dirPath = Path.of(dir).toAbsolutePath();
			Files.walkFileTree(dirPath, Set.of(FileVisitOption.FOLLOW_LINKS), Integer.MAX_VALUE, new SimpleFileVisitor<Path>() {
				@Override
				public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
					if(Files.isSymbolicLink(dir)) {
						return FileVisitResult.SKIP_SUBTREE;
					}

					return FileVisitResult.CONTINUE;
				}

				@Override
				public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
					if(file.getFileName().toString().endsWith(".nidl")) {
						String content = Files.readString(file);
						inputFiles.add(content);

						var relPath = dirPath.relativize(file);
						var outFile = resourceOutputDirPath.resolve(relPath);
						var outDir = outFile.getParent();
						Files.createDirectories(outDir);
						Files.copy(file, outFile);
						libraryPaths.add(relPath.toString());
					}
					return FileVisitResult.CONTINUE;
				}
			});
		}

		for(var pm : packageMappingPairs) {
			String[] parts = pm.split("=", 2);
			if(parts.length != 2) {
				throw new RuntimeException("Invalid package mapping. Use idl=java");
			}

			var packageName = parts[0];
			var javaPackage = parts[1];
			packageMapping.put(packageName, javaPackage);
			currentPackageMapping.put(packageName, javaPackage);
		}

		compile(new JavaIDLCompilerOptions(
			new JavaLanguageOptions(
				outputDirPath,
				new PackageMapping(
					new KeywordMapping<>(packageMapping)
				)
			),
			inputFiles,
			librarySourceFiles
		));

		var jarOptions = new NobleIdlJarOptions(
			List.of("java"),
			libraryPaths,
			new KeywordMapping<>(currentPackageMapping)
		);

		Files.createDirectories(resourceOutputDirPath);
		try(var os = Files.newOutputStream(resourceOutputDirPath.resolve("nobleidl-options.esxb"))) {
			ESExprBinaryWriter.writeWithSymbolTable(os, NobleIdlJarOptions.codec().encode(jarOptions));
		}
	}

}

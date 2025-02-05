package dev.argon.nobleidl.compiler;

import dev.argon.esexpr.KeywordMapping;
import dev.argon.nobleidl.compiler.api.*;

import java.io.File;
import java.io.IOException;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.*;

import org.apache.commons.cli.*;
import org.jetbrains.annotations.NotNull;

import javax.annotation.processing.Processor;
import javax.tools.*;

public class JavaNobleIDLCompiler {
	private JavaNobleIDLCompiler() {}

	public static @NotNull NobleIdlGenerationResult compile(@NotNull JavaIDLCompilerOptions options) throws NobleIDLCompileErrorException, IOException {
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

		var handler = new Backend.PathEmitHandler(options.outputDir());
		backend.emitWith(handler);

		return handler.result();
	}

	public record CompilerInput(
		@NotNull List<@NotNull Path> inputFiles,
		@NotNull AnnotationProcessorPass processorPass,
		@NotNull List<@NotNull Path> javaLibraries,
		@NotNull Path javaOutputDir,
		@NotNull Path resourceOutputDir
	) {}

	@FunctionalInterface
	public interface AnnotationProcessorPass {
		void run(@NotNull Processor processor) throws IOException;
	}

	public static @NotNull NobleIdlGenerationResult compile(@NotNull CompilerInput compilerInput) throws NobleIDLCompileErrorException, IOException {
		List<String> librarySourceFiles = new ArrayList<>();
		Map<String, String> packageMapping = new HashMap<>();

		for(var lib : compilerInput.javaLibraries()) {
			try(var analyzer = LibraryAnalyzer.fromPath(lib)) {
				analyzer.scan();
				librarySourceFiles.addAll(analyzer.sourceFiles);
				packageMapping.putAll(analyzer.packageMapping);
			}
		}

		var libCopyDir = compilerInput.resourceOutputDir().resolve("nobleidl");

		List<String> inputFiles = new ArrayList<>();
		List<String> embeddedFilePaths = new ArrayList<>();
		for(var f : compilerInput.inputFiles()) {
			String content = Files.readString(f);
			inputFiles.add(content);

			int i = 1;
			String fileName;
			String embeddedFilePath;
			while(true) {
				if(i > 1) {
					String originalFileNameNoExt = f.getFileName().toString();
					if(originalFileNameNoExt.endsWith(".nidl")) {
						originalFileNameNoExt = originalFileNameNoExt.substring(0, originalFileNameNoExt.length() - 5);
					}

					fileName = originalFileNameNoExt + "." + i + ".nidl";
				}
				else {
					fileName = f.getFileName().toString();
				}

				embeddedFilePath = "nobleidl/" + fileName;

				if(embeddedFilePaths.contains(embeddedFilePath)) {
					++i;
					continue;
				}

				break;
			}

			embeddedFilePaths.add(embeddedFilePath);
			Files.createDirectories(libCopyDir);
			Files.copy(f, libCopyDir.resolve(fileName));
		}

		var annProcessor = new PackageMappingScannerProcessor();
		compilerInput.processorPass().run(annProcessor);

		packageMapping.putAll(annProcessor.getPackageMapping().mapping().map());

		return compile(
			new JavaIDLCompilerOptions(
				compilerInput.javaOutputDir(),
				new JavaLanguageOptions(
					new PackageMapping(
						new KeywordMapping<>(packageMapping)
					)
				),
				inputFiles,
				librarySourceFiles
			)
		);
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

		Option packageMappingOpt = new Option("j", "java-source", true, "Java source directory");
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

		String[] inputDirOptions = cmd.getOptionValues("input");
		String outputDirOption = cmd.getOptionValue("output");
		String resourceOutputDirOption = cmd.getOptionValue("resource-output");
		String[] javaLibraryOptions = cmd.getOptionValues("java-library");
		String[] javaSourceDirOptions = cmd.getOptionValues("java-source");

		if(inputDirOptions == null) {
			inputDirOptions = new String[] {};
		}


		List<Path> inputFilePaths = new ArrayList<>();
		for(var dir : inputDirOptions) {
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
						inputFilePaths.add(file);
					}
					return FileVisitResult.CONTINUE;
				}
			});
		}

		AnnotationProcessorPass processorPass = processor -> {
			JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();

			DiagnosticCollector<JavaFileObject> diagnostics = new DiagnosticCollector<>();

			try(StandardJavaFileManager fileManager = compiler.getStandardFileManager(diagnostics, null, StandardCharsets.UTF_8)) {
				List<Path> sourceFiles = new ArrayList<>();
				if(javaSourceDirOptions != null) {
					for(var dir : javaSourceDirOptions) {
						try(var files = Files.walk(Path.of(dir))) {
							for(Iterator<Path> it = files.iterator(); it.hasNext(); ) {
								var p = it.next();

								if(!Files.isRegularFile(p)) {
									continue;
								}

								var fileName = p.getFileName();
								if(fileName == null || !fileName.toString().endsWith(".java")) {
									continue;
								}

								sourceFiles.add(p);
							}
						}
					}
				}

				var compilationUnits = fileManager.getJavaFileObjects(sourceFiles.toArray(new Path[0]));

				var modulePath = String.join(File.pathSeparator, javaLibraryOptions);

				JavaCompiler.CompilationTask task = compiler.getTask(
					Writer.nullWriter(),
					fileManager,
					diagnostics,
					List.of(
						"-proc:only",
						"--module-path",
						modulePath
					),
					null,
					compilationUnits
				);

				task.setProcessors(List.of(processor));

				if(!task.call()) {
					var errors = String.join(System.lineSeparator(), diagnostics.getDiagnostics().stream().map(diag -> diag.getMessage(null)).toList());

					throw new RuntimeException("Annotation processing failure" + System.lineSeparator() + errors);
				}
			}
		};

		List<Path> javaLibraryPaths = new ArrayList<>();
		for(var lib : javaLibraryOptions) {
			javaLibraryPaths.add(Path.of(lib));
		}

		compile(new CompilerInput(
			inputFilePaths,
			processorPass,
			javaLibraryPaths,
			Path.of(outputDirOption),
			Path.of(resourceOutputDirOption)
		));
	}

}

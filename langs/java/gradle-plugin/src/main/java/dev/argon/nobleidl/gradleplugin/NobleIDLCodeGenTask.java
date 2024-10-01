package dev.argon.nobleidl.gradleplugin;

import dev.argon.nobleidl.compiler.JavaNobleIDLCompiler;
import dev.argon.nobleidl.compiler.NobleIDLCompileErrorException;
import org.gradle.api.DefaultTask;
import org.gradle.api.artifacts.Configuration;
import org.gradle.api.file.ConfigurableFileCollection;
import org.gradle.api.file.DirectoryProperty;
import org.gradle.api.file.SourceDirectorySet;
import org.gradle.api.tasks.*;
import org.jetbrains.annotations.NotNull;

import javax.annotation.processing.Processor;
import javax.tools.*;
import java.io.File;
import java.io.IOException;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class NobleIDLCodeGenTask extends DefaultTask {

	private final ConfigurableFileCollection inputFiles = getProject().getObjects().fileCollection();
	private final DirectoryProperty javaOutputDir = getProject().getObjects().directoryProperty();
	private final DirectoryProperty resourceOutputDir = getProject().getObjects().directoryProperty();
	private final Configuration classpath = getProject().getConfigurations().getByName("compileClasspath");

	@InputFiles
	public ConfigurableFileCollection getInputFiles() {
		return inputFiles;
	}

	@OutputDirectory
	public DirectoryProperty getJavaOutputDir() {
		return javaOutputDir;
	}


	@OutputDirectory
	public DirectoryProperty getResourceOutputDir() {
		return resourceOutputDir;
	}

	@Classpath
	public Configuration getClasspath() {
		return classpath;
	}

	@TaskAction
	public void generateCode() throws IOException, NobleIDLCompileErrorException {
		var javaOutputDir = getJavaOutputDir().get().getAsFile().toPath();
		var resourceOutputDir = getResourceOutputDir().get().getAsFile().toPath();

		getProject().delete(javaOutputDir);
		getProject().delete(resourceOutputDir);

		JavaNobleIDLCompiler.compile(new JavaNobleIDLCompiler.CompilerInput(
			getInputFiles().getFiles().stream().map(File::toPath).toList(),
			this::runProcessor,
			getClasspath().getFiles().stream().map(File::toPath).toList(),
			javaOutputDir,
			resourceOutputDir
		));
	}


	private void runProcessor(@NotNull Processor processor) throws IOException {

		JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();

		DiagnosticCollector<JavaFileObject> diagnostics = new DiagnosticCollector<>();

		try(StandardJavaFileManager fileManager = compiler.getStandardFileManager(diagnostics, null, StandardCharsets.UTF_8)) {

			List<File> javaSourceFiles = new ArrayList<>();

			var sourceSets = (SourceSetContainer) getProject().getExtensions().getByName("sourceSets");
			sourceSets.named("main", sourceSet -> {
				var outDir = getJavaOutputDir().get().getAsFile().toPath();

				sourceSet.getJava().getFiles()
					.stream()
					.filter(p -> !p.toPath().startsWith(outDir))
					.forEach(javaSourceFiles::add);
			});

			var compilationUnits = fileManager.getJavaFileObjects(javaSourceFiles.toArray(File[]::new));

			JavaCompiler.CompilationTask task = compiler.getTask(
				Writer.nullWriter(),
				fileManager,
				diagnostics,
				List.of(
					"-proc:only",
					"--module-path",
					getClasspath().getAsPath()
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


	}
}


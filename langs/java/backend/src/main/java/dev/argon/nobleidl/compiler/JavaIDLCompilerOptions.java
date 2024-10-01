package dev.argon.nobleidl.compiler;

import java.nio.file.Path;
import java.util.List;

public record JavaIDLCompilerOptions(
	Path outputDir,
	JavaLanguageOptions languageOptions,
	List<String> inputFileData,
	List<String> libraryFileData
) {}

package dev.argon.nobleidl.compiler;

import dev.argon.esexpr.*;
import java.util.List;

@ESExprCodecGen
public record JavaIDLCompilerOptions(
	@Keyword
	JavaLanguageOptions languageOptions,

	@Keyword
	List<String> inputFileData,

	@Keyword
	List<String> libraryFileData
) {}

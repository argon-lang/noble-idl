package dev.argon.nobleidl.compiler.api;

import dev.argon.esexpr.ESExprCodecGen;
import dev.argon.esexpr.Keyword;

import java.util.List;

@ESExprCodecGen
public record NobleIdlGenerationResult(
	@Keyword
	List<String> generatedFiles
) {}

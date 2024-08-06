package dev.argon.nobleidl.compiler.api;

import dev.argon.esexpr.Keyword;

public record NobleIdlGenerationRequest<L>(
	@Keyword
	L languageOptions,

	@Keyword
	NobleIdlModel model
) {}

package dev.argon.nobleidl.util;

import dev.argon.esexpr.KeywordMapping;
import dev.argon.nobleidl.compiler.JavaIDLCompilerOptions;
import dev.argon.nobleidl.compiler.JavaLanguageOptions;
import dev.argon.nobleidl.compiler.JavaNobleIDLCompiler;
import dev.argon.nobleidl.compiler.format.PackageMapping;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

public class RegenerateApi {

	public static void main(String[] args) throws Throwable {
		String coreLib = Files.readString(Path.of("../../noble-idl/runtime/nobleidl-core.nidl"));
		String compilerApi = Files.readString(Path.of("../../noble-idl/backend/compiler-api.nidl"));
		String jarMetadata = Files.readString(Path.of("../../noble-idl/backend/jar-metadata.nidl"));
		String javaAnns = Files.readString(Path.of("../../noble-idl/backend/compiler-api-java-annotations.nidl"));

		JavaNobleIDLCompiler.compile(new JavaIDLCompilerOptions(
			new JavaLanguageOptions(
				"../runtime/src/gen/java",

				new PackageMapping(
					new KeywordMapping<>(Map.of(
						"nobleidl.core", "dev.argon.nobleidl.runtime"
					))
				)
			),
			List.of(coreLib),
			List.of()
		));

		JavaNobleIDLCompiler.compile(new JavaIDLCompilerOptions(
			new JavaLanguageOptions(
				"../backend/src/gen/java",

				new PackageMapping(
					new KeywordMapping<>(Map.of(
						"nobleidl.core", "dev.argon.nobleidl.runtime",
						"nobleidl.compiler.api", "dev.argon.nobleidl.compiler.api",
						"nobleidl.compiler.api.java", "dev.argon.nobleidl.compiler.api.java",
						"nobleidl.compiler.jar-metadata", "dev.argon.nobleidl.compiler.format"
					))
				)
			),
			List.of(compilerApi, jarMetadata, javaAnns),
			List.of(coreLib)
		));
	}

}

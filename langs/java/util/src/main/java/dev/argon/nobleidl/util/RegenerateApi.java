package dev.argon.nobleidl.util;

import dev.argon.esexpr.KeywordMapping;
import dev.argon.nobleidl.compiler.JavaIDLCompilerOptions;
import dev.argon.nobleidl.compiler.JavaLanguageOptions;
import dev.argon.nobleidl.compiler.JavaNobleIDLCompiler;
import dev.argon.nobleidl.compiler.PackageMapping;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

public class RegenerateApi {

	public static void main(String[] args) throws Throwable {
		String coreLib = Files.readString(Path.of("../../noble-idl/runtime/nobleidl-core.nidl"));
		String compilerApi = Files.readString(Path.of("../../noble-idl/backend/compiler-api.nidl"));
		String javaAnns = Files.readString(Path.of("../../noble-idl/backend/compiler-api-java-annotations.nidl"));

		JavaNobleIDLCompiler.compile(
			new JavaIDLCompilerOptions(
				Path.of("../runtime/src/gen/java"),
				new JavaLanguageOptions(
					new PackageMapping(
						new KeywordMapping<>(Map.of(
							"nobleidl.core", "dev.argon.nobleidl.runtime"
						))
					)
				),
				List.of(coreLib),
				List.of()
			)
		);

		JavaNobleIDLCompiler.compile(
			new JavaIDLCompilerOptions(
				Path.of("../backend/src/gen/java"),
				new JavaLanguageOptions(
					new PackageMapping(
						new KeywordMapping<>(Map.of(
							"nobleidl.core", "dev.argon.nobleidl.runtime",
							"nobleidl.compiler.api", "dev.argon.nobleidl.compiler.api",
							"nobleidl.compiler.api.java", "dev.argon.nobleidl.compiler.api.java"
						))
					)
				),
				List.of(compilerApi, javaAnns),
				List.of(coreLib)
			)
		);
	}

}

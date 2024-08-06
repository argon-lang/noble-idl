package dev.argon.nobleidl.compiler;

import dev.argon.esexpr.KeywordMapping;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

public class RegenerateApi {

	public static void main(String[] args) throws Throwable {
		String coreLib = Files.readString(Path.of("../../noble-idl/runtime/nobleidl-core.nidl"));
		String compilerApi = Files.readString(Path.of("../../noble-idl/backend/compiler-api.nidl"));

		JavaNobleIDLCompiler.compile(new JavaIDLCompilerOptions(
			new JavaLanguageOptions(
				"src/gen/java",

				new PackageMapping(
					new KeywordMapping<>(Map.of(
						"nobleidl.core", "dev.argon.nobleidl.runtime",
						"nobleidl.compiler.api", "dev.argon.nobleidl.compiler.api"
					))
				)
			),
			List.of(compilerApi),
			List.of(coreLib)
		));
	}

}

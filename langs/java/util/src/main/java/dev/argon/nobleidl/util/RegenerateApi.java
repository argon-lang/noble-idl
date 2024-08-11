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

		JavaNobleIDLCompiler.compile(new JavaIDLCompilerOptions(
			new JavaLanguageOptions(
				"../backend/src/gen/java",

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

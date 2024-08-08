package dev.argon.nobleidl.util;

import dev.argon.esexpr.ESExprBinaryWriter;
import dev.argon.esexpr.KeywordMapping;
import dev.argon.nobleidl.compiler.format.NobleIdlJarOptions;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

public class CreateNobleIDLRuntimeOptions {

	public static void main(String[] args) throws Throwable {
		var options = new NobleIdlJarOptions(
			List.of("java"),
			List.of("dev/argon/nobleidl/runtime/nobleidl-core.nidl"),
			new KeywordMapping<>(Map.of(
				"nobleidl.core", "dev.argon.nobleidl.runtime"
			))
		);

		try(var os = Files.newOutputStream(Path.of("../runtime/src/main/resources/noble-idl-options.esxb"))) {
			ESExprBinaryWriter.writeWithSymbolTable(os, NobleIdlJarOptions.codec().encode(options));
		}
	}

}

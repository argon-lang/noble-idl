package dev.argon.nobleidl.compiler;

import dev.argon.nobleidl.compiler.api.NobleIdlCompileModelOptions;
import dev.argon.nobleidl.compiler.api.NobleIdlCompileModelResult;
import dev.argon.nobleidl.compiler.api.NobleIdlGenerationRequest;
import dev.argon.nobleidl.compiler.api.NobleIdlGenerationResult;

import java.io.IOException;

public class JavaNobleIDLCompiler {
	private JavaNobleIDLCompiler() {}

	public static NobleIdlGenerationResult compile(JavaIDLCompilerOptions options) throws NobleIDLCompileErrorException, IOException {
		var modelOptions = new NobleIdlCompileModelOptions(
			options.libraryFileData(),
			options.inputFileData()
		);

		NobleIdlCompileModelResult result;
		try(var compiler = new NobleIDLCompiler()) {
			result = compiler.loadModel(modelOptions);
		}

		var model = switch(result) {
			case NobleIdlCompileModelResult.Success success -> success.model();
			case NobleIdlCompileModelResult.Failure(var errors) -> {
				throw new NobleIDLCompileErrorException(String.join("\n", errors));
			}
		};

		var backend = new JavaBackend(new NobleIdlGenerationRequest<>(options.languageOptions(), model));

		backend.emit();

		return backend.result();
	}

}

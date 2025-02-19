package dev.argon.nobleidl.runtime.graaljsInterop;

import org.graalvm.polyglot.Context;
import org.graalvm.polyglot.Value;

public class ErrorChecker {
	private ErrorChecker() {}

	public static Value forErrorName(Context context, String name) {
		return context.eval("js",
			"""
				name => ({
					isInstance(o) {
						return o[Symbol.for("nobleidl-error-type")] === name;
					},
				})
			"""
		).execute(name);
	}
}

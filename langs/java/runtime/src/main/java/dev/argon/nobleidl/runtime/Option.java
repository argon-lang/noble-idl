package dev.argon.nobleidl.runtime;

import dev.argon.nobleidl.runtime.graaljsInterop.JSAdapter;
import dev.argon.nobleidl.runtime.graaljsInterop.JSExecutor;
import org.graalvm.polyglot.Context;
import org.graalvm.polyglot.Value;

import java.util.Optional;

public final class Option {
	private Option() {}

	public static <A> Optional<A> buildFrom(A value) {
		return Optional.of(value);
	}

	public static <A> Optional<A> fromNull() {
		return Optional.empty();
	}

	public static <A> JSAdapter<Optional<A>> jsAdapter(JSAdapter<A> aAdapter) {
		return new JSAdapter<>() {
			@Override
			public Optional<A> fromJS(Context context, JSExecutor executor, Value value) {
				if(context.eval("js", "x => x === null").execute(value).asBoolean()) {
					return Optional.empty();
				}

				Value adjustedValue = context.eval("js",
					"""
					value => {
						const level = Symbol.for("esexpr-wrapped-null-level");
						if(typeof value === "object" && level in value) {
							if(value[level] > 1) {
								return { [level]: value[level] - 1 };
							}
							else {
								return null;
							}
						}
						else {
							return value;
						}
					}
					"""
				).execute(value);

				return Optional.of(aAdapter.fromJS(context, executor, adjustedValue));
			}

			@Override
			public Value toJS(Context context, JSExecutor executor, Optional<A> value) {
				if(value.isEmpty()) {
					return context.asValue(null);
				}

				A elem = value.get();
				Value jsValue = aAdapter.toJS(context, executor, elem);

				return context.eval("js",
					"""
						value => {
							const level = Symbol.for("esexpr-wrapped-null-level");
							if(value === null) {
								return { [level]: 1 };
							}
							else if(typeof value === "object" && level in value) {
								return { [level]: value[level] + 1 };
							}
							else {
								return value;
							}
						}
						"""
				).execute(jsValue);
			}
		};
	}
}

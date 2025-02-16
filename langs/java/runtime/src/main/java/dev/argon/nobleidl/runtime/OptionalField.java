package dev.argon.nobleidl.runtime;

import dev.argon.nobleidl.runtime.graaljsInterop.JSAdapter;
import dev.argon.nobleidl.runtime.graaljsInterop.JSExecutor;
import org.graalvm.polyglot.Context;
import org.graalvm.polyglot.Value;

import java.util.Optional;

public final class OptionalField {
	private OptionalField() {}

	public static <A> Optional<A> fromElement(A value) {
		return Optional.of(value);
	}

	public static <A> Optional<A> empty() {
		return Optional.empty();
	}



	public static <A> JSAdapter<Optional<A>> jsAdapter(JSAdapter<A> aAdapter) {
		return new JSAdapter<>() {
			@Override
			public Optional<A> fromJS(Context context, JSExecutor executor, Value value) {
				if(value == null || context.eval("js", "x => x === void 0").execute(value).asBoolean()) {
					return Optional.empty();
				}

				return Optional.of(aAdapter.fromJS(context, executor, value));
			}

			@Override
			public Value toJS(Context context, JSExecutor executor, Optional<A> value) {
				if(value.isEmpty()) {
					return context.eval("js", "void 0");
				}

				A elem = value.get();
				return aAdapter.toJS(context, executor, elem);
			}
		};
	}
}

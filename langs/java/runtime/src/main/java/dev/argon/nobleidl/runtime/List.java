package dev.argon.nobleidl.runtime;

import dev.argon.nobleidl.runtime.graaljsInterop.JSAdapter;
import dev.argon.nobleidl.runtime.graaljsInterop.JSExecutor;
import org.graalvm.polyglot.Context;
import org.graalvm.polyglot.Value;

import java.util.ArrayList;

public final class List {
	private List() {}

	@SafeVarargs
	public static <A> java.util.List<A> fromValues(A... values) {
		return java.util.List.of(values);
	}

	public static <A> java.util.List<A> buildFrom(ListRepr<A> repr) {
		return repr.values();
	}

	public static <A> JSAdapter<java.util.List<A>> jsAdapter(JSAdapter<A> aAdapter) {
		return new JSAdapter<>() {
			@Override
			public java.util.List<A> fromJS(Context context, JSExecutor executor, Value value) {
				java.util.List<A> l = new ArrayList<>();

				Value iter = value.getIterator();
				while(iter.hasIteratorNextElement()) {
					Value jsValue = iter.getIteratorNextElement();
					A convertedValue = aAdapter.fromJS(context, executor, jsValue);
					l.add(convertedValue);
				}

				return l;
			}

			@Override
			public Value toJS(Context context, JSExecutor executor, java.util.List<A> value) {
				return context.eval("js", "Array.from").execute(value);
			}
		};
	}
}

package dev.argon.nobleidl.runtime;

import dev.argon.nobleidl.runtime.graaljsInterop.JSAdapter;
import dev.argon.nobleidl.runtime.graaljsInterop.JSExecutor;
import org.graalvm.polyglot.Context;
import org.graalvm.polyglot.Value;

public final class Unit {
	private Unit() {}

	public static Unit INSTANCE = new Unit();

	public static JSAdapter<Unit> jsAdapter() {
		return new JSAdapter<>() {
			@Override
			public Unit fromJS(Context context, JSExecutor executor, Value value) {
				return Unit.INSTANCE;
			}

			@Override
			public Value toJS(Context context, JSExecutor executor, Unit value) {
				return context.eval("js", "void 0");
			}
		};
	}
}

package dev.argon.nobleidl.example;

import dev.argon.nobleidl.runtime.graaljsInterop.JSAdapter;
import dev.argon.nobleidl.runtime.graaljsInterop.JSExecutor;

public record MyExtern() {
	public static JSAdapter<MyExtern> jsAdapter() {
		return new JSAdapter<>() {
			@Override
			public MyExtern fromJS(org.graalvm.polyglot.Context context, JSExecutor executor, org.graalvm.polyglot.Value value) {
				return new MyExtern();
			}

			@Override
			public org.graalvm.polyglot.Value toJS(org.graalvm.polyglot.Context context, JSExecutor executor, MyExtern value) {
				return context.eval("js", "({})");
			}
		};
	}
}

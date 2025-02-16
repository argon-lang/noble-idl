package dev.argon.nobleidl.compiler.graaljsInterop;

import org.graalvm.polyglot.Context;
import org.graalvm.polyglot.Value;

public interface JSConverter<T> {
	T fromJS();
	Value toJS(Context context);
}

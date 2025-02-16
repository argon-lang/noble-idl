package dev.argon.nobleidl.runtime.graaljsInterop;

import org.graalvm.polyglot.Value;

public interface WrappedJavaScriptObject {
	Value _getAsJSValue();
}

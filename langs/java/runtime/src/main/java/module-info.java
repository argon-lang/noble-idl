module dev.argon.nobleidl.runtime {
	requires static org.jetbrains.annotations;
	requires dev.argon.esexpr;
	requires static org.graalvm.polyglot;

	exports dev.argon.nobleidl.runtime;
	exports dev.argon.nobleidl.runtime.graaljsInterop;
}

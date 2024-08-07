module dev.argon.nobleidl.compiler {
	requires dev.argon.esexpr;
	requires dev.argon.nobleidl.runtime;
	requires dev.argon.jvmwasm.engine;
	requires org.apache.commons.text;
	requires java.compiler;

	exports dev.argon.nobleidl.compiler;
	exports dev.argon.nobleidl.compiler.api;
}
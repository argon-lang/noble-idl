module dev.argon.nobleidl.compiler {
	requires static org.jetbrains.annotations;
	requires dev.argon.esexpr;
	requires dev.argon.nobleidl.runtime;
	requires dev.argon.jawawasm.engine;
	requires org.apache.commons.text;
	requires org.apache.commons.cli;
	requires java.compiler;
	requires org.objectweb.asm;

	exports dev.argon.nobleidl.compiler;
	exports dev.argon.nobleidl.compiler.api;
}
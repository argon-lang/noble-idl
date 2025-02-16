module dev.argon.nobleidl.test {
	requires static org.jetbrains.annotations;
	requires dev.argon.nobleidl.runtime;
	requires dev.argon.esexpr;
	requires org.graalvm.polyglot;

	exports dev.argon.nobleidl.example;
}
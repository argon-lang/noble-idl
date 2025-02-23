module dev.argon.nobleidl.test.tests {
	requires dev.argon.nobleidl.runtime;
	requires dev.argon.nobleidl.test;
	requires org.junit.jupiter.api;
	requires dev.argon.esexpr;
	requires org.graalvm.polyglot;
	requires org.jetbrains.annotations;
	requires org.easymock;
	exports dev.argon.nobleidl.test.tests;

	opens dev.argon.nobleidl.test.tests to org.junit.platform.commons;
}
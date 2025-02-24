package dev.argon.nobleidl.test.tests;

import dev.argon.nobleidl.example.DoSomething;

import org.graalvm.polyglot.PolyglotException;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import static org.easymock.EasyMock.*;

import java.math.BigInteger;

public class JSAdapterInterfaceTests extends JSAdapterTestBase {

	@Test
	void doSomethingToJS() throws InterruptedException {
		DoSomething javaValue = mock(DoSomething.class);

		expect(javaValue.add(BigInteger.TEN, BigInteger.TWO)).andReturn(BigInteger.valueOf(15));
		replay(javaValue);

		var jsValue = runJsThread(() -> DoSomething.jsAdapter().toJS(context, executor, javaValue));

		var ten = bigint(10);
		var two = bigint(2);

		var result = runAwait(() -> jsValue.invokeMember("add", ten, two));

		assertEquals(
			BigInteger.valueOf(15),
			runJsThread(() -> result.asBigInteger())
		);

		verify(javaValue);
	}

	@Test
	void doSomethingFromJS() throws InterruptedException {
		var jsValue = js(
			"""
				({
					async toStr(value) { return "A"; },
					async run(n) { },
					async add(a, b) { return 15; },
				})
				"""
		);

		var ten = bigint(10);
		var two = bigint(2);

		var javaValue = runJsThread(() -> DoSomething.jsAdapter().fromJS(context, executor, jsValue));

		assertEquals(
			BigInteger.valueOf(15),
			javaValue.add(BigInteger.TEN, BigInteger.TWO)
		);
	}

	@Test
	void throwFromJS() throws InterruptedException {
		var jsValue = js(
			"""
				({
					async toStr(value) { return "A"; },
					async run(n) { throw new Error("Test Error"); },
					async add(a, b) { return 15; },
				})
				"""
		);

		var javaValue = runJsThread(() -> DoSomething.jsAdapter().fromJS(context, executor, jsValue));

		assertThrows(PolyglotException.class, () -> javaValue.run(BigInteger.TEN));
	}

}

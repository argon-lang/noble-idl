package dev.argon.nobleidl.test.tests;

import dev.argon.nobleidl.example.*;
import org.junit.jupiter.api.Test;

import java.math.BigInteger;

public class JSAdapterRecordTests extends JSAdapterTestBase {

	@Test
	void stringIntPair() {
		var adapter = StringIntPair.jsAdapter();

		assertJsAdapted(
			adapter,
			new StringIntPair("ABC", BigInteger.valueOf(456)),
			"{ s: 'ABC', i: 456n }"
		);
	}

}

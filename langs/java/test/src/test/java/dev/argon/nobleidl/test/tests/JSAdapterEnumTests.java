package dev.argon.nobleidl.test.tests;

import dev.argon.nobleidl.example.StringIntPair;
import dev.argon.nobleidl.example.StringOrInt;
import org.junit.jupiter.api.Test;

import java.math.BigInteger;

public class JSAdapterEnumTests extends JSAdapterTestBase {

	@Test
	void stringOrIntInt() {
		var adapter = StringOrInt.jsAdapter();

		assertJsAdapted(
			StringOrInt.jsAdapter(),
			new StringOrInt.A("ABC"),
			"{ $type: 'a', s: 'ABC' }"
		);

		assertJsAdapted(
			adapter,
			new StringOrInt.B(BigInteger.valueOf(456)),
			"{ $type: 'b', i: 456n }"
		);
	}

}

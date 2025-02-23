package dev.argon.nobleidl.test.tests;

import dev.argon.nobleidl.example.MySimpleEnum;
import dev.argon.nobleidl.example.StringOrInt;
import org.junit.jupiter.api.Test;

import java.math.BigInteger;

public class JSAdapterSimpleEnumTests extends JSAdapterTestBase {

	@Test
	void stringOrIntInt() {
		var adapter = MySimpleEnum.jsAdapter();

		assertJsAdapted(
			adapter,
			MySimpleEnum.A,
			"'a'"
		);

		assertJsAdapted(
			adapter,
			MySimpleEnum.B,
			"'b'"
		);
	}

}

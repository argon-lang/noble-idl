package dev.argon.nobleidl.runtime;

import dev.argon.esexpr.KeywordMapping;

import java.lang.String;
import java.util.Map;

public class Dict {
	private Dict() {}

	public static <A> dev.argon.esexpr.KeywordMapping<A> fromMap(Map<String, A> map) {
		return new KeywordMapping<>(map);
	}

	public static <A> dev.argon.esexpr.KeywordMapping<A> buildFrom(DictRepr<A> repr) {
		return repr.values();
	}
}

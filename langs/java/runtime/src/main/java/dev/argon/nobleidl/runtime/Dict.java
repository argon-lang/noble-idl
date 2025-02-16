package dev.argon.nobleidl.runtime;

import dev.argon.esexpr.KeywordMapping;
import dev.argon.nobleidl.runtime.graaljsInterop.JSAdapter;
import dev.argon.nobleidl.runtime.graaljsInterop.JSExecutor;
import org.graalvm.polyglot.Context;
import org.graalvm.polyglot.Value;

import java.lang.String;
import java.util.HashMap;
import java.util.Map;

public final class Dict {
	private Dict() {}

	public static <A> KeywordMapping<A> fromMap(Map<String, A> map) {
		return new KeywordMapping<>(map);
	}

	public static <A> KeywordMapping<A> buildFrom(DictRepr<A> repr) {
		return repr.values();
	}

	public static <A> JSAdapter<KeywordMapping<A>> jsAdapter(JSAdapter<A> aAdapter) {
		return new JSAdapter<>() {
			@Override
			public KeywordMapping<A> fromJS(Context context, JSExecutor executor, Value value) {
				Map<String, A> result = new HashMap<>();

				Value iter = value.getIterator();
				while(iter.hasIteratorNextElement()) {
					Value kv = iter.getIteratorNextElement();
					String keyStr = kv.getArrayElement(0).asString();
					Value jsValue = kv.getArrayElement(1);
					A convertedValue = aAdapter.fromJS(context, executor, jsValue);
					result.put(keyStr, convertedValue);
				}

				return new KeywordMapping<>(result);
			}

			@Override
			public Value toJS(Context context, JSExecutor executor, KeywordMapping<A> value) {
				Value jsMap = context.eval("js", "new Map()");

				for (Map.Entry<String, A> entry : value.map().entrySet()) {
					String key = entry.getKey();
					A javaValue = entry.getValue();
					Value jsValue = aAdapter.toJS(context, executor, javaValue);
					jsMap.invokeMember("set", key, jsValue);
				}

				return jsMap;
			}
		};
	}
}

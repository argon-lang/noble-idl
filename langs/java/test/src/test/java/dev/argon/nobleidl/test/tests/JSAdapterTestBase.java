package dev.argon.nobleidl.test.tests;

import dev.argon.nobleidl.runtime.graaljsInterop.JSAdapter;
import dev.argon.nobleidl.runtime.graaljsInterop.JSExecutor;
import dev.argon.nobleidl.runtime.graaljsInterop.JSPromiseAdapter;
import org.graalvm.polyglot.Context;
import org.graalvm.polyglot.Value;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import static org.junit.jupiter.api.Assertions.*;

import java.util.Arrays;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class JSAdapterTestBase {

	protected Context context;
	protected JSExecutor executor;
	protected ExecutorService jsExecutor;
	protected ExecutorService javaExecutor;

	@BeforeEach
	void setUp() {
		context = Context.newBuilder()
			.option("js.esm-eval-returns-exports", "true")
			.option("engine.WarnInterpreterOnly", "false")
			.build();

		jsExecutor = Executors.newSingleThreadExecutor();
		javaExecutor = Executors.newVirtualThreadPerTaskExecutor();

		executor = JSExecutor.fromExecutors(jsExecutor, javaExecutor);
	}

	@AfterEach
	void tearDown() {
		if(context != null) {
			context.close();
			context = null;
		}

		executor = null;

		if(jsExecutor != null) {
			jsExecutor.close();
			jsExecutor = null;
		}

		if(javaExecutor != null) {
			javaExecutor.close();
			javaExecutor = null;
		}
	}

	public Value js(String code) {
		return runJsThread(() -> context.eval("js", code));
	}

	public <A> A runJsThread(JSExecutor.CallbackWithoutError<A> callback) {
		try {
			return executor.runOnJSThreadWithoutError(callback).get();
		} catch(InterruptedException e) {
			throw new RuntimeException(e);
		}
	}

	public Value runAwait(JSExecutor.CallbackWithoutError<Value> callback) {
		return await(runJsThread(callback));
	}

	public Value await(Value promise) {
		try {
			return runJsThread(() -> new JSPromiseAdapter<>(JSAdapter.VALUE_ADAPTER).fromJS(context, executor, promise)).get();
		} catch(InterruptedException e) {
			throw new RuntimeException(e);
		}
	}

	public Value bigint(int n) {
		return js(n + "n");
	}

	public void assertJsEquals(Value expected, Value actual) {
		try {
			executor.runOnJSThreadWithoutError(() -> {
				Assertions.assertTrue(
					deepEquals(expected, actual),
					() -> "Expected: " + expected.toString() + " but got: " + actual.toString()
				);
				return null;
			}).get();
		} catch(InterruptedException e) {
			throw new RuntimeException(e);
		}
	}

	public <T> void assertJsAdapted(JSAdapter<T> adapter, T value, String jsValue) {
		assertJsEquals(jsValue, adapter.toJS(context, executor, value));
		assertEquals(value, adapter.fromJS(context, executor, js("(" + jsValue + ")")));
	}

	public void assertJsEquals(String expected, Value actual) {
		Value expected2 = js("(" + expected + ")");
		assertJsEquals(expected2, actual);
	}

	private boolean deepEquals(Value v1, Value v2) {

		Value typeof = context.eval("js", "x => typeof x");
		String valueType = typeof.execute(v1).asString();

		if(!valueType.equals(typeof.execute(v2).asString())) {
			return false;
		}

		if(v1.isNull()) return true;

		return switch(valueType) {
			case "string" -> v1.asString().equals(v2.asString());
			case "number" -> v1.asDouble() == v2.asDouble();
			case "bigint" -> v1.asBigInteger().equals(v2.asBigInteger());
			case "boolean" -> v1.asBoolean() == v2.asBoolean();
			case "undefined" -> true;
			case "object" -> {
				if (isUint8Array(v1)) {
					if(!isUint8Array(v2)) {
						yield false;
					}

					byte[] arr1 = v1.as(byte[].class);
					byte[] arr2 = v2.as(byte[].class);
					yield Arrays.equals(arr1, arr2);
				}


				if (v1.hasArrayElements()) {
					if(!v2.hasArrayElements()) {
						yield false;
					}

					if(v1.getArraySize() != v2.getArraySize()) yield false;
					for(long i = 0; i < v1.getArraySize(); i++) {
						if (!deepEquals(v1.getArrayElement(i), v2.getArrayElement(i))) yield false;
					}

					yield true;
				}

				if (v1.hasMembers() && v2.hasMembers()) {
					for (String key : v1.getMemberKeys()) {
						if (!v2.hasMember(key)) yield false;
						if (!deepEquals(v1.getMember(key), v2.getMember(key))) yield false;
					}
					yield true;
				}

				yield false;
			}
			default -> false;
		};
	}

	private boolean isUint8Array(Value value) {
		return value.hasMembers() && value.hasArrayElements() && "Uint8Array".equals(value.getMetaObject().toString());
	}
}

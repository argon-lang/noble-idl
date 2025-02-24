package dev.argon.nobleidl.runtime;

import dev.argon.esexpr.ESExpr;
import dev.argon.nobleidl.runtime.graaljsInterop.JSAdapter;
import dev.argon.nobleidl.runtime.graaljsInterop.JSExecutor;
import org.graalvm.polyglot.Context;
import org.graalvm.polyglot.TypeLiteral;
import org.graalvm.polyglot.Value;

import java.math.BigInteger;
import java.util.Map;
import java.util.stream.Collectors;

public class Esexpr {
	private Esexpr() {}

	public static JSAdapter<ESExpr> jsAdapter() {
		return new JSAdapter<>() {
			@Override
			public ESExpr fromJS(Context context, JSExecutor executor, Value value) {
				if(value.isString()) {
					return new ESExpr.Str(value.asString());
				}
				else if(value.isBoolean()) {
					return new ESExpr.Bool(value.asBoolean());
				}
				else if(value.isNumber()) {
					if(value.getMetaObject().toString().equals("bigint")) {
						return new ESExpr.Int(value.asBigInteger());
					}
					else {
						return new ESExpr.Float64(value.asDouble());
					}
				}
				else if(value.isNull()) {
					return new ESExpr.Null(BigInteger.ZERO);
				}
				else if(context.eval("js", "expr => expr instanceof Uint8Array").execute(value).asBoolean()) {
					return new ESExpr.Binary(Binary.jsAdapter().fromJS(context, executor, value));
				}
				else {
					return switch(value.getMember("type").asString()) {
						case "constructor" -> {
							var name = value.getMember("name").asString();
							var args = value.getMember("args").as(new TypeLiteral<java.util.List<Value>>() {})
								.stream()
								.map(arg -> fromJS(context, executor, arg))
								.toList();

							var kwargs = value.getMember("kwargs").as(new TypeLiteral<Map<java.lang.String, Value>>() {})
								.entrySet()
								.stream()
								.collect(Collectors.toMap(Map.Entry::getKey, e -> fromJS(context, executor, e.getValue())));

							yield new ESExpr.Constructor(name, args, kwargs);
						}
						case "null" -> new ESExpr.Null(value.getMember("level").asBigInteger());
						case "float32" -> new ESExpr.Float32(value.getMember("value").asFloat());
						default -> {
							throw new IllegalArgumentException("Invalid ESExpr value");
						}
					};
				}
			}

			@Override
			public Value toJS(Context context, JSExecutor executor, ESExpr value) {
				return switch(value) {
					case ESExpr.Constructor(var name, var args, var kwargs) -> {
						var args2 = context.eval("js", "[]");
						for(var arg : args) {
							args2.invokeMember("push", toJS(context, executor, arg));
						}

						var kwargs2 = context.eval("js", "new Map()");
						for(var entry : kwargs.entrySet()) {
							kwargs2.invokeMember("set", entry.getKey(), toJS(context, executor, entry.getValue()));
						}

						yield context.eval("js", "(name, args, kwargs) => ({ type: 'constructor', name, args: Array.from(args), kwargs: new Map(kwargs) })")
							.execute(name, args2, kwargs2);
					}

					case ESExpr.Bool(var b) -> context.asValue(b);
					case ESExpr.Int(var i) -> context.eval("js", "BigInt").execute(i);
					case ESExpr.Str(var s) -> context.asValue(s);
					case ESExpr.Binary(var b) -> Binary.jsAdapter().toJS(context, executor, b);
					case ESExpr.Float32(var f) ->
						context.eval("js", "value => ({ type: 'float32', value })").execute(f);
					case ESExpr.Float64(var f) -> context.asValue(f);
					case ESExpr.Null(var level) -> {
						if(level.signum() > 0) {
							yield context.eval("js", "level => ({ type: 'null', level: BigInt(level) })").execute(level);
						}
						else {
							yield context.asValue(null);
						}
					}
				};

			}
		};
	}
}

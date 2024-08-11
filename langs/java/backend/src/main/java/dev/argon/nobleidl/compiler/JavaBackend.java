package dev.argon.nobleidl.compiler;

import dev.argon.esexpr.DecodeException;
import dev.argon.nobleidl.compiler.api.*;
import dev.argon.nobleidl.compiler.api.java.JavaAnnExternType;
import dev.argon.nobleidl.compiler.api.java.JavaMappedType;
import org.apache.commons.text.StringEscapeUtils;

import java.io.IOException;
import java.io.StringWriter;
import java.io.Writer;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.stream.Collectors;
import javax.lang.model.SourceVersion;

class JavaBackend {
	public JavaBackend(NobleIdlGenerationRequest<JavaLanguageOptions> options) {
		this.languageOptions = options.languageOptions();
		this.model = options.model();
		this.packageMapping = getPackageMapping(languageOptions);
	}

	private final JavaLanguageOptions languageOptions;
	private final NobleIdlModel model;
	private final Map<PackageName, String> packageMapping;

	private List<String> outputFiles = new ArrayList<>();



	private static Map<PackageName, String> getPackageMapping(JavaLanguageOptions options) {
		Map<PackageName, String> packageMapping = new HashMap<>();
		for(var entry : options.packageMapping().mapping().map().entrySet()) {
			packageMapping.put(PackageNameUtil.fromString(entry.getKey()), entry.getValue());
		}
		return packageMapping;
	}


	public NobleIdlGenerationResult result() {
		return new NobleIdlGenerationResult(outputFiles);
	}

	public void emit() throws IOException, NobleIDLCompileErrorException {
		for(var dfn : model.definitions()) {
			if(dfn.isLibrary()) {
				continue;
			}

			emitDefinition(dfn);
		}
	}

	private void emitDefinition(DefinitionInfo dfn) throws IOException, NobleIDLCompileErrorException {
		switch(dfn.definition()) {
			case Definition.Record(var r) -> emitRecord(dfn, r);
			case Definition.Enum(var e) -> emitEnum(dfn, e);
			case Definition.SimpleEnum(var e) -> emitSimpleEnum(dfn, e);
			case Definition.ExternType(_) -> {}
			case Definition.Interface(var iface) -> emitInterface(dfn, iface);
		}
	}

	private CodeWriter openFile(DefinitionInfo dfn) throws IOException, NobleIDLCompileErrorException {
		var idlPackageName = dfn.name()._package();
		var javaPackageName = getJavaPackage(idlPackageName);

		var path = Path.of(languageOptions.outputDir());
		if(!javaPackageName.isEmpty()) {
			var parts = javaPackageName.split("\\.");
			for(String part : parts) {
				path = path.resolve(part);
			}
		}

		Files.createDirectories(path);

		path = path.resolve(convertIdPascal(dfn.name().name()) + ".java");

		return new CodeWriter(Files.newBufferedWriter(path, StandardCharsets.UTF_8));
	}

	private void emitRecord(DefinitionInfo dfn, RecordDefinition r) throws IOException, NobleIDLCompileErrorException {
		try(var w = openFile(dfn)) {
			w.print("package ");
			w.print(getJavaPackage(dfn.name()._package()));
			w.println(";");

			var esexprOptions = r.esexprOptions().orElse(null);
			if(esexprOptions != null) {
				w.println("@dev.argon.esexpr.ESExprCodecGen");
				w.print("@dev.argon.esexpr.Constructor(\"");
				w.print(StringEscapeUtils.escapeJava(esexprOptions.constructor()));
				w.println("\")");
			}

			w.print("public record ");
			w.print(convertIdPascal(dfn.name().name()));
			writeTypeParameters(w, dfn.typeParameters());
			writeRecordParameters(w, r.fields());
			w.println(" {");
			w.indent();

			if(r.esexprOptions().isPresent()) {
				writeCodecMethod(w, dfn);
			}

			w.dedent();
			w.println("}");
		}
	}

	private void emitEnum(DefinitionInfo dfn, EnumDefinition e) throws IOException, NobleIDLCompileErrorException {
		try(var w = openFile(dfn)) {
			w.print("package ");
			w.print(getJavaPackage(dfn.name()._package()));
			w.println(";");


			{
				var esexprOptions = e.esexprOptions().orElse(null);
				if(esexprOptions != null) {
					w.println("@dev.argon.esexpr.ESExprCodecGen");
				}
			}

			w.print("public sealed interface ");
			w.print(convertIdPascal(dfn.name().name()));
			writeTypeParameters(w, dfn.typeParameters());
			w.println(" {");
			w.indent();

			for(var c : e.cases()) {
				{
					var esexprOptions = c.esexprOptions().orElse(null);
					if(esexprOptions != null) {
						switch(esexprOptions.caseType()) {
							case EsexprEnumCaseType.Constructor(var constructor) -> {
								w.print("@dev.argon.esexpr.Constructor(\"");
								w.print(StringEscapeUtils.escapeJava(constructor));
								w.println("\")");
							}
							case EsexprEnumCaseType.InlineValue() ->
								w.println("@dev.argon.esexpr.InlineValue");
						}
					}
				}

				w.print("record ");
				w.print(convertIdPascal(c.name()));
				writeTypeParameters(w, dfn.typeParameters());
				writeRecordParameters(w, c.fields());
				w.print(" implements ");
				w.print(getJavaPackage(dfn.name()._package()));
				w.print(".");
				w.print(convertIdPascal(dfn.name().name()));
				writeTypeParametersAsArguments(w, dfn.typeParameters());
				w.println(" {}");
			}

			if(e.esexprOptions().isPresent()) {
				writeCodecMethod(w, dfn);
			}

			w.dedent();
			w.println("}");
		}
	}

	private void emitSimpleEnum(DefinitionInfo dfn, SimpleEnumDefinition e) throws IOException, NobleIDLCompileErrorException {
		try(var w = openFile(dfn)) {
			w.print("package ");
			w.print(getJavaPackage(dfn.name()._package()));
			w.println(";");


			{
				var esexprOptions = e.esexprOptions().orElse(null);
				if(esexprOptions != null) {
					w.println("@dev.argon.esexpr.ESExprCodecGen");
				}
			}

			w.print("public enum ");
			w.print(convertIdPascal(dfn.name().name()));
			w.println(" {");
			w.indent();

			for(var c : e.cases()) {
				{
					var esexprOptions = c.esexprOptions().orElse(null);
					if(esexprOptions != null) {
						w.print("@dev.argon.esexpr.Constructor(\"");
						w.print(StringEscapeUtils.escapeJava(esexprOptions.name()));
						w.println("\")");
					}
				}

				w.print(convertIdConst(c.name()));
				w.println(",");
			}

			w.println(";");

			if(e.esexprOptions().isPresent()) {
				writeCodecMethod(w, dfn);
			}

			w.dedent();
			w.println("}");
		}
	}

	private void emitInterface(DefinitionInfo dfn, InterfaceDefinition iface) throws IOException, NobleIDLCompileErrorException {
		try(var w = openFile(dfn)) {
			w.print("package ");
			w.print(getJavaPackage(dfn.name()._package()));
			w.println(";");

			w.print("public interface ");
			w.print(convertIdPascal(dfn.name().name()));
			writeTypeParameters(w, dfn.typeParameters());
			w.println(" {");
			w.indent();

			for(var m : iface.methods()) {
				writeReturnType(w, m.returnType());
				w.print(" ");
				w.print(convertIdCamel(m.name()));
				writeTypeParameters(w, m.typeParameters());
				w.print("(");

				for(int i = 0; i < m.parameters().size(); ++i) {
					if(i > 0) {
						w.print(", ");
					}

					var param = m.parameters().get(i);
					writeTypeExpr(w, param.parameterType());
					w.print(" ");
					w.print(convertIdCamel(param.name()));
				}

				w.println(");");
			}

			w.dedent();
			w.println("}");
		}
	}







	private void writeTypeParameters(CodeWriter w, List<TypeParameter> typeParameters) throws IOException {
		if(!typeParameters.isEmpty()) {
			w.print("<");
			for(int i = 0; i < typeParameters.size(); ++i) {
				if(i > 0) {
					w.print(", ");
				}

				switch(typeParameters.get(i)) {
					case TypeParameter.Type(var name, _) -> {
						w.print(convertIdPascal(name));
					}
				}
			}
			w.print(">");
		}
	}

	private void writeTypeParametersAsArguments(CodeWriter w, List<TypeParameter> typeParameters) throws IOException {
		writeTypeParameters(w, typeParameters);
	}

	private void writeRecordParameters(CodeWriter w, List<RecordField> fields) throws IOException, NobleIDLCompileErrorException {
		w.println("(");
		w.indent();

		for(int i = 0; i < fields.size(); ++i) {
			if(i > 0) {
				w.println(",");
			}

			var field = fields.get(i);

			var esexprOptions = field.esexprOptions().orElse(null);
			if(esexprOptions != null) {
				switch(esexprOptions.kind()) {
					case EsexprRecordFieldKind.Positional(EsexprRecordPositionalMode.Required()) -> {}

					case EsexprRecordFieldKind.Positional(EsexprRecordPositionalMode.Optional(var elementType)) ->
						w.println("@dev.argon.esexpr.OptionalValue");

					case EsexprRecordFieldKind.Keyword(var name, var mode) -> {
						w.print("@dev.argon.esexpr.Keyword(\"");
						w.print(StringEscapeUtils.escapeJava(name));
						w.println("\")");

						switch(mode) {
							case EsexprRecordKeywordMode.Required() -> {}
							case EsexprRecordKeywordMode.Optional(_) ->
								w.println("@dev.argon.esexpr.OptionalValue");
							case EsexprRecordKeywordMode.DefaultValue(var defaultValue) -> {
								var valueWriter = new StringWriter();
								writeDecodedValue(valueWriter, defaultValue);
								w.print("@dev.argon.esexpr.DefaultValue(\"");
								w.print(StringEscapeUtils.escapeJava(valueWriter.toString()));
								w.println("\")");
							}
						}
					}

					case EsexprRecordFieldKind.Dict(_) ->
						w.println("@dev.argon.esexpr.Dict");

					case EsexprRecordFieldKind.Vararg(_) ->
						w.println("@dev.argon.esexpr.Vararg");
				}
			}

			writeTypeExpr(w, field.fieldType());
			w.print(" ");
			w.print(convertIdCamel(field.name()));
		}

		w.println();
		w.dedent();
		w.print(")");
	}

	private void writeCodecMethod(CodeWriter w, DefinitionInfo dfn) throws IOException, NobleIDLCompileErrorException {
		w.print("public static ");
		if(!dfn.typeParameters().isEmpty()) {
			writeTypeParameters(w, dfn.typeParameters());
			w.print(" ");
		}
		w.print("dev.argon.esexpr.ESExprCodec<");
		w.print(getJavaPackage(dfn.name()._package()));
		w.print(".");
		w.print(convertIdPascal(dfn.name().name()));
		writeTypeParametersAsArguments(w, dfn.typeParameters());
		w.print("> codec(");

		for(int i = 0; i < dfn.typeParameters().size(); ++i) {
			if(i > 0) {
				w.print(", ");
			}

			switch(dfn.typeParameters().get(i)) {
				case TypeParameter.Type type -> {
					w.print("dev.argon.esexpr.ESExprCodec<");
					w.print(convertIdPascal(type.name()));
					w.print("> ");
					w.print(convertIdCamel(type.name()));
					w.print("Codec");
				}
			}
		}

		w.println(") {");
		w.indent();

		w.print("return ");
		if(dfn.typeParameters().isEmpty()) {
			w.print(getJavaPackage(dfn.name()._package()));
			w.print(".");
			w.print(convertIdPascal(dfn.name().name()));
			w.print("_CodecImpl.INSTANCE");
		}
		else {
			w.print("new ");
			w.print(getJavaPackage(dfn.name()._package()));
			w.print(".");
			w.print(convertIdPascal(dfn.name().name()));
			w.print("_CodecImpl");
			writeTypeParametersAsArguments(w, dfn.typeParameters());
			w.print("(");

			for(int i = 0; i < dfn.typeParameters().size(); ++i) {
				if(i > 0) {
					w.print(", ");
				}

				switch(dfn.typeParameters().get(i)) {
					case TypeParameter.Type type -> {
						w.print(convertIdCamel(type.name()));
						w.print("Codec");
					}
				}
			}

			w.print(")");
		}

		w.println(";");

		w.dedent();
		w.println("}");
	}

	private void writeDecodedValue(Writer w, EsexprDecodedValue value) throws IOException, NobleIDLCompileErrorException {
		switch(value) {
			case EsexprDecodedValue.Record record -> {
				var javaType = (JavaTypeExpr.NormalType)typeExprToJava(record.t());
				w.write("new ");
				javaType.writeQualifiedClassName(w);
				javaType.writeArgs(w);
				w.write("(");

				int i = 0;
				for(var field : record.fields()) {
					if(i > 0) {
						w.write(", ");
					}
					++i;

					writeDecodedValue(w, field.value());
				}

				w.write(")");
			}
			case EsexprDecodedValue.Enum enumValue -> {
				var javaType = (JavaTypeExpr.NormalType)typeExprToJava(enumValue.t());
				w.write("new ");
				javaType.writeQualifiedClassName(w);
				w.write(".");
				w.write(enumValue.caseName());
				javaType.writeArgs(w);
				w.write("(");

				int i = 0;
				for(var field : enumValue.fields()) {
					if(i > 0) {
						w.write(", ");
					}
					++i;

					writeDecodedValue(w, field.value());
				}

				w.write(")");
			}

			case EsexprDecodedValue.Optional optional -> {
				if(optional.value().isPresent()) {
					writeStaticMethod(w, optional.t(), "fromElement");
					w.write("(");
					writeDecodedValue(w, optional.value().get());
					w.write(")");
				}
				else {
					writeStaticMethod(w, optional.t(), "empty");
					w.write("()");
				}
			}

			case EsexprDecodedValue.Vararg vararg -> {
				writeStaticMethod(w, vararg.t(), "fromValues");
				w.write("(");

				int i = 0;
				for(var item : vararg.values()) {
					if(i > 0) {
						w.write(", ");
					}
					++i;

					writeDecodedValue(w, item);
				}

				w.write(")");
			}

			case EsexprDecodedValue.Dict dict -> {
				writeStaticMethod(w, dict.t(), "fromMap");
				w.write("(java.util.Map.ofEntries(");

				int i = 0;
				for(var item : dict.values().map().entrySet()) {
					if(i > 0) {
						w.write(", ");
					}
					++i;

					w.write("java.util.Map.entry(\"");
					w.write(StringEscapeUtils.escapeJava(item.getKey()));
					w.write("\", ");
					writeDecodedValue(w, item.getValue());
					w.write(")");
				}

				w.write("))");
			}

			case EsexprDecodedValue.BuildFrom buildFrom -> {
				writeStaticMethod(w, buildFrom.t(), "buildFrom");
				w.write("(");
				writeDecodedValue(w, buildFrom.fromValue());
				w.write(")");
			}

			case EsexprDecodedValue.FromBool fromBool -> {
				writeStaticMethod(w, fromBool.t(), "fromBoolean");
				w.write("(");
				w.write(Boolean.toString(fromBool.b()));
				w.write(")");
			}
			case EsexprDecodedValue.FromInt fromInt -> {
				if(fromInt.minInt().isPresent() && fromInt.maxInt().isPresent()) {
					BigInteger min = fromInt.minInt().get();
					BigInteger max = fromInt.maxInt().get();

					if(min.signum() >= 0) {
						if(max.compareTo(BigInteger.valueOf(255)) <= 0) {
							writeStaticMethod(w, fromInt.t(), "fromUnsignedByte");
							w.write("((byte)");
							w.write(fromInt.i().toString());
							w.write(")");
							return;
						}
						else if(max.compareTo(BigInteger.valueOf(65535)) <= 0) {
							writeStaticMethod(w, fromInt.t(), "fromUnsignedShort");
							w.write("((short)");
							w.write(fromInt.i().toString());
							w.write(")");
							return;
						}
						else if(max.compareTo(BigInteger.valueOf(4294967295L)) <= 0) {
							writeStaticMethod(w, fromInt.t(), "fromUnsignedInt");
							w.write("(");
							w.write(Integer.toString(fromInt.i().intValue()));
							w.write(")");
							return;
						}
						else if(max.compareTo(new BigInteger("18446744073709551615")) <= 0) {
							writeStaticMethod(w, fromInt.t(), "fromUnsignedLong");
							w.write("(");
							w.write(Long.toString(fromInt.i().longValue()));
							w.write("L)");
							return;
						}
					}
					else {
						if(min.compareTo(BigInteger.valueOf(Byte.MIN_VALUE)) >= 0 && max.compareTo(BigInteger.valueOf(Byte.MAX_VALUE)) <= 0) {
							writeStaticMethod(w, fromInt.t(), "fromByte");
							w.write("((byte)");
							w.write(fromInt.i().toString());
							w.write(")");
							return;
						}
						else if(min.compareTo(BigInteger.valueOf(Short.MIN_VALUE)) >= 0 && max.compareTo(BigInteger.valueOf(Short.MAX_VALUE)) <= 0) {
							writeStaticMethod(w, fromInt.t(), "fromShort");
							w.write("((short)");
							w.write(fromInt.i().toString());
							w.write(")");
							return;
						}
						else if(min.compareTo(BigInteger.valueOf(Integer.MIN_VALUE)) >= 0 && max.compareTo(BigInteger.valueOf(Integer.MAX_VALUE)) <= 0) {
							writeStaticMethod(w, fromInt.t(), "fromInt");
							w.write("(");
							w.write(Integer.toString(fromInt.i().intValue()));
							w.write(")");
							return;
						}
						else if(min.compareTo(BigInteger.valueOf(Long.MIN_VALUE)) >= 0 && max.compareTo(BigInteger.valueOf(Long.MAX_VALUE)) <= 0) {
							writeStaticMethod(w, fromInt.t(), "fromLong");
							w.write("(");
							w.write(Long.toString(fromInt.i().longValue()));
							w.write("L)");
							return;
						}
					}
				}

				writeStaticMethod(w, fromInt.t(), "fromBigInteger");
				w.write("(new java.math.BigInteger(\"");
				w.write(fromInt.i().toString());
				w.write("\"))");
			}
			case EsexprDecodedValue.FromStr fromStr -> {
				writeStaticMethod(w, fromStr.t(), "fromString");
				w.write("(\"");
				w.write(StringEscapeUtils.escapeJava(fromStr.s()));
				w.write("\")");
			}
			case EsexprDecodedValue.FromBinary fromBinary -> {
				writeStaticMethod(w, fromBinary.t(), "fromByteArray");
				w.write("(new byte[] { ");
				for(byte b : fromBinary.b()) {
					w.write(Byte.toString(b));
					w.write(", ");
				}
				w.write("})");
			}
			case EsexprDecodedValue.FromFloat32 fromFloat32 -> {
				writeStaticMethod(w, fromFloat32.t(), "fromFloat");
				w.write("(");

				float f = fromFloat32.f();
				if(Float.isNaN(f)) {
					w.write("java.lang.Float.NaN");
				}
				else if(f == Float.POSITIVE_INFINITY) {
					w.write("java.lang.Float.POSITIVE_INFINITY");
				}
				else if(f == Float.NEGATIVE_INFINITY) {
					w.write("java.lang.Float.NEGATIVE_INFINITY");
				}
				else {
					w.write(Float.toHexString(f));
					w.write("f");
				}
				w.write(")");
			}
			case EsexprDecodedValue.FromFloat64 fromFloat64 -> {
				writeStaticMethod(w, fromFloat64.t(), "fromDouble");
				w.write("(");

				double f = fromFloat64.f();
				if(Double.isNaN(f)) {
					w.write("java.lang.Double.NaN");
				}
				else if(f == Double.POSITIVE_INFINITY) {
					w.write("java.lang.Double.POSITIVE_INFINITY");
				}
				else if(f == Double.NEGATIVE_INFINITY) {
					w.write("java.lang.Double.NEGATIVE_INFINITY");
				}
				else {
					w.write(Double.toHexString(f));
				}
				w.write(")");
			}
			case EsexprDecodedValue.FromNull fromNull -> {
				writeStaticMethod(w, fromNull.t(), "fromNull");
				w.write("()");
			}
		}
	}


	private void writeStaticMethod(Writer w, TypeExpr t, String methodName) throws IOException, NobleIDLCompileErrorException {
		typeExprToJava(t, true).writeStaticMethod(w, methodName);
	}


	private void writeTypeExpr(Writer w, TypeExpr t) throws IOException, NobleIDLCompileErrorException {
		typeExprToJava(t).nonReturnType().writeType(w);
	}

	private void writeReturnType(Writer w, TypeExpr t) throws IOException, NobleIDLCompileErrorException {
		typeExprToJava(t).writeType(w);
	}


	private sealed interface JavaTypeExpr {
		// Have to ensure that array type annotations are in the right position.
		default void writeType(Writer w) throws IOException {
			var arrayAnnStack = new ArrayList<ArrayList<String>>();
			var currentTypeAnn = new ArrayList<String>();

			JavaTypeExpr t = this;
			typeLoop:
			while(true) {
				switch(t) {
					case PrimitiveType primitiveType -> {
						writeAnns(w, currentTypeAnn);

						w.write(switch(primitiveType) {
							case BOOLEAN -> "boolean";
							case CHAR -> "char";
							case BYTE -> "byte";
							case SHORT -> "short";
							case INT -> "int";
							case LONG -> "long";
							case FLOAT -> "float";
							case DOUBLE -> "double";
							case VOID -> "void";
						});

						writeArrayParts(w, arrayAnnStack);

						break typeLoop;
					}

					case NormalType normalType -> {
						normalType.writePackagePrefix(w);

						writeAnns(w, currentTypeAnn);

						normalType.writeSimpleClassName(w);
						normalType.writeArgs(w);

						writeArrayParts(w, arrayAnnStack);

						break typeLoop;
					}

					case Annotated(var ann, var inner) -> {
						currentTypeAnn.addAll(ann);
						t = inner;
					}
					case ArrayType(var elementType) -> {
						arrayAnnStack.add(currentTypeAnn);
						currentTypeAnn = new ArrayList<>();
						t = elementType;
					}
				}
			}
		}

		private void writeAnns(Writer w, List<String> anns) throws IOException {
			for(var ann : anns) {
				w.write("@");
				w.write(ann);
				w.write(" ");
			}
		}

		private void writeArrayParts(Writer w, List<? extends List<String>> arrayAnnStack) throws IOException {
			for(var arrayPart : arrayAnnStack) {
				for(var ann : arrayPart) {
					w.write("@");
					w.write(ann);
					w.write(" ");
				}

				w.write("[]");
			}
		}


		void writeStaticMethod(Writer w, String methodName) throws IOException;

		JavaTypeExpr boxed();
		JavaTypeExpr nonReturnType();

		enum PrimitiveType implements JavaTypeExpr {
			BOOLEAN,
			CHAR,
			BYTE,
			SHORT,
			INT,
			LONG,
			FLOAT,
			DOUBLE,
			VOID,

			;


			@Override
			public void writeStaticMethod(Writer w, String methodName) throws IOException {
				throw new IllegalStateException();
			}

			@Override
			public JavaTypeExpr boxed() {
				var className = switch(this) {
					case BOOLEAN -> "Boolean";
					case CHAR -> "Character";
					case BYTE -> "Byte";
					case SHORT -> "Short";
					case INT -> "Integer";
					case LONG -> "Long";
					case FLOAT -> "Float";
					case DOUBLE -> "Double";
					case VOID -> "Object";
				};

				return new NormalType(Optional.of("java.lang"), className, List.of());
			}

			@Override
			public JavaTypeExpr nonReturnType() {
				if(this == VOID) {
					return boxed();
				}
				else {
					return this;
				}
			}
		}

		record NormalType(
			Optional<String> packageName,
			String className,

			List<JavaTypeExpr> args
		) implements JavaTypeExpr {
			@Override
			public void writeStaticMethod(Writer w, String methodName) throws IOException {
				writeQualifiedClassName(w);
				w.write(".");
				writeArgs(w);
				w.write(methodName);
			}

			@Override
			public JavaTypeExpr boxed() {
				return this;
			}

			@Override
			public JavaTypeExpr nonReturnType() {
				return this;
			}

			public void writePackagePrefix(Writer w) throws IOException {
				if(packageName.isPresent()) {
					w.write(packageName.get());
					w.write(".");
				}
			}

			public void writeSimpleClassName(Writer w) throws IOException {
				w.write(className);
			}

			public void writeQualifiedClassName(Writer w) throws IOException {
				writePackagePrefix(w);
				writeSimpleClassName(w);
			}

			public void writeArgs(Writer w) throws IOException {
				if(!args.isEmpty()) {
					w.write("<");
					int i = 0;
					for(var arg : args) {
						if(i > 0) {
							w.write(", ");
						}
						++i;

						arg.writeType(w);
					}

					w.write(">");
				}
			}
		}

		record ArrayType(
			JavaTypeExpr elementType
		) implements JavaTypeExpr {
			@Override
			public void writeStaticMethod(Writer w, String methodName) throws IOException {
				throw new IllegalStateException();
			}

			@Override
			public JavaTypeExpr boxed() {
				return this;
			}

			@Override
			public JavaTypeExpr nonReturnType() {
				return this;
			}
		}

		record Annotated(
			List<String> annotations,
			JavaTypeExpr inner
		) implements JavaTypeExpr {

			@Override
			public void writeStaticMethod(Writer w, String methodName) throws IOException {
				inner.writeStaticMethod(w, methodName);
			}

			@Override
			public JavaTypeExpr boxed() {
				return new Annotated(annotations, inner.boxed());
			}

			@Override
			public JavaTypeExpr nonReturnType() {
				return new Annotated(annotations, inner.nonReturnType());
			}
		}
	}


	private JavaTypeExpr typeExprToJava(TypeExpr t) throws NobleIDLCompileErrorException {
		return typeExprToJava(t, false);
	}

	private JavaTypeExpr typeExprToJava(TypeExpr t, boolean ignoreMapping) throws NobleIDLCompileErrorException {
		return switch(t) {
			case TypeExpr.DefinedType(var name, var args) -> {
				var mappedType = ignoreMapping ? null : getMappedType(name).orElse(null);
				if(mappedType == null) {
					var packageName = Optional.of(getJavaPackage(name._package())).filter(String::isEmpty);
					var className = convertIdPascal(name.name());
					var javaArgs = new ArrayList<JavaTypeExpr>();
					for(var arg : args) {
						javaArgs.add(typeExprToJava(arg, ignoreMapping).boxed());
					}
					yield new JavaTypeExpr.NormalType(packageName, className, javaArgs);
				}
				else {
					var typeParamMap = getTypeParameterMapping(t);
					yield mappedTypeToJava(typeParamMap, mappedType);
				}
			}
			case TypeExpr.TypeParameter(var name) -> new JavaTypeExpr.NormalType(Optional.empty(), convertIdPascal(name), List.of());
		};
	}

	private JavaTypeExpr mappedTypeToJava(Map<String, TypeExpr> typeParameters, JavaMappedType mappedType) throws NobleIDLCompileErrorException {
		return switch(mappedType) {
			case JavaMappedType.TypeName(var name) -> {
				var primType = getPrimitiveType(name);
				if(primType.isPresent()) {
					yield primType.get();
				}
				else {
					yield getJavaType(name, List.of());
				}
			}

			case JavaMappedType.Annotated(var t, var ann) ->
				new JavaTypeExpr.Annotated(ann, mappedTypeToJava(typeParameters, t));

			case JavaMappedType.Apply(var name, var args) -> {
				var javaArgs = new ArrayList<JavaTypeExpr>(args.size());
				for(var arg : args) {
					javaArgs.add(mappedTypeToJava(typeParameters, arg).boxed());
				}

				yield getJavaType(name, javaArgs);
			}

			case JavaMappedType.TypeParameter(var name) -> {
				var t = typeParameters.get(name);
				if(t == null) {
					throw new NobleIDLCompileErrorException("Invalid type parameter: " + name);
				}

				yield typeExprToJava(t);
			}

			case JavaMappedType.Array(var elementType) ->
				new JavaTypeExpr.ArrayType(mappedTypeToJava(typeParameters, elementType));
		};
	}

	private JavaTypeExpr.NormalType getJavaType(String className, List<JavaTypeExpr> args) {
		var lastDot = className.lastIndexOf('.');
		if(lastDot >= 0) {
			return new JavaTypeExpr.NormalType(Optional.of(className.substring(0, lastDot)), className.substring(lastDot + 1), args);
		}
		else {
			return new JavaTypeExpr.NormalType(Optional.empty(), className, args);
		}

	}

	private Optional<JavaTypeExpr.PrimitiveType> getPrimitiveType(String name) {
		return switch(name) {
			case "boolean" -> Optional.of(JavaTypeExpr.PrimitiveType.BOOLEAN);
			case "char" -> Optional.of(JavaTypeExpr.PrimitiveType.CHAR);
			case "byte" -> Optional.of(JavaTypeExpr.PrimitiveType.BYTE);
			case "short" -> Optional.of(JavaTypeExpr.PrimitiveType.SHORT);
			case "int" -> Optional.of(JavaTypeExpr.PrimitiveType.INT);
			case "long" -> Optional.of(JavaTypeExpr.PrimitiveType.LONG);
			case "float" -> Optional.of(JavaTypeExpr.PrimitiveType.FLOAT);
			case "double" -> Optional.of(JavaTypeExpr.PrimitiveType.DOUBLE);
			case "void" -> Optional.of(JavaTypeExpr.PrimitiveType.VOID);
			default -> Optional.empty();
		};
	}


	private Map<String, TypeExpr> getTypeParameterMapping(TypeExpr t) throws NobleIDLCompileErrorException {
		return switch(t) {
			case TypeExpr.DefinedType(var name, var args) -> {
				var dfn = model.definitions()
					.stream()
					.filter(d -> d.name().equals(name))
					.findFirst();

				if(dfn.isEmpty()) {
					throw new NobleIDLCompileErrorException("Could not find definition: " + name);
				}

				var typeParameters = dfn.get().typeParameters();

				if(typeParameters.size() != args.size()) {
					throw new NobleIDLCompileErrorException("Type parameter mismatch");
				}

				Map<String, TypeExpr> map = new HashMap<>();
				for(int i = 0; i < typeParameters.size(); ++i) {
					switch(typeParameters.get(i)) {
						case TypeParameter.Type type ->
							map.put(type.name(), args.get(i));
					}
				}
				yield map;
			}
			case TypeExpr.TypeParameter(_) -> Map.of();
		};
	}

	private Optional<JavaMappedType> getMappedType(QualifiedName name) throws NobleIDLCompileErrorException {
		var javaAnns = model.definitions()
			.stream()
			.filter(dfn -> dfn.name().equals(name))
			.findFirst()
			.stream()
			.filter(dfn -> dfn.definition() instanceof Definition.ExternType)
			.flatMap(dfn -> dfn.annotations().stream())
			.filter(ann -> ann.scope().equals("java"))
			.toList();

		for(var javaAnn : javaAnns) {
			JavaAnnExternType etAnn;
			try {
				etAnn = JavaAnnExternType.codec().decode(javaAnn.value());
			}
			catch(DecodeException e) {
				throw new NobleIDLCompileErrorException("Could not decode extern type annotation: " + javaAnn.value(), e);
			}

			if(!(etAnn instanceof JavaAnnExternType.MappedTo mappedTo)) {
				continue;
			}

			return Optional.of(mappedTo.javaType());
		}

		return Optional.empty();
	}



	private String getJavaPackage(PackageName packageName) throws NobleIDLCompileErrorException {
		var javaPackageName = packageMapping.get(packageName);
		if(javaPackageName == null) {
			throw new NobleIDLCompileErrorException("Unmapped package: " + PackageNameUtil.display(packageName));
		}

		return javaPackageName;
	}


	private static String convertIdPascal(String kebab) {
		return Arrays.stream(kebab.split("-"))
			.map(segment -> segment.substring(0, 1).toUpperCase(Locale.ROOT) + segment.substring(1))
			.collect(Collectors.joining());
	}

	private static String convertIdCamel(String kebab) {
		var pascal = convertIdPascal(kebab);
		var camel = pascal.substring(0, 1).toLowerCase(Locale.ROOT) + pascal.substring(1);

		if(SourceVersion.isKeyword(camel)) {
			camel = "_" + camel;
		}

		return camel;
	}

	private static String convertIdConst(String kebab) {
		return kebab.replace("-", "_").toUpperCase(Locale.ROOT);
	}
}

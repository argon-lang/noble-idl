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
import java.nio.file.Path;
import java.util.*;
import java.util.stream.Stream;

import static dev.argon.nobleidl.compiler.JavaBackendUtils.*;

public final class JavaBackend implements Backend {
	public JavaBackend(NobleIdlGenerationRequest<JavaLanguageOptions> options) {
		this.languageOptions = options.languageOptions();
		this.model = options.model();
		this.packageMapping = getPackageMapping(languageOptions);
	}

	private final JavaLanguageOptions languageOptions;
	private final NobleIdlModel model;
	private final Map<PackageName, String> packageMapping;



	private static Map<PackageName, String> getPackageMapping(JavaLanguageOptions options) {
		Map<PackageName, String> packageMapping = new HashMap<>();
		for(var entry : options.packageMapping().mapping().map().entrySet()) {
			packageMapping.put(PackageNameUtil.fromString(entry.getKey()), entry.getValue());
		}
		return packageMapping;
	}

	@Override
	public Stream<FileGenerator> emit() throws NobleIDLCompileErrorException {
		return model.definitions()
			.stream()
			.filter(dfn -> !dfn.isLibrary())
			.map(this::emitDefinition)
			.filter(Objects::nonNull);
	}

	private FileGenerator emitDefinition(DefinitionInfo dfn) {
		return switch(dfn.definition()) {
			case Definition.Record(var r) -> emitRecord(dfn, r);
			case Definition.Enum(var e) -> emitEnum(dfn, e);
			case Definition.SimpleEnum(var e) -> emitSimpleEnum(dfn, e);
			case Definition.ExternType(_) -> null;
			case Definition.Interface(var iface) -> emitInterface(dfn, iface);
			case Definition.ExceptionType(var ex) -> emitExceptionType(dfn, ex);
		};
	}

	@FunctionalInterface
	private interface FileGeneratorCallback {
		void write(CodeWriter w) throws IOException, NobleIDLCompileErrorException;
	}

	private FileGenerator fileGenerator(DefinitionInfo dfn, FileGeneratorCallback f) {
		return new FileGenerator() {
			@Override
			public Path getPath() throws NobleIDLCompileErrorException {
				var idlPackageName = dfn.name()._package();
				var javaPackageName = getJavaPackage(idlPackageName);

				var path = Path.of("");
				if(!javaPackageName.isEmpty()) {
					var parts = javaPackageName.split("\\.");
					for(String part : parts) {
						path = path.resolve(part);
					}
				}

				path = path.resolve(convertIdPascal(dfn.name().name()) + ".java");

				return path;
			}

			@Override
			public void generate(Writer writer) throws NobleIDLCompileErrorException, IOException {
				try(var w = new CodeWriter(writer)) {
					f.write(w);
				}
			}
		};
	}

	private FileGenerator emitRecord(DefinitionInfo dfn, RecordDefinition r) {
		return fileGenerator(dfn, w -> {
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
			writeTypeParameters(w, "T_", dfn.typeParameters());
			writeRecordParameters(w, r.fields());
			w.println(" {");
			w.indent();

			if(r.esexprOptions().isPresent()) {
				writeCodecMethod(w, dfn);
			}

			new JSAdapterWriter(w, dfn) {
				@Override
				protected void writeFromJS() throws IOException, NobleIDLCompileErrorException {
					writeFromJSVars("value_", r.fields());
					w.print("return new ");
					writeDefinitionAsType(w, dfn);
					w.print("(");

					int i = 0;
					for(var field : r.fields()) {
						if(i > 0) {
							w.print(", ");
						}
						++i;

						w.print("field_");
						w.print(convertIdCamelNoEscape(field.name()));
					}

					w.println(");");
				}

				@Override
				protected void writeToJS() throws IOException, NobleIDLCompileErrorException {
					w.println("var obj = context_.eval(\"js\", \"({})\");");
					writeToJSAssignments("value_", "obj", r.fields());
					w.println("return obj;");
				}
			}.writeJSAdapter();

			w.dedent();
			w.println("}");
		});
	}

	private FileGenerator emitEnum(DefinitionInfo dfn, EnumDefinition e) {
		return fileGenerator(dfn, w -> {
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
			writeTypeParameters(w, "T_", dfn.typeParameters());
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
				writeTypeParameters(w, "T_", dfn.typeParameters());
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

			new JSAdapterWriter(w, dfn) {
				@Override
				protected void writeFromJS() throws IOException, NobleIDLCompileErrorException {
					w.println("return switch(value_.getMember(\"$type\").asString()) {");
					w.indent();

					for(var c : e.cases()) {
						w.print("case \"");
						w.print(StringEscapeUtils.escapeJava(c.name()));
						w.println("\" -> {");
						w.indent();

						writeFromJSVars("value_", c.fields());
						w.print("yield new ");
						w.print(getJavaPackage(dfn.name()._package()));
						w.print(".");
						w.print(convertIdPascal(dfn.name().name()));
						w.print(".");
						w.print(convertIdPascal(c.name()));
						writeTypeParametersAsArguments(w, dfn.typeParameters());
						w.print("(");

						int i = 0;
						for(var field : c.fields()) {
							if(i > 0) {
								w.print(", ");
							}
							++i;

							w.print("field_");
							w.print(convertIdCamelNoEscape(field.name()));
						}

						w.println(");");

						w.dedent();
						w.println("}");
					}

					w.println("case java.lang.String name -> throw new java.lang.IllegalArgumentException(\"Invalid enum value: \" + name);");

					w.dedent();
					w.println("};");


				}

				@Override
				protected void writeToJS() throws IOException, NobleIDLCompileErrorException {
					w.println("var obj = context_.eval(\"js\", \"({})\");");

					w.println("switch(value_) {");
					w.indent();

					for(var c : e.cases()) {
						w.print("case ");
						w.print(getJavaPackage(dfn.name()._package()));
						w.print(".");
						w.print(convertIdPascal(dfn.name().name()));
						w.print(".");
						w.print(convertIdPascal(c.name()));
						writeTypeParametersAsArguments(w, dfn.typeParameters());
						w.println(" value2 -> {");
						w.indent();

						w.print("obj.putMember(\"$type\", \"");
						w.print(StringEscapeUtils.escapeJava(c.name()));
						w.println("\");");

						writeToJSAssignments("value2", "obj", c.fields());

						w.dedent();
						w.println("}");
					}

					w.dedent();
					w.println("}");

					w.println("return obj;");
				}
			}.writeJSAdapter();

			w.dedent();
			w.println("}");
		});
	}

	private FileGenerator emitSimpleEnum(DefinitionInfo dfn, SimpleEnumDefinition e) {
		return fileGenerator(dfn, w -> {
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

			new JSAdapterWriter(w, dfn) {
				@Override
				protected void writeFromJS() throws IOException, NobleIDLCompileErrorException {
					w.println("return switch(value_.asString()) {");
					w.indent();

					for(var c : e.cases()) {
						w.print("case \"");
						w.print(StringEscapeUtils.escapeJava(c.name()));
						w.print("\" -> ");
						writeDefinitionAsType(w, dfn);
						w.print(".");
						w.print(convertIdConst(c.name()));
						w.println(";");
					}

					w.println("case java.lang.String name -> throw new java.lang.IllegalArgumentException(\"Invalid enum value: \" + name);");

					w.dedent();
					w.println("};");
				}

				@Override
				protected void writeToJS() throws IOException, NobleIDLCompileErrorException {
					w.println("return context_.asValue(switch(value_) {");
					w.indent();

					for(var c : e.cases()) {
						w.print("case ");
						w.print(convertIdConst(c.name()));
						w.print(" -> \"");
						w.print(StringEscapeUtils.escapeJava(c.name()));
						w.println("\";");
					}

					w.dedent();
					w.println("});");
				}
			}.writeJSAdapter();

			w.dedent();
			w.println("}");
		});
	}

	private FileGenerator emitInterface(DefinitionInfo dfn, InterfaceDefinition iface) {
		return fileGenerator(dfn, w -> {
			w.print("package ");
			w.print(getJavaPackage(dfn.name()._package()));
			w.println(";");

			w.print("public interface ");
			w.print(convertIdPascal(dfn.name().name()));
			writeTypeParameters(w, "T_", dfn.typeParameters());
			w.println(" {");
			w.indent();

			for(var m : iface.methods()) {
				writeMethodSignature(w, m);
				w.println(";");
			}

			new JSAdapterWriter(w, dfn) {
				@Override
				protected void writeFromJS() throws IOException, NobleIDLCompileErrorException {
					w.println("{");
					w.indent();

					if(!dfn.typeParameters().isEmpty()) {
						w.println("@java.lang.SuppressWarnings(\"unchecked\")");
					}
					writeDefinitionAsType(w, dfn);
					w.print(" obj = (");
					writeDefinitionAsType(w, dfn);
					w.println(")dev.argon.nobleidl.runtime.graaljsInterop.ObjectWrapUtil.getJavaObject(context_, value_);");
					w.println("if(obj != null) return obj;");

					w.dedent();
					w.println("}");

					w.print("class ");
					w.print(convertIdPascal(dfn.name().name()));
					w.print("_Impl implements ");
					writeDefinitionAsType(w, dfn);
					w.println(", dev.argon.nobleidl.runtime.graaljsInterop.WrappedJavaScriptObject {");
					w.indent();

					w.println("@java.lang.Override");
					w.println("public org.graalvm.polyglot.Value _getAsJSValue() {");
					w.indent();
					w.println("return value_;");
					w.dedent();
					w.println("}");

					for(var m : iface.methods()) {
						w.println("@java.lang.Override");
						w.print("public ");
						writeMethodSignature(w, m);
						w.println(" {");
						w.indent();

						if(!(typeExprToJava(m.returnType()) instanceof JavaTypeExpr.VoidType)) {
							w.print("return ");
						}

						w.print("dev.argon.nobleidl.runtime.graaljsInterop.CallUtil.callJSFunction(context_, executor_, ");
						writeAdapterExpr(m.returnType());
						w.print(", ");

						if(m._throws().isPresent()) {
							var exType = m._throws().get();

							switch(exType) {
								case TypeExpr.DefinedType dt -> {
									w.print("\"");
									w.print(getExceptionTypeName(dt.name()));
									w.print("\", ");
									writeAdapterExpr(exType);
									w.print(", ");
								}
								case TypeExpr.TypeParameter tp -> {}
							}
						}

						w.println("() ->");
						w.indent();


						w.print("value_.invokeMember(\"");
						w.print(StringEscapeUtils.escapeJava(convertIdCamelNoEscape(m.name())));
						w.print("\"");
						for(var tp : m.typeParameters()) {
							switch(tp) {
								case TypeParameter.Type type -> {
									if(!type.constraints().stream().anyMatch(c -> switch(c) {
										case TypeParameterTypeConstraint.Exception() -> true;
									})) {
										continue;
									}

									w.print(", dev.argon.nobleidl.runtime.graaljsInterop.ErrorTypeAdapter.toJS(context_, executor_, errorType_");
									w.print(convertIdCamelNoEscape(type.name()));
									w.print(")");
								}
							}
						}

						for(var p : m.parameters()) {
							w.print(", ");
							writeAdapterExpr(p.parameterType());
							w.print(".toJS(context_, executor_, ");
							w.print(convertIdCamel(p.name()));
							w.print(")");
						}

						w.println(")");


						w.dedent();
						w.println(");");


						w.dedent();
						w.println("}");
					}

					w.dedent();
					w.println("}");

					w.println("return new ");
					w.print(convertIdPascal(dfn.name().name()));
					w.print("_Impl();");
				}

				@Override
				protected void writeToJS() throws IOException, NobleIDLCompileErrorException {
					w.println("{");
					w.indent();

					if(!dfn.typeParameters().isEmpty()) {
						w.println("@java.lang.SuppressWarnings(\"unchecked\")");
					}
					w.print("org.graalvm.polyglot.Value obj = dev.argon.nobleidl.runtime.graaljsInterop.ObjectWrapUtil.getJSObject(value_);");
					w.println("if(obj != null) return obj;");

					w.dedent();
					w.println("}");


					w.println("var obj = context_.eval(\"js\", \"({})\");");
					w.println("dev.argon.nobleidl.runtime.graaljsInterop.ObjectWrapUtil.putJavaObject(context_, obj, value_);");

					for(var m : iface.methods()) {

						var methodParamTypeConv = new TypeExprConverter() {
							@Override
							JavaTypeExpr typeExprToJava(TypeExpr t) throws NobleIDLCompileErrorException {
								if(t instanceof TypeExpr.TypeParameter tp && tp.owner() == TypeParameterOwner.BY_METHOD) {
									var typeParamDef = m.typeParameters().stream().filter(p -> switch(p) {
										case TypeParameter.Type tpt -> tpt.name().equals(tp.name());
									}).findFirst().orElseThrow();

									switch(typeParamDef) {
										case TypeParameter.Type tpt -> {
											if(tpt.constraints().stream().anyMatch(c -> switch(c) {
												case TypeParameterTypeConstraint.Exception() -> true;
											})) {
												return new JavaTypeExpr.NormalType(Optional.of("java.lang"), "Throwable", List.of());
											}
											else {
												return new JavaTypeExpr.NormalType(Optional.of("org.graalvm.polyglot"), "Value", List.of());
											}
										}
									}
								}
								else {
									return super.typeExprToJava(t);
								}
							}
						};


						w.print("obj.putMember(\"");
						w.print(StringEscapeUtils.escapeJava(convertIdCamelNoEscape(m.name())));
						w.println("\", (org.graalvm.polyglot.proxy.ProxyExecutable)(arguments -> {");
						w.indent();

						int argumentsIndex = 0;

						for(var tp : m.typeParameters()) {
							switch(tp) {
								case TypeParameter.Type type -> {
									if(!type.constraints().stream().anyMatch(c -> switch(c) {
										case TypeParameterTypeConstraint.Exception() -> true;
									})) {
										continue;
									}

									w.print("var errorType_");
									w.print(convertIdCamelNoEscape(type.name()));
									w.print(" = dev.argon.nobleidl.runtime.graaljsInterop.ErrorTypeAdapter.fromJS(context_, executor_, arguments[");
									w.print(Integer.toString(argumentsIndex));
									w.println("]);");

									++argumentsIndex;
								}
							}
						}

						for(var p : m.parameters()) {
							w.print("var arg_");
							w.print(convertIdCamel(p.name()));
							w.print(" = ");
							writeAdapterExpr(p.parameterType(), methodParamTypeConv);
							w.print(".fromJS(context_, executor_, arguments[");
							w.print(Integer.toString(argumentsIndex));
							w.println("]);");

							++argumentsIndex;
						}


						w.print("return dev.argon.nobleidl.runtime.graaljsInterop.CallUtil.callJavaFunction(context_, executor_, ");
						writeAdapterExpr(m.returnType(), methodParamTypeConv);
						w.print(", ");

						if(m._throws().isPresent()) {
							var exType = m._throws().get();

							switch(exType) {
								case TypeExpr.DefinedType _ -> {
									typeExprToJava(exType).writeStaticMethod(w, "class");
									w.print(", ");
									writeAdapterExpr(exType, methodParamTypeConv);
									w.print(", ");
								}
								case TypeExpr.TypeParameter _ -> {}
							}
						}

						w.print("() -> value_.");
						w.print(convertIdCamel(m.name()));
						w.print("(");

						argumentsIndex = 0;
						for(var tp : m.typeParameters()) {
							switch(tp) {
								case TypeParameter.Type type -> {
									if(!type.constraints().stream().anyMatch(c -> switch(c) {
										case TypeParameterTypeConstraint.Exception() -> true;
									})) {
										continue;
									}

									if(argumentsIndex > 0) {
										w.print(", ");
									}

									w.print("errorType_");
									w.print(convertIdCamelNoEscape(type.name()));

									++argumentsIndex;
								}
							}
						}

						for(var p : m.parameters()) {
							if(argumentsIndex > 0) {
								w.print(", ");
							}

							w.print("arg_");
							w.print(convertIdCamel(p.name()));

							++argumentsIndex;
						}

						w.println("));");

						w.dedent();
						w.println("}));");
					}

					w.println("return obj;");
				}
			}.writeJSAdapter();

			w.dedent();
			w.println("}");
		});
	}

	private void writeMethodSignature(CodeWriter w, InterfaceMethod m) throws IOException, NobleIDLCompileErrorException {
		writeTypeParameters(w, "M_", m.typeParameters());
		if(!m.typeParameters().isEmpty()) {
			w.print(" ");
		}

		writeReturnType(w, m.returnType());
		w.print(" ");
		w.print(convertIdCamel(m.name()));
		w.print("(");

		boolean needsComma = false;

		for(var tp : m.typeParameters()) {
			switch(tp) {
				case TypeParameter.Type typeParam -> {
					if(typeParam.constraints().stream().noneMatch(c -> c instanceof TypeParameterTypeConstraint.Exception)) {
						continue;
					}

					if(needsComma) {
						w.print(", ");
					}
					needsComma = true;

					w.print("dev.argon.nobleidl.runtime.ErrorType<? extends ");
					writeTypeExpr(w, new TypeExpr.TypeParameter(typeParam.name(), TypeParameterOwner.BY_METHOD));
					w.print("> errorType_");
					w.print(convertIdCamelNoEscape(typeParam.name()));
				}
			}
		}

		for(var param : m.parameters()) {
			if(needsComma) {
				w.print(", ");
			}
			needsComma = true;

			writeTypeExpr(w, param.parameterType());
			w.print(" ");
			w.print(convertIdCamel(param.name()));
		}

		w.print(")");

		w.print(" throws java.lang.InterruptedException");
		var throwsClause = m._throws().orElse(null);
		if(throwsClause != null) {
			w.print(", ");
			writeTypeExpr(w, throwsClause);
		}
	}


	private FileGenerator emitExceptionType(DefinitionInfo dfn, ExceptionTypeDefinition ex) {
		return fileGenerator(dfn, w -> {
			w.print("package ");
			w.print(getJavaPackage(dfn.name()._package()));
			w.println(";");

			w.print("public class ");
			w.print(convertIdPascal(dfn.name().name()));
			w.println(" extends dev.argon.nobleidl.runtime.NobleIDLException {");
			w.indent();

			w.print("public ");
			w.print(convertIdPascal(dfn.name().name()));
			w.print("(");
			writeTypeExpr(w, ex.information());
			w.println(" information) {");
			w.indent();
			w.println("this.information = information;");
			w.dedent();
			w.println("}");

			w.print("public ");
			w.print(convertIdPascal(dfn.name().name()));
			w.print("(");
			writeTypeExpr(w, ex.information());
			w.println(" information, java.lang.String message) {");
			w.indent();
			w.println("super(message);");
			w.println("this.information = information;");
			w.dedent();
			w.println("}");

			w.print("public ");
			w.print(convertIdPascal(dfn.name().name()));
			w.print("(");
			writeTypeExpr(w, ex.information());
			w.println(" information, java.lang.Throwable cause) {");
			w.indent();
			w.println("super(cause);");
			w.println("this.information = information;");
			w.dedent();
			w.println("}");

			w.print("public ");
			w.print(convertIdPascal(dfn.name().name()));
			w.print("(");
			writeTypeExpr(w, ex.information());
			w.println(" information, java.lang.String message, java.lang.Throwable cause) {");
			w.indent();
			w.println("super(message, cause);");
			w.println("this.information = information;");
			w.dedent();
			w.println("}");

			w.print("public final ");
			writeTypeExpr(w, ex.information());
			w.println(" information;");

			new JSAdapterWriter(w, dfn) {
				@Override
				protected void writeFromJS() throws IOException, NobleIDLCompileErrorException {
					w.print("return new ");
					writeDefinitionAsType(w, dfn);
					w.print("(");
					writeAdapterExpr(ex.information());
					w.println(".fromJS(context_, executor_, value_.getMember(\"information\")), value_.getMember(\"message\").asString(), dev.argon.nobleidl.runtime.graaljsInterop.ExceptionUtil.unwrapJSException(value_.getMember(\"cause\")));");
				}

				@Override
				protected void writeToJS() throws IOException, NobleIDLCompileErrorException {
					w.print("return context_.eval(\"js\", \"(info, message, cause) => { const e = new globalThis.Error(message ?? undefined, { cause: cause ?? undefined }); e.information = info; return e; }\").execute(");
					writeAdapterExpr(ex.information());
					w.println(".toJS(context_, executor_, value_.information), value_.getMessage(), dev.argon.nobleidl.runtime.graaljsInterop.ExceptionUtil.wrapJSException(context_, value_.getCause()));");
				}
			}.writeJSAdapter();

			w.dedent();
			w.println("}");
		});
	}





	private void writeTypeParameters(CodeWriter w, String prefix, List<TypeParameter> typeParameters) throws IOException {
		if(!typeParameters.isEmpty()) {
			w.print("<");
			for(int i = 0; i < typeParameters.size(); ++i) {
				if(i > 0) {
					w.print(", ");
				}

				switch(typeParameters.get(i)) {
					case TypeParameter.Type tp -> {
						w.print(prefix);
						w.print(convertIdPascal(tp.name()));
						if(tp.constraints().stream().anyMatch(c -> c instanceof TypeParameterTypeConstraint.Exception)) {
							w.print(" extends java.lang.Throwable");
						}
					}
				}
			}
			w.print(">");
		}
	}

	private void writeTypeParametersAsArguments(CodeWriter w, List<TypeParameter> typeParameters) throws IOException {
		if(!typeParameters.isEmpty()) {
			w.print("<");
			for(int i = 0; i < typeParameters.size(); ++i) {
				if(i > 0) {
					w.print(", ");
				}

				switch(typeParameters.get(i)) {
					case TypeParameter.Type tp -> {
						w.print("T_");
						w.print(convertIdPascal(tp.name()));
					}
				}
			}
			w.print(">");
		}
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
			writeTypeParameters(w, "M_", dfn.typeParameters());
			w.print(" ");
		}
		w.print("dev.argon.esexpr.ESExprCodec<");
		writeDefinitionAsType(w, dfn);
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
					w.print(convertIdCamelNoEscape(type.name()));
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
						w.print(convertIdCamelNoEscape(type.name()));
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

	private abstract class JSAdapterWriter {
		public JSAdapterWriter(CodeWriter w, DefinitionInfo dfn) {
			this.w = w;
			this.dfn = dfn;
		}

		private final CodeWriter w;
		private final DefinitionInfo dfn;


		protected abstract void writeFromJS() throws IOException, NobleIDLCompileErrorException;
		protected abstract void writeToJS() throws IOException, NobleIDLCompileErrorException;

		public final void writeJSAdapter() throws IOException, NobleIDLCompileErrorException {
			if(!languageOptions.generateGraalJSAdapters()) {
				return;
			}

			w.print("public static ");
			if(!dfn.typeParameters().isEmpty()) {
				writeTypeParameters(w, "T_", dfn.typeParameters());
				w.print(" ");
			}
			w.print("dev.argon.nobleidl.runtime.graaljsInterop.JSAdapter<");
			writeDefinitionAsType(w, dfn);
			w.print("> jsAdapter(");

			for(int i = 0; i < dfn.typeParameters().size(); ++i) {
				if(i > 0) {
					w.print(", ");
				}

				switch(dfn.typeParameters().get(i)) {
					case TypeParameter.Type type -> {
						if(type.constraints().stream().anyMatch(c -> c instanceof TypeParameterTypeConstraint.Exception)) {
							w.print("dev.argon.nobleidl.runtime.ErrorType<? extends T_");
							w.print(convertIdPascal(type.name()));
							w.print("> errorType_");
							w.print(convertIdCamelNoEscape(type.name()));
						}
						else {
							w.print("dev.argon.nobleidl.runtime.graaljsInterop.JSAdapter<T_");
							w.print(convertIdPascal(type.name()));
							w.print("> adapter_");
							w.print(convertIdCamelNoEscape(type.name()));
						}
					}
				}
			}

			w.println(") {");
			w.indent();

			w.println("return new dev.argon.nobleidl.runtime.graaljsInterop.JSAdapter<>() {");
			w.indent();

			w.println("@java.lang.Override");
			w.print("public ");
			writeDefinitionAsType(w, dfn);
			w.println(" fromJS(org.graalvm.polyglot.Context context_, dev.argon.nobleidl.runtime.graaljsInterop.JSExecutor executor_, org.graalvm.polyglot.Value value_) {");
			w.indent();
			writeFromJS();
			w.dedent();
			w.println("}");

			w.println("@java.lang.Override");
			w.print("public org.graalvm.polyglot.Value toJS(org.graalvm.polyglot.Context context_, dev.argon.nobleidl.runtime.graaljsInterop.JSExecutor executor_, ");
			writeDefinitionAsType(w, dfn);
			w.println(" value_) {");
			w.indent();
			writeToJS();
			w.dedent();
			w.println("}");


			w.dedent();
			w.println("};");

			w.dedent();
			w.println("}");
		}

		protected final void writeFromJSVars(String valueName, List<RecordField> fields) throws IOException, NobleIDLCompileErrorException {
			for(var field : fields) {
				w.print("var field_");
				w.print(convertIdCamelNoEscape(field.name()));
				w.print(" = ");
				writeAdapterExpr(field.fieldType());
				w.print(".fromJS(context_, executor_, ");
				w.print(valueName);
				w.print(".getMember(\"");
				w.print(StringEscapeUtils.escapeJava(convertIdCamelNoEscape(field.name())));
				w.println("\"));");
			}
		}

		protected final void writeToJSAssignments(String valueName, String objName, List<RecordField> fields) throws IOException, NobleIDLCompileErrorException {
			for(var field : fields) {
				w.print(objName);
				w.print(".putMember(\"");
				w.print(StringEscapeUtils.escapeJava(convertIdCamelNoEscape(field.name())));
				w.print("\", ");
				writeAdapterExpr(field.fieldType());
				w.print(".toJS(context_, executor_, ");
				w.print(valueName);
				w.print(".");
				w.print(convertIdCamel(field.name()));
				w.println("()));");
			}
		}

		protected final void writeAdapterExpr(TypeExpr t) throws IOException, NobleIDLCompileErrorException {
			writeAdapterExpr(t, new TypeExprConverter());
		}

		protected final void writeAdapterExpr(TypeExpr t, TypeExprConverter converter) throws IOException, NobleIDLCompileErrorException {
			switch(t) {
				case TypeExpr.DefinedType definedType -> {
					List<TypeParameter> typeParams = model.definitions().stream()
						.filter(d -> d.name().equals(definedType.name()))
						.findFirst()
						.map(DefinitionInfo::typeParameters)
						.orElseGet(List::of);



					converter.ignoreMapping().typeExprToJava(t).boxed().nonReturnType().writeStaticMethod(w, "jsAdapter");
					w.print("(");

					int i = 0;
					for(var arg : definedType.args()) {
						if(i > 0) {
							w.print(", ");
						}


						if(typeParams.get(i) instanceof TypeParameter.Type tpt && tpt.constraints().stream().anyMatch(c -> c instanceof TypeParameterTypeConstraint.Exception)) {
							writeErrorType(w, arg);
						}
						else {
							writeAdapterExpr(arg, converter);
						}


						++i;
					}

					w.print(")");
				}
				case TypeExpr.TypeParameter tp -> {
					switch(tp.owner()) {
						case BY_TYPE -> {
							w.print("adapter_");
							w.print(convertIdCamelNoEscape(tp.name()));
						}
						case BY_METHOD -> {
							w.print("dev.argon.nobleidl.runtime.graaljsInterop.JSAdapter.<");
							converter.typeExprToJava(t).writeType(w);
							w.print(">identity()");
						}
					}

				}
			}
		}
	}

	private String getExceptionTypeName(QualifiedName name) {
		var parts = new ArrayList<String>(name._package().parts().size() + 1);
		parts.addAll(name._package().parts());
		parts.add(name.name());
		return String.join(".", parts);
	}

	private void writeErrorType(CodeWriter w, TypeExpr t) throws IOException, NobleIDLCompileErrorException {
		switch(t) {
			case TypeExpr.DefinedType dt -> {
				w.println("dev.argon.nobleidl.runtime.ErrorType.fromClass(");
				w.print(getJavaPackage(dt.name()._package()));
				w.print(".");
				w.print(convertIdPascal(dt.name().name()));
				w.print(".class)");
			}

			case TypeExpr.TypeParameter tp -> {
				w.print("errorType_");
				w.print(convertIdCamelNoEscape(tp.name()));
			}
		}
	}


	private void writeDefinitionAsType(CodeWriter w, DefinitionInfo dfn) throws IOException, NobleIDLCompileErrorException {
		w.print(getJavaPackage(dfn.name()._package()));
		w.print(".");
		w.print(convertIdPascal(dfn.name().name()));
		writeTypeParametersAsArguments(w, dfn.typeParameters());
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
				w.write(convertIdPascal(enumValue.caseName()));
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

			case EsexprDecodedValue.SimpleEnum simpleEnumValue -> {
				var javaType = (JavaTypeExpr.NormalType)typeExprToJava(simpleEnumValue.t());
				javaType.writeQualifiedClassName(w);
				w.write(".");
				w.write(convertIdConst(simpleEnumValue.caseName()));
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
				w.write("(");

				var maxLevel = fromNull.maxLevel().orElse(null);
				if(maxLevel == null || !maxLevel.equals(BigInteger.ZERO)) {
					w.write(Integer.toString(fromNull.level().orElse(BigInteger.ZERO).intValueExact()));
				}

				w.write(")");
			}
		}
	}


	private void writeStaticMethod(Writer w, TypeExpr t, String methodName) throws IOException, NobleIDLCompileErrorException {
		typeExprToJava(t, true).writeStaticMethod(w, methodName);
	}


	private void writeTypeExpr(Writer w, TypeExpr t) throws IOException, NobleIDLCompileErrorException {
		typeExprToJava(t).nonReturnType().withNotNull().writeType(w);
	}

	private void writeReturnType(Writer w, TypeExpr t) throws IOException, NobleIDLCompileErrorException {
		typeExprToJava(t).withNotNull().writeType(w);
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
						});

						writeArrayParts(w, arrayAnnStack);

						break typeLoop;
					}

					case VoidType() -> {
						if(!arrayAnnStack.isEmpty()) {
							t = t.boxed();
							continue;
						}

						writeAnns(w, currentTypeAnn);
						w.write("void");

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

		/**
		 * Adds a not null annotation to the type.
		 * @return The annotated type.
		 */
		default JavaTypeExpr withNotNull() {
			return new Annotated(List.of("org.jetbrains.annotations.NotNull"), this);
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
			;


			@Override
			public JavaTypeExpr withNotNull() {
				return this;
			}

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
				};

				return new NormalType(Optional.of("java.lang"), className, List.of());
			}

			@Override
			public JavaTypeExpr nonReturnType() {
				return this;
			}


		}

		record VoidType() implements JavaTypeExpr {
			@Override
			public JavaTypeExpr withNotNull() {
				return this;
			}

			@Override
			public void writeStaticMethod(Writer w, String methodName) throws IOException {
				throw new IllegalStateException();
			}

			@Override
			public JavaTypeExpr boxed() {
				return new NormalType(Optional.of("dev.argon.nobleidl.runtime"), "Unit", List.of());
			}

			@Override
			public JavaTypeExpr nonReturnType() {
				return boxed();
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
		var converter = new TypeExprConverter();
		return converter.typeExprToJava(t);
	}

	private JavaTypeExpr typeExprToJava(TypeExpr t, boolean ignoreMapping) throws NobleIDLCompileErrorException {
		var converter = new TypeExprConverter();
		if(ignoreMapping) converter = converter.ignoreMapping();
		return converter.typeExprToJava(t);
	}

	private class TypeExprConverter implements Cloneable {

		public TypeExprConverter() {}

		protected boolean ignoreMapping = false;

		@Override
		public TypeExprConverter clone() {
			try {
				return (TypeExprConverter)super.clone();
			} catch(CloneNotSupportedException e) {
				throw new RuntimeException(e);
			}
		}

		public final TypeExprConverter ignoreMapping() {
			var other = clone();
			other.ignoreMapping = true;
			return other;
		}


		JavaTypeExpr typeExprToJava(TypeExpr t) throws NobleIDLCompileErrorException {
			return switch(t) {
				case TypeExpr.DefinedType(var name, var args) -> {
					var mappedType = ignoreMapping ? null : getMappedType(name).orElse(null);
					if(mappedType == null) {
						var packageName = Optional.of(getJavaPackage(name._package())).filter(pn -> !pn.isEmpty());
						var className = convertIdPascal(name.name());
						var javaArgs = new ArrayList<JavaTypeExpr>();

						var argConverter = clone();
						argConverter.ignoreMapping = false;
						for(var arg : args) {
							javaArgs.add(argConverter.typeExprToJava(arg).boxed());
						}
						yield new JavaTypeExpr.NormalType(packageName, className, javaArgs);
					}
					else {
						var typeParamMap = getTypeParameterMapping(t);
						yield mappedTypeToJava(typeParamMap, mappedType);
					}
				}
				case TypeExpr.TypeParameter(var name, var owner) -> {
					String prefix = switch(owner) {
						case BY_TYPE -> "T_";
						case BY_METHOD -> "M_";
					};
					yield new JavaTypeExpr.NormalType(Optional.empty(), prefix + convertIdPascal(name), List.of());
				}
			};
		}

		protected JavaTypeExpr typeArgToJava(QualifiedName name, int index, TypeExpr arg) throws NobleIDLCompileErrorException {
			var argConverter = clone();
			argConverter.ignoreMapping = false;
			return argConverter.typeExprToJava(arg).boxed();
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

		private JavaTypeExpr mappedTypeToJava(Map<String, TypeExpr> typeParameters, JavaMappedType mappedType) throws NobleIDLCompileErrorException {
			return switch(mappedType) {
				case JavaMappedType.TypeName(var name) -> {
					var primType = getPrimitiveType(name);
					if(primType.isPresent()) {
						yield primType.get();
					}
					else if(name.equals("void")) {
						yield new JavaTypeExpr.VoidType();
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
				case TypeExpr.TypeParameter(_, _) -> Map.of();
			};
		}

	}







	private String getJavaPackage(PackageName packageName) throws NobleIDLCompileErrorException {
		var javaPackageName = packageMapping.get(packageName);
		if(javaPackageName == null) {
			throw new NobleIDLCompileErrorException("Unmapped package: " + PackageNameUtil.display(packageName));
		}

		return javaPackageName;
	}

}

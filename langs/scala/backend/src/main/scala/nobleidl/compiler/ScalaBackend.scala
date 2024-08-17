package nobleidl.compiler

import esexpr.ESExprCodec
import nobleidl.compiler
import nobleidl.compiler.CodeWriter.Operations.*
import nobleidl.compiler.api.{java as _, *}
import nobleidl.compiler.format.PackageMapping
import org.apache.commons.text.StringEscapeUtils
import zio.*
import zio.stream.*

import _root_.java.nio.file.Path
import _root_.java.util.Locale
import nobleidl.compiler.api.java.{JavaAnnExternType, JavaMappedType}

import scala.jdk.CollectionConverters.*

private[compiler] class ScalaBackend(genRequest: NobleIdlGenerationRequest[ScalaLanguageOptions]) extends ScalaBackendBase {
  import ScalaBackendBase.*

  private val options: ScalaLanguageOptions = genRequest.languageOptions

  protected override def model: NobleIdlModel = genRequest.model
  protected override def packageMappingRaw: PackageMapping = options.packageMapping
  override protected def outputDir: Path = Path.of(options.outputDir).nn

  protected override def emitRecord(dfn: DefinitionInfo, r: RecordDefinition): ZIO[CodeWriter, NobleIDLCompileErrorException, Unit] =
    for
      _ <- write("package ")
      _ <- getScalaPackage(dfn.name.`package`).flatMap(writeln)

      _ <- ZIO.foreachDiscard(r.esexprOptions) { esexprOptions =>
        for
          _ <- write("@_root_.esexpr.constructor(\"")
          _ <- write(StringEscapeUtils.escapeJava(esexprOptions.constructor).nn)
          _ <- writeln("\")")
        yield ()
      }
      _ <- write("final case class ")
      _ <- write(convertIdPascal(dfn.name.name))
      _ <- writeTypeParameters(dfn.typeParameters)
      _ <- writeCaseClassParameters(r.fields)
      _ <- write(" derives _root_.esexpr.ESExprCodec, _root_.scala.CanEqual").when(r.esexprOptions.isDefined)
      _ <- writeln()

      _ <- write("object ")
      _ <- write(convertIdPascal(dfn.name.name))
      _ <- writeln(" {")
      _ <- indent()

      _ <- writeJavaAdapters(dfn)(
        writeToJava = adapterWriter =>
          for
            _ <- write("new ")
            _ <- adapterWriter.adaptedExprWriter.writeTypeExpr(definitionAsType(dfn), TypePosition.Normal)
            _ <- writeln("(")
            _ <- indent()
            _ <- writeScalaToJavaArgs(adapterWriter)(r.fields)
            _ <- dedent()
            _ <- writeln(")")
          yield (),
        writeFromJava = adapterWriter =>
          for
            _ <- AdapterScalaTypeExprWriter.writeTypeExpr(definitionAsType(dfn), TypePosition.Normal)
            _ <- writeln("(")
            _ <- indent()
            _ <- writeScalaFromJavaArgs(adapterWriter)(r.fields)
            _ <- dedent()
            _ <- writeln(")")
          yield (),
      )

      _ <- writeJSAdapters(dfn)(
        writeToJS = adapterWriter =>
          for
            _ <- write("new ")
            _ <- adapterWriter.adaptedExprWriter.writeTypeExpr(definitionAsType(dfn), TypePosition.Normal)
            _ <- writeln(" {")
            _ <- indent()
            _ <- writeScalaToJSArgs(adapterWriter)(r.fields)
            _ <- dedent()
            _ <- writeln("}")
          yield (),
        writeFromJS = adapterWriter =>
          for
            _ <- AdapterScalaTypeExprWriter.writeTypeExpr(definitionAsType(dfn), TypePosition.Normal)
            _ <- writeln("(")
            _ <- indent()
            _ <- writeScalaFromJSArgs(adapterWriter, write("j"))(r.fields)
            _ <- dedent()
            _ <- writeln(")")
          yield (),
      )

      _ <- dedent()
      _ <- writeln("}")

    yield ()

  protected override def emitEnum(dfn: DefinitionInfo, e: EnumDefinition): ZIO[CodeWriter, NobleIDLCompileErrorException, Unit] =
    for
      _ <- write("package ")
      _ <- getScalaPackage(dfn.name.`package`).flatMap(writeln)

      _ <- write("enum ")
      _ <- write(convertIdPascal(dfn.name.name))
      _ <- writeTypeParameters(dfn.typeParameters)
      _ <- write(" derives _root_.esexpr.ESExprCodec, _root_.scala.CanEqual").when(e.esexprOptions.isDefined)
      _ <- writeln(" {")
      _ <- indent()

      _ <- ZIO.foreachDiscard(e.cases) { c =>
        for
          _ <- ZIO.foreachDiscard(c.esexprOptions) { esexprOptions =>
            esexprOptions.caseType match {
              case constructor: EsexprEnumCaseType.Constructor =>
                for
                  _ <- write("@_root_.esexpr.constructor(\"")
                  _ <- write(StringEscapeUtils.escapeJava(constructor.name).nn)
                  _ <- writeln("\")")
                yield ()

              case _: EsexprEnumCaseType.InlineValue =>
                writeln("@_root_.esexpr.inlineValue")
            }
          }

          _ <- write("case ")
          _ <- write(convertIdPascal(c.name))
          _ <- writeCaseClassParameters(c.fields)
          _ <- writeln()
        yield ()
      }

      _ <- dedent()
      _ <- writeln("}")

      _ <- write("object ")
      _ <- write(convertIdPascal(dfn.name.name))
      _ <- writeln(" {")
      _ <- indent()

      _ <- writeJavaAdapters(dfn)(
        writeToJava = adapterWriter => {
          val dfnType = definitionAsType(dfn)
          for
            _ <- writeln("s_value match {")
            _ <- indent()
            _ <- ZIO.foreachDiscard(e.cases) { c =>
              for
                _ <- write("case s_value: ")
                _ <- AdapterScalaTypeExprWriter.writeDefinedTypeName(dfnType.name)
                _ <- write(".")
                _ <- write(convertIdPascal(c.name))
                _ <- AdapterScalaTypeExprWriter.writeTypeArguments(dfnType.args)
                _ <- writeln(" =>")
                _ <- indent()

                _ <- write("new ")
                _ <- adapterWriter.adaptedExprWriter.writeDefinedTypeName(dfnType.name)
                _ <- write(".")
                _ <- write(convertIdPascal(c.name))
                _ <- adapterWriter.adaptedExprWriter.writeTypeArguments(dfnType.args)
                _ <- writeln("(")
                _ <- indent()
                _ <- writeScalaToJavaArgs(adapterWriter)(c.fields)
                _ <- dedent()
                _ <- writeln(")")

                _ <- dedent()
              yield ()
            }
            _ <- dedent()
            _ <- writeln("}")
          yield ()
        },
        writeFromJava = adapterWriter => {
          val dfnType = definitionAsType(dfn)
          for
            _ <- writeln("j match {")
            _ <- indent()
            _ <- ZIO.foreachDiscard(e.cases) { c =>
              for
                _ <- write("case j: ")
                _ <- adapterWriter.adaptedExprWriter.writeDefinedTypeName(dfnType.name)
                _ <- write(".")
                _ <- write(convertIdPascal(c.name))
                _ <- adapterWriter.adaptedExprWriter.writeTypeArguments(dfnType.args)
                _ <- writeln(" =>")
                _ <- indent()

                _ <- write("new ")
                _ <- AdapterScalaTypeExprWriter.writeDefinedTypeName(dfnType.name)
                _ <- write(".")
                _ <- write(convertIdPascal(c.name))
                _ <- AdapterScalaTypeExprWriter.writeTypeArguments(dfnType.args)
                _ <- writeln("(")
                _ <- indent()
                _ <- writeScalaFromJavaArgs(adapterWriter)(c.fields)
                _ <- dedent()
                _ <- writeln(")")

                _ <- dedent()
              yield ()
            }

            _ <- writeln("case _ => throw new _root_.scala.MatchError(j)")

            _ <- dedent()
            _ <- writeln("}")
          yield ()
        },
      )

      _ <- writeJSAdapters(dfn)(
        writeToJS = adapterWriter => {
          val dfnType = definitionAsType(dfn)
          for
            _ <- writeln("s_value match {")
            _ <- indent()
            _ <- ZIO.foreachDiscard(e.cases) { c =>
              for
                _ <- write("case s_value: ")
                _ <- AdapterScalaTypeExprWriter.writeDefinedTypeName(dfnType.name)
                _ <- write(".")
                _ <- write(convertIdPascal(c.name))
                _ <- AdapterScalaTypeExprWriter.writeTypeArguments(dfnType.args)
                _ <- writeln(" =>")
                _ <- indent()

                _ <- write("new ")
                _ <- adapterWriter.adaptedExprWriter.writeDefinedTypeName(dfnType.name)
                _ <- write(".")
                _ <- write(convertIdPascal(c.name))
                _ <- adapterWriter.adaptedExprWriter.writeTypeArguments(dfnType.args)
                _ <- writeln(" {")
                _ <- indent()
                _ <- write("override val $type: \"")
                _ <- write(StringEscapeUtils.escapeJava(c.name).nn)
                _ <- write("\" = \"")
                _ <- write(StringEscapeUtils.escapeJava(c.name).nn)
                _ <- writeln("\"")
                _ <- writeScalaToJSArgs(adapterWriter)(c.fields)
                _ <- dedent()
                _ <- writeln("}")

                _ <- dedent()
              yield ()
            }
            _ <- dedent()
            _ <- writeln("}")
          yield ()
        },
        writeFromJS = adapterWriter => {
          val dfnType = definitionAsType(dfn)
          for
            _ <- writeln("j.$type match {")
            _ <- indent()
            _ <- ZIO.foreachDiscard(e.cases) { c =>
              for
                _ <- write("case \"")
                _ <- write(StringEscapeUtils.escapeJava(c.name).nn)
                _ <- writeln(" \" =>")
                _ <- indent()

                _ <- write("new ")
                _ <- AdapterScalaTypeExprWriter.writeDefinedTypeName(dfnType.name)
                _ <- write(".")
                _ <- write(convertIdPascal(c.name))
                _ <- AdapterScalaTypeExprWriter.writeTypeArguments(dfnType.args)
                _ <- writeln("(")
                _ <- indent()
                _ <- writeScalaFromJSArgs(
                  adapterWriter,
                  for
                    _ <- write("j.asInstanceOf[")
                    _ <- adapterWriter.adaptedExprWriter.writeDefinedTypeName(dfnType.name)
                    _ <- write(".")
                    _ <- write(convertIdPascal(c.name))
                    _ <- adapterWriter.adaptedExprWriter.writeTypeArguments(dfnType.args)
                    _ <- write("]")
                  yield ()
                )(c.fields)
                _ <- dedent()
                _ <- writeln(")")

                _ <- dedent()
              yield ()
            }

            _ <- writeln("case _ => throw new _root_.scala.MatchError(j)")

            _ <- dedent()
            _ <- writeln("}")
          yield ()
        },
      )

      _ <- dedent()
      _ <- writeln("}")

    yield ()

  protected override def emitSimpleEnum(dfn: DefinitionInfo, e: SimpleEnumDefinition): ZIO[CodeWriter, NobleIDLCompileErrorException, Unit] =
    for
      _ <- write("package ")
      _ <- getScalaPackage(dfn.name.`package`).flatMap(writeln)

      _ <- writeln("@_root_.esexpr.simple").when(e.esexprOptions.isDefined)
      _ <- write("enum ")
      _ <- write(convertIdPascal(dfn.name.name))
      _ <- write(" derives ")
      _ <- write("_root_.esexpr.ESExprCodec, ").when(e.esexprOptions.isDefined)
      _ <- write("_root_.scala.CanEqual")
      _ <- writeln(" {")
      _ <- indent()

      _ <- ZIO.foreachDiscard(e.cases) { c =>
        for
          _ <- ZIO.foreachDiscard(c.esexprOptions) { esexprOptions =>
            for
              _ <- write("@_root_.esexpr.constructor(\"")
              _ <- write(StringEscapeUtils.escapeJava(esexprOptions.name).nn)
              _ <- write("\")")
            yield ()
          }

          _ <- write("case ")
          _ <- write(convertIdPascal(c.name))
          _ <- writeln()
        yield ()
      }

      _ <- dedent()
      _ <- writeln("}")

      _ <- write("object ")
      _ <- write(convertIdPascal(dfn.name.name))
      _ <- writeln(" {")
      _ <- indent()

      _ <- writeJavaAdapters(dfn)(
        writeToJava = adapterWriter => {
          val dfnType = definitionAsType(dfn)
          for
            _ <- writeln("s_value match {")
            _ <- indent()
            _ <- ZIO.foreachDiscard(e.cases) { c =>
              for
                _ <- write("case ")
                _ <- AdapterScalaTypeExprWriter.writeDefinedTypeName(dfnType.name)
                _ <- write(".")
                _ <- write(convertIdPascal(c.name))
                _ <- writeln(" =>")
                _ <- indent()

                _ <- adapterWriter.adaptedExprWriter.writeDefinedTypeName(dfnType.name)
                _ <- write(".")
                _ <- writeln(convertIdConst(c.name))

                _ <- dedent()
              yield ()
            }
            _ <- dedent()
            _ <- writeln("}")
          yield ()
        },
        writeFromJava = adapterWriter => {
          val dfnType = definitionAsType(dfn)
          for
            _ <- write("j match {")
            _ <- indent()
            _ <- ZIO.foreachDiscard(e.cases) { c =>
              for
                _ <- write("case _: ")
                _ <- adapterWriter.adaptedExprWriter.writeDefinedTypeName(dfnType.name)
                _ <- write(".")
                _ <- writeln(convertIdConst(c.name))
                _ <- writeln(".type =>")
                _ <- indent()

                _ <- AdapterScalaTypeExprWriter.writeDefinedTypeName(dfnType.name)
                _ <- write(".")
                _ <- writeln(convertIdPascal(c.name))

                _ <- dedent()
              yield ()
            }
            _ <- dedent()
            _ <- writeln("}")
          yield ()
        },
      )

      _ <- writeJSAdapters(dfn)(
        writeToJS = _ => {
          val dfnType = definitionAsType(dfn)
          for
            _ <- writeln("s_value match {")
            _ <- indent()
            _ <- ZIO.foreachDiscard(e.cases) { c =>
              for
                _ <- write("case ")
                _ <- AdapterScalaTypeExprWriter.writeDefinedTypeName(dfnType.name)
                _ <- write(".")
                _ <- write(convertIdPascal(c.name))
                _ <- write(" => \"")
                _ <- write(StringEscapeUtils.escapeJava(c.name).nn)
                _ <- writeln("\"")

              yield ()
            }
            _ <- dedent()
            _ <- writeln("}")
          yield ()
        },
        writeFromJS = _ => {
          val dfnType = definitionAsType(dfn)
          for
            _ <- write("j match {")
            _ <- indent()
            _ <- ZIO.foreachDiscard(e.cases) { c =>
              for
                _ <- write("case \"")
                _ <- write(StringEscapeUtils.escapeJava(c.name).nn)
                _ <- writeln("\" =>")
                _ <- indent()

                _ <- AdapterScalaTypeExprWriter.writeDefinedTypeName(dfnType.name)
                _ <- write(".")
                _ <- writeln(convertIdPascal(c.name))

                _ <- dedent()
              yield ()
            }
            _ <- dedent()
            _ <- writeln("}")
          yield ()
        },
      )

      _ <- dedent()
      _ <- writeln("}")

    yield ()

  protected override def emitInterface(dfn: DefinitionInfo, iface: InterfaceDefinition): ZIO[CodeWriter, NobleIDLCompileErrorException, Unit] =
    for
      _ <- write("package ")
      _ <- getScalaPackage(dfn.name.`package`).flatMap(writeln)

      _ <- write("trait ")
      _ <- write(convertIdPascal(dfn.name.name))
      _ <- writeTypeParameters(dfn.typeParameters)
      _ <- writeln(" {")
      _ <- indent()

      _ <- ZIO.foreachDiscard(iface.methods) { m =>
        for
          _ <- write("def ")
          _ <- write(convertIdCamel(m.name))
          _ <- writeTypeParameters(m.typeParameters)
          _ <- write("(")

          _ <- ZIO.foreachDiscard(m.parameters.view.zipWithIndex) { (param, index) =>
            for
              _ <- write(", ").when(index > 0)
              _ <- write(convertIdCamel(param.name))
              _ <- write(": ")
              _ <- writeTypeExpr(param.parameterType)
            yield ()
          }

          _ <- write("): ")

          _ <- writeTypeExpr(m.returnType)
          _ <- writeln()
        yield ()
      }

      _ <- dedent()
      _ <- writeln("}")

    yield ()

  protected override def emitExceptionType(dfn: DefinitionInfo, ex: ExceptionTypeDefinition): ZIO[CodeWriter, NobleIDLCompileErrorException, Unit] =
    for
      _ <- write("package ")
      _ <- getScalaPackage(dfn.name.`package`).flatMap(writeln)

      _ <- write("class ")
      _ <- write(convertIdPascal(dfn.name.name))
      _ <- writeln("(")
      _ <- indent()
      _ <- write("val information: ")
      _ <- writeTypeExpr(ex.information)
      _ <- writeln(",")
      _ <- writeln("message: _root_.java.lang.String | _root_.scala.Null = null,")
      _ <- writeln("cause: _root_.java.lang.Throwable")
      _ <- dedent()
      _ <- writeln(") extends _root_.java.lang.Exception(message, cause)")
    yield ()


  private def writeCaseClassParameters(fields: Seq[RecordField]): ZIO[CodeWriter, NobleIDLCompileErrorException, Unit] =
    for
      _ <- writeln("(")
      _ <- indent()

      _ <- ZIO.foreachDiscard(fields) { field =>
        for
          _ <- ZIO.foreachDiscard(field.esexprOptions) { esexprOptions =>
            esexprOptions.kind match {
              case positional: EsexprRecordFieldKind.Positional =>
                positional.mode match {
                  case EsexprRecordPositionalMode.Required() => ZIO.unit
                  case EsexprRecordPositionalMode.Optional(_) => writeln("@_root_.esexpr.optional")
                }


              case keyword: EsexprRecordFieldKind.Keyword =>
                for
                  _ <- write("@_root_.esexpr.keyword(\"")
                  _ <- write(StringEscapeUtils.escapeJava(keyword.name).nn)
                  _ <- writeln("\")")

                  _ <- keyword.mode match {
                    case EsexprRecordKeywordMode.Required() => ZIO.unit

                    case EsexprRecordKeywordMode.Optional(_) =>
                      writeln("@_root_.esexpr.optional")

                    case EsexprRecordKeywordMode.DefaultValue(_) => ZIO.unit
                  }
                yield ()

              case _: EsexprRecordFieldKind.Dict =>
                writeln("@_root_.esexpr.dict")

              case _: EsexprRecordFieldKind.Vararg =>
                writeln("@_root_.esexpr.vararg")
            }
          }

          _ <- write(convertIdCamel(field.name))
          _ <- write(": ")
          _ <- writeTypeExpr(field.fieldType)
          _ <- ZIO.foreachDiscard(field.esexprOptions) { esexprOptions =>
            esexprOptions.kind match {
              case EsexprRecordFieldKind.Keyword(_, EsexprRecordKeywordMode.DefaultValue(defaultValue)) =>
                write(" = ") *> writeDecodedValue(defaultValue)

              case _ => ZIO.unit
            }
          }

          _ <- writeln(",")
        yield ()
      }

      _ <- dedent()
      _ <- write(")")
    yield ()


  private def writeScalaToJavaArgs(adapterWriter: AdapterWriter)(fields: Seq[RecordField]): ZIO[CodeWriter, NobleIDLCompileErrorException, Unit] =
    for
      _ <- ZIO.foreachDiscard(fields) { field =>
        for
          _ <- adapterWriter.writeAdapterExpr(field.fieldType, TypePosition.Normal)
          _ <- write(".toJava(s_value.")
          _ <- write(convertIdCamel(field.name))
          _ <- writeln("),")
        yield ()
      }
    yield ()

  private def writeScalaFromJavaArgs(adapterWriter: AdapterWriter)(fields: Seq[RecordField]): ZIO[CodeWriter, NobleIDLCompileErrorException, Unit] =
    for
      _ <- ZIO.foreachDiscard(fields) { field =>
        for
          _ <- adapterWriter.writeAdapterExpr(field.fieldType, TypePosition.Normal)
          _ <- write(".fromJava(j.")
          _ <- write(convertIdCamel(field.name))
          _ <- writeln("().nn),")
        yield ()
      }
    yield ()

  private def writeScalaToJSArgs(adapterWriter: AdapterWriter)(fields: Seq[RecordField]): ZIO[CodeWriter, NobleIDLCompileErrorException, Unit] =
    for
      _ <- ZIO.foreachDiscard(fields) { field =>
        for
          _ <- write("override val ")
          _ <- write(convertIdCamel(field.name))
          _ <- write(": ")
          _ <- adapterWriter.adaptedExprWriter.writeTypeExpr(field.fieldType, TypePosition.Normal)
          _ <- write(" = ")
          _ <- adapterWriter.writeAdapterExpr(field.fieldType, TypePosition.Normal)
          _ <- write(".toJS(s_value.")
          _ <- write(convertIdCamel(field.name))
          _ <- writeln(")")
        yield ()
      }
    yield ()

  private def writeScalaFromJSArgs(adapterWriter: AdapterWriter, writeValue: ZIO[CodeWriter, NobleIDLCompileErrorException, Unit])(fields: Seq[RecordField]): ZIO[CodeWriter, NobleIDLCompileErrorException, Unit] =
    for
      _ <- ZIO.foreachDiscard(fields) { field =>
        for
          _ <- adapterWriter.writeAdapterExpr(field.fieldType, TypePosition.Normal)
          _ <- write(".fromJS(")
          _ <- writeValue
          _ <- write(".")
          _ <- write(convertIdCamel(field.name))
          _ <- writeln("),")
        yield ()
      }
    yield ()

  private def writeDecodedValue(value: EsexprDecodedValue): ZIO[CodeWriter, NobleIDLCompileErrorException, Unit] =
    value match {
      case EsexprDecodedValue.FromBool(t, b) =>
        for
          _ <- writeTypeExpr(t)
          _ <- write(".fromBool(")
          _ <- write(b.toString)
          _ <- write(")")
        yield ()

      case _ => ???
    }

  private def writeJavaAdapters(dfn: DefinitionInfo)(
    writeToJava: AdapterWriter => ZIO[CodeWriter, NobleIDLCompileErrorException, Unit],
    writeFromJava: AdapterWriter => ZIO[CodeWriter, NobleIDLCompileErrorException, Unit],
  ): ZIO[CodeWriter, NobleIDLCompileErrorException, Unit] =
    writePlatformAdapters(dfn)(
      adapterMethod = "javaAdapter",
      toMethod = "toJava",
      fromMethod = "fromJava",
    )(
      buildAdapterWriter = () => options.javaAdapters.map { javaAdapters =>
        JavaAdapterWriter(JavaTypeExprWriter(buildPackageMapping(javaAdapters.packageMapping.mapping.dict)))
      },
      writeToPlatform = writeToJava,
      writeFromPlatform = writeFromJava,
    )

  private def writeJSAdapters(dfn: DefinitionInfo)(
    writeToJS: AdapterWriter => ZIO[CodeWriter, NobleIDLCompileErrorException, Unit],
    writeFromJS: AdapterWriter => ZIO[CodeWriter, NobleIDLCompileErrorException, Unit],
  ): ZIO[CodeWriter, NobleIDLCompileErrorException, Unit] =
    writePlatformAdapters(dfn)(
      adapterMethod = "jsAdapter",
      toMethod = "toJS",
      fromMethod = "fromJS",
    )(
      buildAdapterWriter = () => options.jsAdapters.map { jsAdapters =>
        JSAdapterWriter(JSTypeExprWriter(buildPackageMapping(jsAdapters.packageMapping.mapping.dict)))
      },
      writeToPlatform = writeToJS,
      writeFromPlatform = writeFromJS,
    )


  private def writePlatformAdapters[AdapterOptions](dfn: DefinitionInfo)(
    adapterMethod: String,
    toMethod: String,
    fromMethod: String,
  )(
    buildAdapterWriter: () => Option[AdapterWriter],
    writeToPlatform: AdapterWriter => ZIO[CodeWriter, NobleIDLCompileErrorException, Unit],
    writeFromPlatform: AdapterWriter => ZIO[CodeWriter, NobleIDLCompileErrorException, Unit],
  ): ZIO[CodeWriter, NobleIDLCompileErrorException, Unit] =
    ZIO.foreachDiscard(buildAdapterWriter()) { adapterWriter =>
      val dfnType = definitionAsType(dfn)

      for
        _ <- write("def ")
        _ <- write(adapterMethod)
        _ <- adapterWriter.writeTypeParamPairs(dfn)

        _ <- write("(")
        _ <- ZIO.foreachDiscard(dfn.typeParameters.view.zipWithIndex) {
          case (tp: TypeParameter.Type, index) =>
            for
              _ <- write(", ").when(index > 0)
              _ <- write(convertIdCamel(tp.name))
              _ <- write("Adapter: ")
              _ <- adapterWriter.writeAdapterType(TypeExpr.TypeParameter(tp.name, TypeParameterOwner.ByType), TypePosition.Normal)
            yield ()
        }
        _ <- write(")")

        _ <- write(": ")
        _ <- adapterWriter.writeAdapterType(dfnType, TypePosition.Normal)
        _ <- writeln(" =")
        _ <- indent()
        _ <- write("new ")
        _ <- adapterWriter.writeAdapterType(dfnType, TypePosition.Normal)
        _ <- writeln(" {")
        _ <- indent()

        _ <- write("override def ")
        _ <- write(toMethod)
        _ <- write("(s_value: ")
        _ <- AdapterScalaTypeExprWriter.writeTypeExpr(dfnType, TypePosition.Normal)
        _ <- write("): ")
        _ <- adapterWriter.adaptedExprWriter.writeTypeExpr(dfnType, TypePosition.Normal)
        _ <- writeln(" = {")
        _ <- indent()
        _ <- writeToPlatform(adapterWriter)
        _ <- dedent()
        _ <- writeln("}")

        _ <- write("override def ")
        _ <- write(fromMethod)
        _ <- write("(j: ")
        _ <- adapterWriter.adaptedExprWriter.writeTypeExpr(dfnType, TypePosition.Normal)
        _ <- write("): ")
        _ <- AdapterScalaTypeExprWriter.writeTypeExpr(dfnType, TypePosition.Normal)
        _ <- writeln(" = {")
        _ <- indent()
        _ <- writeFromPlatform(adapterWriter)
        _ <- dedent()
        _ <- writeln("}")

        _ <- dedent()
        _ <- writeln("}")
        _ <- dedent()
      yield ()
    }

  private object AdapterScalaTypeExprWriter extends TypeExprWriter {
    override def writeTypeParameter(parameter: TypeExpr.TypeParameter, pos: TypePosition): ZIO[CodeWriter, NobleIDLCompileErrorException, Unit] =
      write("S" + convertIdPascal(parameter.name))
  }

  private abstract class AdapterWriter {

    def adapterType: String
    def adapterMemberName: String
    def adaptedExprWriter: TypeExprWriter

    def writeAdapterType(t: TypeExpr, pos: TypePosition): ZIO[CodeWriter, NobleIDLCompileErrorException, Unit] =
      for
        _ <- write("_root_.nobleidl.core.")
        _ <- write(adapterType)
        _ <- write("[")
        _ <- AdapterScalaTypeExprWriter.writeTypeExpr(t, pos)
        _ <- write(", ")
        _ <- adaptedExprWriter.writeTypeExpr(t, pos)
        _ <- write("]")
      yield ()


    def writeAdapterExpr(t: TypeExpr, pos: TypePosition): ZIO[CodeWriter, NobleIDLCompileErrorException, Unit] =
      t match {
        case TypeExpr.DefinedType(name, args) =>
          for
            _ <- AdapterScalaTypeExprWriter.writeDefinedTypeName(name)
            _ <- write(".")
            _ <- write(adapterMemberName)
            _ <- writeTypeArgPairs(args)
            _ <- write("(")
            _ <- ZIO.foreachDiscard(args.view.zipWithIndex) { (arg, index) =>
              for
                _ <- write(", ").when(index > 0)
                _ <- writeAdapterExpr(arg, TypePosition.TypeArgument)
              yield ()
            }
            _ <- write(")")
          yield ()

        case TypeExpr.TypeParameter(name, _) =>
          write(convertIdCamel(name) + "Adapter")
      }

    def writeTypeParamPairs(dfn: DefinitionInfo) =
      (
        for
          _ <- write("[")
          _ <- ZIO.foreachDiscard(dfn.typeParameters.view.zipWithIndex) {
            case (param: TypeParameter.Type, index) =>
              for
                _ <- write(", ").when(index > 0)
                _ <- write("S")
                _ <- write(convertIdPascal(param.name))
                _ <- write(", J")
                _ <- write(convertIdPascal(param.name))
              yield ()
          }
          _ <- write("]")
        yield ()
      ).whenDiscard(dfn.typeParameters.nonEmpty)

    def writeTypeArgPairs(args: Seq[TypeExpr]) =
      (
        for
          _ <- write("[")
          _ <- ZIO.foreachDiscard(args.view.zipWithIndex) { (arg, index) =>
            for
              _ <- write(", ").when(index > 0)
              _ <- AdapterScalaTypeExprWriter.writeTypeExpr(arg, TypePosition.TypeArgument)
              _ <- write(", ")
              _ <- adaptedExprWriter.writeTypeExpr(arg, TypePosition.TypeArgument)
            yield ()
          }
          _ <- write("]")
        yield ()
        ).whenDiscard(args.nonEmpty)
  }

  private abstract class JavaAdapterWriterBase extends AdapterWriter {
    override def adapterType: String = "JavaAdapter"
    override def adapterMemberName: String = "javaAdapter"
    override def adaptedExprWriter: JavaTypeExprWriter
  }

  private class JavaAdapterWriterBoxed(val adaptedExprWriter: JavaTypeExprWriter) extends JavaAdapterWriterBase {
    override def adapterType: String = "JavaAdapter"
    override def adapterMemberName: String = "javaAdapterBoxed"
  }

  private class JavaAdapterWriter(val adaptedExprWriter: JavaTypeExprWriter) extends JavaAdapterWriterBase() {
    override def adapterType: String = "JavaAdapter"
    override def adapterMemberName: String = "javaAdapter"

    override def writeAdapterExpr(t: TypeExpr, pos: TypePosition): ZIO[CodeWriter, NobleIDLCompileErrorException, Unit] =
      if adaptedExprWriter.isBoxedType(t, pos) then
        JavaAdapterWriterBoxed(adaptedExprWriter).writeAdapterExpr(t, pos)
      else
        super.writeAdapterExpr(t, pos)
  }

  private class JavaTypeExprWriter(javaPackageMapping: Map[PackageName, String]) extends TypeExprWriter {
    override def writeDefinedType(definedType: TypeExpr.DefinedType, pos: TypePosition): ZIO[CodeWriter, NobleIDLCompileErrorException, Unit] =
      getMappedType(definedType)
        .map(writeMappedType(_, _, pos))
        .getOrElse(super.writeDefinedType(definedType, pos))

    private def getMappedType(t: TypeExpr): Option[(JavaMappedType, Map[String, TypeExpr])] =
      Some(t)
        .collect {
          case t: TypeExpr.DefinedType => t
        }
        .flatMap { definedType =>
          model.definitions.find(_.name == definedType.name)
            .flatMap { dfn =>
                dfn.definition match {
                  case Definition.ExternType(_) =>
                    dfn.annotations
                      .view
                      .filter(_.scope == "java")
                      .flatMap { ann =>
                        summon[ESExprCodec[JavaAnnExternType]].decode(ann.value).toOption
                      }
                      .collectFirst {
                        case JavaAnnExternType.MappedTo(mappedType) => mappedType
                      }
                      .map { mappedType =>
                        val argMapping = dfn.typeParameters
                          .map { case TypeParameter.Type(name, _) => name }
                          .zip(definedType.args)
                          .toMap

                        (mappedType, argMapping)
                      }

                  case _ => None
                }
              }
        }

    private def getJavaPackage(packageName: PackageName): IO[NobleIDLCompileErrorException, String] =
      ZIO.fromEither(
        javaPackageMapping.get(packageName)
          .toRight { NobleIDLCompileErrorException("Unmapped Java package: " + packageName.display) }
      )

    override def writePackageName(packageName: PackageName): ZIO[CodeWriter, NobleIDLCompileErrorException, Unit] =
      getJavaPackage(packageName).flatMap(write)

    override def writeTypeParameter(parameter: TypeExpr.TypeParameter, pos: TypePosition): ZIO[CodeWriter, NobleIDLCompileErrorException, Unit] =
      write("J" + convertIdPascal(parameter.name))

    private def writeMappedType(t: JavaMappedType, argsMapping: Map[String, TypeExpr], pos: TypePosition): ZIO[CodeWriter, NobleIDLCompileErrorException, Unit] =
      val boxed = pos == TypePosition.TypeArgument
      t match {
        case JavaMappedType.TypeName("boolean") => write(if boxed then "_root_.java.lang.Boolean" else "_root_.scala.Boolean")
        case JavaMappedType.TypeName("char") => write(if boxed then "_root_.java.lang.Character" else "_root_.scala.Char")
        case JavaMappedType.TypeName("byte") => write(if boxed then "_root_.java.lang.Byte" else "_root_.scala.Byte")
        case JavaMappedType.TypeName("short") => write(if boxed then "_root_.java.lang.Short" else "_root_.scala.Short")
        case JavaMappedType.TypeName("int") => write(if boxed then "_root_.java.lang.Integer" else "_root_.scala.Int")
        case JavaMappedType.TypeName("long") => write(if boxed then "_root_.java.lang.Long" else "_root_.scala.Long")
        case JavaMappedType.TypeName("float") => write(if boxed then "_root_.java.lang.Float" else "_root_.scala.Float")
        case JavaMappedType.TypeName("double") => write(if boxed then "_root_.java.lang.Double" else "_root_.scala.Double")
        case JavaMappedType.TypeName("void") => write(if pos == TypePosition.ReturnType then "_root_.scala.Unit" else "_root_.scala.AnyRef")
        case JavaMappedType.TypeName(name) => write("_root_.") *> write(name)
        case JavaMappedType.Apply(name, args) =>
          for
            _ <- write("_root_.")
            _ <- write(name)
            _ <- write("[")
            _ <- ZIO.foreachDiscard(args.view.zipWithIndex) { (arg, index) =>
              write(", ").when(index > 0) *> writeMappedType(arg, argsMapping, TypePosition.TypeArgument)
            }
            _ <- write("]")
          yield ()

        case JavaMappedType.Annotated(t, _) => writeMappedType(t, argsMapping, pos)

        case JavaMappedType.TypeParameter(name) =>
          ZIO.fromEither(argsMapping.get(name).toRight { NobleIDLCompileErrorException("Unknown type parameter: " + name) })
            .flatMap(writeTypeExpr(_, pos))

        case JavaMappedType.Array(elementType) =>
          write("_root_.scala.Array[") *> writeMappedType(elementType, argsMapping, TypePosition.Normal) *> write("]")
      }
    end writeMappedType

    def isBoxedType(t: TypeExpr, pos: TypePosition): Boolean =
      getMappedType(t).exists { (mappedType, _) =>
        def checkMappedType(mappedType: JavaMappedType): Boolean =
          mappedType match {
            case JavaMappedType.TypeName("boolean" | "char" | "byte" | "short" | "int" | "long" | "float" | "double") =>
              pos == TypePosition.TypeArgument

            case JavaMappedType.TypeName("void") => pos != TypePosition.ReturnType

            case JavaMappedType.Annotated(inner, _) => checkMappedType(inner)

            case _ => false
          }

        checkMappedType(mappedType)
      }
  }


  private class JSAdapterWriter(val adaptedExprWriter: JSTypeExprWriter) extends AdapterWriter {
    override def adapterType: String = "JSAdapter"
    override def adapterMemberName: String = "jsAdapter"
  }

  private class JSTypeExprWriter(jsPackageMapping: Map[PackageName, String]) extends TypeExprWriter {

    private def getJSPackage(packageName: PackageName): IO[NobleIDLCompileErrorException, String] =
      ZIO.fromEither(
        jsPackageMapping.get(packageName)
          .toRight {
            NobleIDLCompileErrorException("Unmapped Java package: " + packageName.display)
          }
      )

    override def writePackageName(packageName: PackageName): ZIO[CodeWriter, NobleIDLCompileErrorException, Unit] =
      getJSPackage(packageName).flatMap(write)

    override def writeTypeParameter(parameter: TypeExpr.TypeParameter, pos: TypePosition): ZIO[CodeWriter, NobleIDLCompileErrorException, Unit] =
      write("J" + convertIdPascal(parameter.name))
  }

}

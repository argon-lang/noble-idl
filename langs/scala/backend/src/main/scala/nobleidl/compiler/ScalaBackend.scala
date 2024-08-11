package nobleidl.compiler

import dev.argon.nobleidl.compiler.JavaLanguageOptions
import nobleidl.compiler.CodeWriter.Operations.*
import nobleidl.compiler.api.*
import org.apache.commons.text.StringEscapeUtils
import zio.*
import zio.stream.*

import java.util.Locale
import scala.jdk.CollectionConverters.*

private[nobleidl] class ScalaBackend(genRequest: NobleIdlGenerationRequest[JavaLanguageOptions]) {
  import ScalaBackend.*

  private val options: JavaLanguageOptions = genRequest.languageOptions
  private val model = genRequest.model
  private val packageMapping = options.packageMapping.nn.mapping.nn.map.nn.asScala
    .view
    .map { (k, v) => PackageName.fromString(k) -> v }
    .toMap

  def emit: Stream[NobleIDLCompileErrorException, GeneratedFile] =
    ZStream.fromIterable(model.definitions)
      .filterNot(_.isLibrary)
      .flatMap(emitDefinition)



  private def emitDefinition(dfn: DefinitionInfo): Stream[NobleIDLCompileErrorException, GeneratedFile] =
    dfn.definition match {
      case Definition.Record(r) => writeFile(dfn)(emitRecord(dfn, r))
      case Definition.Enum(e) => writeFile(dfn)(emitEnum(dfn, e))
      case Definition.SimpleEnum(e) => writeFile(dfn)(emitSimpleEnum(dfn, e))
      case Definition.ExternType(_) => ZStream()
      case Definition.Interface(iface) => writeFile(dfn)(emitInterface(dfn, iface))
    }

  private def writeFile(dfn: DefinitionInfo)(data: ZIO[CodeWriter, NobleIDLCompileErrorException, Unit]): Stream[NobleIDLCompileErrorException, GeneratedFile] =
    ZStream.fromZIO(getScalaPackage(dfn.name.`package`))
      .map { pkg =>
        GeneratedFile(
          path = pkg.split("\\.").nn.view.map(_.nn).toSeq :+ (convertIdPascal(dfn.name.name) + ".scala"),
          content = CodeWriter.withWriter(data)
        )
      }

  private def emitRecord(dfn: DefinitionInfo, r: RecordDefinition): ZIO[CodeWriter, NobleIDLCompileErrorException, Unit] =
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

    yield ()

  private def emitEnum(dfn: DefinitionInfo, e: EnumDefinition): ZIO[CodeWriter, NobleIDLCompileErrorException, Unit] =
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

      _ <- dendent()
      _ <- writeln("}")

    yield ()

  private def emitSimpleEnum(dfn: DefinitionInfo, e: SimpleEnumDefinition): ZIO[CodeWriter, NobleIDLCompileErrorException, Unit] =
    for
      _ <- write("package ")
      _ <- getScalaPackage(dfn.name.`package`).flatMap(writeln)

      _ <- writeln("@_root_.esexpr.simple")
      _ <- write("enum ")
      _ <- write(convertIdPascal(dfn.name.name))
      _ <- writeTypeParameters(dfn.typeParameters)
      _ <- write(" derives _root_.esexpr.ESExprCodec, _root_.scala.CanEqual").when(e.esexprOptions.isDefined)
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

      _ <- dendent()
      _ <- writeln("}")

    yield ()

  private def emitInterface(dfn: DefinitionInfo, iface: InterfaceDefinition): ZIO[CodeWriter, NobleIDLCompileErrorException, Unit] =
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

          _ <- write(")")
        yield ()
      }

      _ <- dendent()
      _ <- writeln("}")

    yield ()

  private def writeTypeParameters(tps: Seq[TypeParameter]): ZIO[CodeWriter, NobleIDLCompileErrorException, Unit] =
    (
      for
        _ <- writeln("[")

        _ <- ZIO.foreachDiscard(tps.zipWithIndex) {
          case (tp: TypeParameter.Type, index) =>
            for
              _ <- write(", ").when(index > 0)
              _ <- write(convertIdPascal(tp.name))
            yield ()
        }

        _ <- write("]")
      yield ()
    ).when(tps.nonEmpty).unit

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

      _ <- dendent()
      _ <- write(")")
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

  private def writeTypeExpr(expr: TypeExpr): ZIO[CodeWriter, NobleIDLCompileErrorException, Unit] =
    expr match {
      case definedType: TypeExpr.DefinedType =>
        for
          _ <- write("_root_.")
          _ <- getScalaPackage(definedType.name.`package`).flatMap(write)
          _ <- write(".")
          _ <- write(convertIdPascal(definedType.name.name))
          _ <- (
            for
              _ <- write("[")
              _ <- ZIO.foreachDiscard(definedType.args.view.zipWithIndex) { (arg, index) =>
                write(", ").when(index > 0) *>
                  writeTypeExpr(arg)
              }
              _ <- write("]")
            yield ()
          ).when(definedType.args.nonEmpty)
        yield ()

      case parameter: TypeExpr.TypeParameter =>
        write(convertIdPascal(parameter.name))
    }


  private def getScalaPackage(packageName: PackageName): IO[NobleIDLCompileErrorException, String] =
    ZIO.fromEither(
      packageMapping.get(packageName)
        .toRight { NobleIDLCompileErrorException("Unmapped package: " + packageName.display) }
    )

  private def convertIdPascal(kebab: String): String =
    kebab.split("-")
      .nn
      .view
      .map(_.nn)
      .map { segment => segment.substring(0, 1).nn.toUpperCase(Locale.ROOT).nn + segment.substring(1).nn }
      .mkString

  private def convertIdCamel(kebab: String): String =
    val pascal = convertIdPascal(kebab)
    var camel = pascal.substring(0, 1).nn.toLowerCase(Locale.ROOT).nn + pascal.substring(1).nn

    if keywords.contains(camel) then
      s"`$camel`"
    else
      camel
  end convertIdCamel

}

private[nobleidl] object ScalaBackend {
  final case class GeneratedFile(path: Seq[String], content: Stream[NobleIDLCompileErrorException, String])

  private val keywords = Seq(
    "abstract",
    "case",
    "catch",
    "class",
    "def",
    "do",
    "else",
    "enum",
    "export",
    "extends",
    "false",
    "final",
    "finally",
    "for",
    "given",
    "if",
    "implicit",
    "import",
    "lazy",
    "match",
    "new",
    "null",
    "object",
    "override",
    "package",
    "private",
    "protected",
    "return",
    "sealed",
    "super",
    "then",
    "throw",
    "trait",
    "true",
    "try",
    "type",
    "val",
    "var",
    "while",
    "with",
    "yield",
  )
}

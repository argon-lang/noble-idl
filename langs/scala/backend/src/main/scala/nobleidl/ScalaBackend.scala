package nobleidl

import dev.argon.nobleidl.compiler.JavaLanguageOptions
import dev.argon.nobleidl.compiler.api.{java as _, *}

import java.util.Locale
import scala.collection.mutable
import scala.jdk.CollectionConverters.*
import scala.jdk.OptionConverters.*
import scala.language.unsafeNulls
import zio.*
import zio.stream.*
import CodeWriter.Operations.*
import org.apache.commons.text.StringEscapeUtils

private[nobleidl] class ScalaBackend(genRequest: NobleIdlGenerationRequest[JavaLanguageOptions]) {
  import ScalaBackend.*

  private val options: JavaLanguageOptions = genRequest.languageOptions()
  private val model = genRequest.model()
  private val packageMapping = options.packageMapping.packageMapping.map.asScala
    .view
    .map { (k, v) => PackageNameUtil.fromString(k) -> v }
    .toMap

  def emit: Stream[NobleIDLCompileErrorException, GeneratedFile] =
    ZStream.fromIterable(model.definitions().asScala)
      .filterNot(_.isLibrary)
      .flatMap(emitDefinition)



  private def emitDefinition(dfn: DefinitionInfo): Stream[NobleIDLCompileErrorException, GeneratedFile] =
    dfn.definition() match {
      case d: Definition.Record => writeFile(dfn)(emitRecord(dfn, d.r()))
      case d: Definition.Enum => writeFile(dfn)(emitEnum(dfn, d.e()))
      case d: Definition.SimpleEnum => writeFile(dfn)(emitSimpleEnum(dfn, d.e()))
      case _: Definition.ExternType => ZStream()
      case d: Definition.Interface => writeFile(dfn)(emitInterface(dfn, d.iface()))
      case d => throw new MatchError(d)
    }

  private def writeFile(dfn: DefinitionInfo)(data: ZIO[CodeWriter, NobleIDLCompileErrorException, Unit]): Stream[NobleIDLCompileErrorException, GeneratedFile] =
    ZStream.fromZIO(getScalaPackage(dfn.name()._package()))
      .map { pkg =>
        GeneratedFile(
          path = pkg.split("\\.").toSeq :+ (convertIdPascal(dfn.name().name()) + ".scala"),
          content = CodeWriter.withWriter(data)
        )
      }

  private def emitRecord(dfn: DefinitionInfo, r: RecordDefinition): ZIO[CodeWriter, NobleIDLCompileErrorException, Unit] =
    for
      _ <- write("package ")
      _ <- getScalaPackage(dfn.name()._package()).flatMap(writeln)

      _ <- ZIO.foreachDiscard(r.esexprOptions().toScala) { esexprOptions =>
        for
          _ <- write("@_root_.esexpr.constructor(\"")
          _ <- write(StringEscapeUtils.escapeJava(esexprOptions.constructor()))
          _ <- write("\")")
        yield ()
      }
      _ <- write("final case class ")
      _ <- write(convertIdPascal(dfn.name().name()))
      _ <- writeTypeParameters(dfn.typeParameters().asScala.toSeq)
      _ <- writeCaseClassParameters(r.fields().asScala.toSeq)
      _ <- write(" derives _root_.esexpr.ESExprCodec, _root_.scala.CanEqual").when(r.esexprOptions().isPresent)
      _ <- writeln()

    yield ()

  private def emitEnum(dfn: DefinitionInfo, e: EnumDefinition): ZIO[CodeWriter, NobleIDLCompileErrorException, Unit] =
    for
      _ <- write("package ")
      _ <- getScalaPackage(dfn.name()._package()).flatMap(writeln)

      _ <- write("enum ")
      _ <- write(convertIdPascal(dfn.name().name()))
      _ <- writeTypeParameters(dfn.typeParameters().asScala.toSeq)
      _ <- write(" derives _root_.esexpr.ESExprCodec, _root_.scala.CanEqual").when(e.esexprOptions().isPresent)
      _ <- writeln(" {")
      _ <- indent()

      _ <- ZIO.foreachDiscard(e.cases().asScala) { c =>
        for
          _ <- ZIO.foreachDiscard(c.esexprOptions().toScala) { esexprOptions =>
            esexprOptions.caseType() match {
              case constructor: EsexprEnumCaseType.Constructor =>
                for
                  _ <- write("@_root_.esexpr.constructor(\"")
                  _ <- write(StringEscapeUtils.escapeJava(constructor.name()))
                  _ <- write("\")")
                yield ()

              case _: EsexprEnumCaseType.InlineValue =>
                writeln("@_root_.esexpr.inlineValue")

              case caseType => throw new MatchError(caseType)
            }
          }

          _ <- write("case ")
          _ <- write(convertIdPascal(dfn.name().name()))
          _ <- writeCaseClassParameters(c.fields().asScala.toSeq)
          _ <- writeln()
        yield ()
      }

      _ <- dendent()
      _ <- writeln("}")

    yield ()

  private def emitSimpleEnum(dfn: DefinitionInfo, e: SimpleEnumDefinition): ZIO[CodeWriter, NobleIDLCompileErrorException, Unit] =
    for
      _ <- write("package ")
      _ <- getScalaPackage(dfn.name()._package()).flatMap(writeln)

      _ <- writeln("@_root_.esexpr.simple")
      _ <- write("enum ")
      _ <- write(convertIdPascal(dfn.name().name()))
      _ <- writeTypeParameters(dfn.typeParameters().asScala.toSeq)
      _ <- write(" derives _root_.esexpr.ESExprCodec, _root_.scala.CanEqual").when(e.esexprOptions().isPresent)
      _ <- writeln(" {")
      _ <- indent()

      _ <- ZIO.foreachDiscard(e.cases().asScala) { c =>
        for
          _ <- ZIO.foreachDiscard(c.esexprOptions().toScala) { esexprOptions =>
            for
              _ <- write("@_root_.esexpr.constructor(\"")
              _ <- write(StringEscapeUtils.escapeJava(esexprOptions.name()))
              _ <- write("\")")
            yield ()
          }

          _ <- write("case ")
          _ <- write(convertIdPascal(dfn.name().name()))
          _ <- writeln()
        yield ()
      }

      _ <- dendent()
      _ <- writeln("}")

    yield ()

  private def emitInterface(dfn: DefinitionInfo, iface: InterfaceDefinition): ZIO[CodeWriter, NobleIDLCompileErrorException, Unit] =
    for
      _ <- write("package ")
      _ <- getScalaPackage(dfn.name()._package()).flatMap(writeln)

      _ <- write("trait ")
      _ <- write(convertIdPascal(dfn.name().name()))
      _ <- writeTypeParameters(dfn.typeParameters().asScala.toSeq)
      _ <- writeln(" {")
      _ <- indent()

      _ <- ZIO.foreachDiscard(iface.methods().asScala) { m =>
        for
          _ <- write("def ")
          _ <- write(convertIdCamel(m.name))
          _ <- writeTypeParameters(m.typeParameters().asScala.toSeq)
          _ <- write("(")

          _ <- ZIO.foreachDiscard(m.parameters().asScala.view.zipWithIndex) { (param, index) =>
            for
              _ <- write(", ").when(index > 0)
              _ <- write(convertIdCamel(param.name))
              _ <- write(": ")
              _ <- writeTypeExpr(param.parameterType())
            yield ()
          }

          _ <- write(")")
        yield ()
      }

      _ <- dendent()
      _ <- writeln("}")

    yield ()

  private def writeTypeParameters(tps: Seq[TypeParameter]): ZIO[CodeWriter, NobleIDLCompileErrorException, Unit] =
    for
      _ <- writeln("[")

      _ <- ZIO.foreachDiscard(tps.zipWithIndex) {
        case (tp: TypeParameter.Type, index) =>
          for
            _ <- write(", ").when(index > 0)
            _ <- write(convertIdPascal(tp.name()))
          yield ()

        case tpi => throw new MatchError(tpi)
      }

      _ <- write("]")
    yield ()

  private def writeCaseClassParameters(fields: Seq[RecordField]): ZIO[CodeWriter, NobleIDLCompileErrorException, Unit] =
    for
      _ <- writeln("(")
      _ <- indent()

      _ <- ZIO.foreachDiscard(fields) { field =>
        for
          _ <- ZIO.foreachDiscard(field.esexprOptions().toScala) { esexprOptions =>
            esexprOptions.kind match {
              case positional: EsexprRecordFieldKind.Positional =>
                positional.mode() match {
                  case _: EsexprRecordPositionalMode.Required => ZIO.unit
                  case _: EsexprRecordPositionalMode.Optional => writeln("@_root_.esexpr.optional")
                  case mode => throw new MatchError(mode)
                }


              case keyword: EsexprRecordFieldKind.Keyword =>
                for
                  _ <- write("@_root_.esexpr.keyword(\"")
                  _ <- write(StringEscapeUtils.escapeJava(keyword.name()))
                  _ <- writeln("\")")

                  _ <- keyword.mode() match {
                    case _: EsexprRecordKeywordMode.Required => ZIO.unit

                    case _: EsexprRecordKeywordMode.Optional =>
                      write("@_root_.esexpr.optional")
                    case _: EsexprRecordKeywordMode.DefaultValue => ZIO.unit
                    case mode => throw new MatchError(mode)
                  }
                yield ()

              case _: EsexprRecordFieldKind.Dict =>
                writeln("@_root_.esexpr.dict")

              case _: EsexprRecordFieldKind.Vararg =>
                writeln("@_root_.esexpr.vararg")

              case kind => throw new MatchError(kind)
            }
          }

          _ <- write(convertIdCamel(field.name))
          _ <- write(": ")
          _ <- writeTypeExpr(field.fieldType)
          _ <- ZIO.foreachDiscard(field.esexprOptions().toScala) { esexprOptions =>
            esexprOptions.kind match {
              case keyword: EsexprRecordFieldKind.Keyword =>
                keyword.mode() match {
                  case defaultValue: EsexprRecordKeywordMode.DefaultValue =>
                    write(" = ") *> writeDecodedValue(defaultValue.value())

                  case _ => ZIO.unit
                }

              case _ => ZIO.unit
            }
          }
        yield ()
      }

      _ <- dendent()
      _ <- write(")")
    yield ()

  private def writeDecodedValue(value: EsexprDecodedValue): ZIO[CodeWriter, NobleIDLCompileErrorException, Unit] =
    value match {
      case value: EsexprDecodedValue.FromBool =>
        for
          _ <- writeTypeExpr(value.t())
          _ <- write("(")
          _ <- write(value.b().toString)
          _ <- write(")")
        yield ()

      case _ => ???
    }

  private def writeTypeExpr(expr: TypeExpr): ZIO[CodeWriter, NobleIDLCompileErrorException, Unit] =
    expr match {
      case definedType: TypeExpr.DefinedType =>
        for
          _ <- write("_root_.")
          _ <- getScalaPackage(definedType.name()._package()).flatMap(write)
          _ <- write(".")
          _ <- write(convertIdPascal(definedType.name().name()))
          _ <- (
            for
              _ <- write("[")
              _ <- ZIO.foreachDiscard(definedType.args().asScala.view.zipWithIndex) { (arg, index) =>
                write(", ").when(index > 0) *>
                  writeTypeExpr(arg)
              }
              _ <- write("]")
            yield ()
          ).when(definedType.args().asScala.nonEmpty)
        yield ()

      case parameter: TypeExpr.TypeParameter =>
        write(convertIdPascal(parameter.name))

      case _ => throw new MatchError(expr)
    }


  private def getScalaPackage(packageName: PackageName): IO[NobleIDLCompileErrorException, String] =
    ZIO.fromEither(
      packageMapping.get(packageName)
        .toRight { NobleIDLCompileErrorException("Unmapped package: " + PackageNameUtil.display(packageName)) }
    )

  private def convertIdPascal(kebab: String): String =
    kebab.split("-")
      .map { segment => segment.substring(0, 1).toUpperCase(Locale.ROOT) + segment.substring(1) }
      .mkString

  private def convertIdCamel(kebab: String): String =
    val pascal = convertIdPascal(kebab)
    var camel = pascal.substring(0, 1).toLowerCase(Locale.ROOT) + pascal.substring(1)

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

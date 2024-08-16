package nobleidl.compiler

import nobleidl.compiler.CodeWriter.Operations.*
import nobleidl.compiler.api.*
import nobleidl.compiler.format.PackageMapping
import org.apache.commons.text.StringEscapeUtils
import zio.*
import zio.stream.*

import java.util.Locale
import scala.jdk.CollectionConverters.*

private[compiler] class ScalaJSBackend(genRequest: NobleIdlGenerationRequest[ScalaLanguageOptions]) extends ScalaBackendBase {
  import ScalaBackendBase.*

  private val options: ScalaLanguageOptions = genRequest.languageOptions

  protected override def model: NobleIdlModel = genRequest.model
  protected override def packageMappingRaw: PackageMapping = options.packageMapping

  protected override def emitRecord(dfn: DefinitionInfo, r: RecordDefinition): ZIO[CodeWriter, NobleIDLCompileErrorException, Unit] =
    for
      _ <- write("package ")
      _ <- getScalaPackage(dfn.name.`package`).flatMap(writeln)

      _ <- write("trait ")
      _ <- write(convertIdPascal(dfn.name.name))
      _ <- writeTypeParameters(dfn.typeParameters)
      _ <- writeln(" extends _root_.scala.scalajs.js.Object {")
      _ <- indent()
      _ <- writeFields(r.fields)
      _ <- dedent()
      _ <- writeln("}")

    yield ()

  protected override def emitEnum(dfn: DefinitionInfo, e: EnumDefinition): ZIO[CodeWriter, NobleIDLCompileErrorException, Unit] =
    for
      _ <- write("package ")
      _ <- getScalaPackage(dfn.name.`package`).flatMap(writeln)

      _ <- write("sealed trait ")
      _ <- write(convertIdPascal(dfn.name.name))
      _ <- writeTypeParameters(dfn.typeParameters)
      
      _ <- write("object ")
      _ <- write(convertIdPascal(dfn.name.name))
      _ <- writeln(" {")
      _ <- indent()
      
      _ <- ZIO.foreachDiscard(e.cases) { c =>
        for
          _ <- write("trait ")
          _ <- write(convertIdPascal(c.name))
          _ <- write(" extends _root_.")
          _ <- getScalaPackage(dfn.name.`package`).flatMap(write)
          _ <- write(".")
          _ <- write(convertIdPascal(dfn.name.name))
          _ <- writeTypeParametersAsArguments(dfn.typeParameters)
          _ <- writeln(" {")
          _ <- indent()
          _ <- writeFields(c.fields)
          _ <- dedent()
          _ <- writeln("}")
        yield ()
      }
      
      _ <- dedent()
      _ <- writeln("}")

    yield ()

  protected override def emitSimpleEnum(dfn: DefinitionInfo, e: SimpleEnumDefinition): ZIO[CodeWriter, NobleIDLCompileErrorException, Unit] =
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

          _ <- write(")")
        yield ()
      }

      _ <- dedent()
      _ <- writeln("}")

    yield ()

  private def writeFields(fields: Seq[RecordField]): ZIO[CodeWriter, NobleIDLCompileErrorException, Unit] =
    ZIO.foreachDiscard(fields) { field =>
      for
        _ <- write("val ")
        _ <- write(convertIdCamel(field.name))
        _ <- write(": ")
        _ <- writeTypeExpr(field.fieldType)
      yield ()
    }
}

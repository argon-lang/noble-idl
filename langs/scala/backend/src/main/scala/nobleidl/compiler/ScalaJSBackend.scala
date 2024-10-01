package nobleidl.compiler

import nobleidl.compiler.CodeWriter.Operations.*
import nobleidl.compiler.api.{java as _, *}
import nobleidl.compiler.PackageMapping
import org.apache.commons.text.StringEscapeUtils
import zio.*
import zio.stream.*

import java.nio.file.Path
import java.util.Locale
import scala.jdk.CollectionConverters.*
import dev.argon.nobleidl.compiler.NobleIDLCompileErrorException

final class ScalaJSBackend(genRequest: NobleIdlGenerationRequest[ScalaJSLanguageOptions]) extends ScalaBackendBase {
  import ScalaBackendBase.*

  private val options: ScalaJSLanguageOptions = genRequest.languageOptions

  protected override def model: NobleIdlModel = genRequest.model
  protected override def packageMappingRaw: PackageMapping = options.packageMapping

  private lazy val packageImportMapping =
    buildPackageMapping(options.packageImportMapping.mapping)

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
      _ <- writeln(" {")
      _ <- indent()
      _ <- writeln("val $type: String")
      _ <- dedent()
      _ <- writeln("}")

      _ <- write("object ")
      _ <- write(convertIdPascal(dfn.name.name))
      _ <- writeln(" {")
      _ <- indent()

      dfnType = definitionAsType(dfn)

      _ <- ZIO.foreachDiscard(e.cases) { c =>
        for
          _ <- write("trait ")
          _ <- write(convertIdPascal(c.name))
          _ <- writeTypeParameters(dfn.typeParameters)
          _ <- write(" extends ")
          _ <- writeTypeExpr(dfnType)
          _ <- writeln(" {")
          _ <- indent()
          _ <- write("val $type: \"")
          _ <- write(StringEscapeUtils.escapeJava(c.name).nn)
          _ <- writeln("\"")
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
      _ <- write("type ")
      _ <- write(convertIdPascal(dfn.name.name))
      _ <- writeln(" =")
      _ <- indent()

      _ <- ZIO.foreachDiscard(e.cases.view.zipWithIndex) { (c, i) =>
        for
          _ <- write("| ").when(i > 0)
          _ <- write("\"")
          _ <- write(StringEscapeUtils.escapeJava(c.name).nn)
          _ <- writeln("\"")
        yield ()
      }

      _ <- dedent()

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

          _ <- write("): _root_.scala.scalajs.js.Promise[")
          _ <- writeTypeExpr(m.returnType)
          _ <- writeln("]")
        yield ()
      }

      _ <- dedent()
      _ <- writeln("}")

    yield ()

  protected override def emitExceptionType(dfn: DefinitionInfo, ex: ExceptionTypeDefinition): ZIO[CodeWriter, NobleIDLCompileErrorException, Unit] =
    for
      _ <- write("package ")
      _ <- getScalaPackage(dfn.name.`package`).flatMap(writeln)

      _ <- writeln("@_root_.scala.scalajs.js.native")
      _ <- write("@_root_.scala.scalajs.js.annotation.JSImport(\"")
      importPath <- getPackageImport(dfn.name.`package`)
      _ <- write(StringEscapeUtils.escapeJava(importPath).nn)
      _ <- writeln("\")")
      _ <- write("class ")
      _ <- write(convertIdPascal(dfn.name.name))
      _ <- writeln("(")
      _ <- indent()
      _ <- write("val information: ")
      _ <- writeTypeExpr(ex.information)
      _ <- writeln(",")
      _ <- writeln("message: _root_.scala.scalajs.js.UndefOr[_root_.java.lang.String] = _root_.scala.scalajs.js.undefined,")
      _ <- writeln("cause: _root_.scala.scalajs.js.UndefOr[_root_.nobleidl.sjs.core.ErrorOptions] = _root_.scala.scalajs.js.undefined,")
      _ <- dedent()
      _ <- writeln(") extends _root_.scala.scalajs.js.Error()")
    yield ()

  private def getPackageImport(packageName: PackageName): ZIO[CodeWriter, NobleIDLCompileErrorException, String] =
    ZIO.fromEither(packageImportMapping.get(packageName).toRight { NobleIDLCompileErrorException("Unmapped package JS import: " + packageName.display) })

  private def writeFields(fields: Seq[RecordField]): ZIO[CodeWriter, NobleIDLCompileErrorException, Unit] =
    ZIO.foreachDiscard(fields) { field =>
      for
        _ <- write("val ")
        _ <- write(convertIdCamel(field.name))
        _ <- write(": ")
        _ <- writeTypeExpr(field.fieldType)
        _ <- writeln()
      yield ()
    }
}

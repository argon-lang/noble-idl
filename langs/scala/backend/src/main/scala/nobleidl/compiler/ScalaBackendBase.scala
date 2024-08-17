package nobleidl.compiler

import nobleidl.compiler
import nobleidl.compiler.CodeWriter.Operations.*
import nobleidl.compiler.api.{java as _, *}
import nobleidl.compiler.format.PackageMapping
import org.apache.commons.text.StringEscapeUtils
import zio.*
import zio.stream.*

import java.nio.file.Path
import java.util.Locale


private[compiler] abstract class ScalaBackendBase {
  import ScalaBackendBase.*

  protected def model: NobleIdlModel
  protected def packageMappingRaw: PackageMapping
  protected def outputDir: Path

  protected final lazy val packageMapping = buildPackageMapping(packageMappingRaw.mapping.dict)

  final def emit: Stream[NobleIDLCompileErrorException, GeneratedFile] =
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
      case Definition.ExceptionType(ex) => writeFile(dfn)(emitExceptionType(dfn, ex))
    }

  private def writeFile(dfn: DefinitionInfo)(data: ZIO[CodeWriter, NobleIDLCompileErrorException, Unit]): Stream[NobleIDLCompileErrorException, GeneratedFile] =
    ZStream.fromZIO(getScalaPackage(dfn.name.`package`))
      .map { pkg =>
        GeneratedFile(
          path = pkg.split("\\.").nn.view.map(_.nn).foldLeft(outputDir)(_.resolve(_).nn).resolve(convertIdPascal(dfn.name.name) + ".scala").nn,
          content = CodeWriter.withWriter(data)
        )
      }

  protected def emitRecord(dfn: DefinitionInfo, r: RecordDefinition): ZIO[CodeWriter, NobleIDLCompileErrorException, Unit]
  protected def emitEnum(dfn: DefinitionInfo, e: EnumDefinition): ZIO[CodeWriter, NobleIDLCompileErrorException, Unit]
  protected def emitSimpleEnum(dfn: DefinitionInfo, e: SimpleEnumDefinition): ZIO[CodeWriter, NobleIDLCompileErrorException, Unit]
  protected def emitInterface(dfn: DefinitionInfo, iface: InterfaceDefinition): ZIO[CodeWriter, NobleIDLCompileErrorException, Unit]
  protected def emitExceptionType(dfn: DefinitionInfo, ex: ExceptionTypeDefinition): ZIO[CodeWriter, NobleIDLCompileErrorException, Unit]


  protected final def writeTypeParameters(tps: Seq[TypeParameter]): ZIO[CodeWriter, NobleIDLCompileErrorException, Unit] =
    (
      for
        _ <- write("[")

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

  protected def definitionAsType(dfn: DefinitionInfo): TypeExpr.DefinedType =
    TypeExpr.DefinedType(
      dfn.name,
      dfn.typeParameters.map {
        case tp: TypeParameter.Type =>
          TypeExpr.TypeParameter(tp.name, TypeParameterOwner.ByType)
      },
    )


  protected enum TypePosition derives CanEqual {
    case Normal
    case ReturnType
    case TypeArgument
  }

  protected open class TypeExprWriter {
    def writeTypeExpr(expr: TypeExpr, pos: TypePosition): ZIO[CodeWriter, NobleIDLCompileErrorException, Unit] =
      expr match {
        case definedType: TypeExpr.DefinedType =>
          writeDefinedType(definedType, pos)

        case parameter: TypeExpr.TypeParameter =>
          writeTypeParameter(parameter, pos)
      }

    def writePackageName(packageName: PackageName): ZIO[CodeWriter, NobleIDLCompileErrorException, Unit] =
      getScalaPackage(packageName).flatMap(write)

    def writeDefinedTypeName(name: QualifiedName): ZIO[CodeWriter, NobleIDLCompileErrorException, Unit] =
      for
        _ <- write("_root_.")
        _ <- writePackageName(name.`package`)
        _ <- write(".")
        _ <- write(convertIdPascal(name.name))
      yield ()

    def writeTypeArguments(args: Seq[TypeExpr]): ZIO[CodeWriter, NobleIDLCompileErrorException, Unit] =
      (
        for
          _ <- write("[")
          _ <- ZIO.foreachDiscard(args.view.zipWithIndex) { (arg, index) =>
            write(", ").when(index > 0) *>
              writeTypeExpr(arg, TypePosition.TypeArgument)
          }
          _ <- write("]")
        yield ()
        ).whenDiscard(args.nonEmpty)

    def writeDefinedType(definedType: TypeExpr.DefinedType, pos: TypePosition): ZIO[CodeWriter, NobleIDLCompileErrorException, Unit] =
      for
        _ <- writeDefinedTypeName(definedType.name)
        _ <- writeTypeArguments(definedType.args)
      yield ()

    def writeDefinedTypeCase(definedType: TypeExpr.DefinedType, caseName: String, pos: TypePosition): ZIO[CodeWriter, NobleIDLCompileErrorException, Unit] =
      for
        _ <- writeDefinedTypeName(definedType.name)
        _ <- write(".")
        _ <- write(convertIdPascal(caseName))
        _ <- writeTypeArguments(definedType.args)
      yield ()

    def writeTypeParameter(parameter: TypeExpr.TypeParameter, pos: TypePosition): ZIO[CodeWriter, NobleIDLCompileErrorException, Unit] =
      write(convertIdPascal(parameter.name))
  }

  protected object ScalaTypeExprWriter extends TypeExprWriter

  protected final def writeTypeExpr(expr: TypeExpr): ZIO[CodeWriter, NobleIDLCompileErrorException, Unit] =
    ScalaTypeExprWriter.writeTypeExpr(expr, TypePosition.Normal)

  protected final def getScalaPackage(packageName: PackageName): IO[NobleIDLCompileErrorException, String] =
    ZIO.fromEither(
      packageMapping.get(packageName)
        .toRight { NobleIDLCompileErrorException("Unmapped package: " + packageName.display) }
    )

  protected final def convertIdPascal(kebab: String): String =
    kebab.split("-")
      .nn
      .view
      .map(_.nn)
      .map { segment => segment.substring(0, 1).nn.toUpperCase(Locale.ROOT).nn + segment.substring(1).nn }
      .mkString

  protected final def convertIdCamel(kebab: String): String =
    val pascal = convertIdPascal(kebab)
    val camel = pascal.substring(0, 1).nn.toLowerCase(Locale.ROOT).nn + pascal.substring(1).nn

    if keywords.contains(camel) then
      s"`$camel`"
    else
      camel
  end convertIdCamel

  protected final def convertIdConst(kebab: String): String =
    kebab.replace("-", "_").nn.toUpperCase(Locale.ROOT).nn

}

private[compiler] object ScalaBackendBase {

  final case class GeneratedFile(path: Path, content: Stream[NobleIDLCompileErrorException, String])

  val keywords = Seq(
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

  def buildPackageMapping[V](mapping: Map[String, V]): Map[PackageName, V] =
    mapping
      .view
      .map { (k, v) => PackageName.fromString(k) -> v }
      .toMap

}

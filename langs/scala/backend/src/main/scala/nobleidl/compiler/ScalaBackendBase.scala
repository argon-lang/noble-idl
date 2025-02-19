package nobleidl.compiler

import nobleidl.compiler
import nobleidl.compiler.CodeWriter.Operations.*
import nobleidl.compiler.api.{java as _, *}
import nobleidl.compiler.PackageMapping
import org.apache.commons.text.StringEscapeUtils
import zio.*
import zio.stream.*

import dev.argon.nobleidl.compiler.NobleIDLCompileErrorException

import java.nio.file.Path
import java.util.Locale


abstract class ScalaBackendBase private[compiler] extends Backend {
  import ScalaBackendBase.*
  import Backend.*

  protected def model: NobleIdlModel
  protected def packageMappingRaw: PackageMapping

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
          path = pkg.split("\\.").nn.view.map(_.nn).foldLeft(Path.of("."))(_.resolve(_).nn).resolve(convertIdPascal(dfn.name.name) + ".scala").nn,
          content = CodeWriter.withWriter(data)
        )
      }

  protected def emitRecord(dfn: DefinitionInfo, r: RecordDefinition): ZIO[CodeWriter, NobleIDLCompileErrorException, Unit]
  protected def emitEnum(dfn: DefinitionInfo, e: EnumDefinition): ZIO[CodeWriter, NobleIDLCompileErrorException, Unit]
  protected def emitSimpleEnum(dfn: DefinitionInfo, e: SimpleEnumDefinition): ZIO[CodeWriter, NobleIDLCompileErrorException, Unit]
  protected def emitInterface(dfn: DefinitionInfo, iface: InterfaceDefinition): ZIO[CodeWriter, NobleIDLCompileErrorException, Unit]
  protected def emitExceptionType(dfn: DefinitionInfo, ex: ExceptionTypeDefinition): ZIO[CodeWriter, NobleIDLCompileErrorException, Unit]

  
  protected enum ConstraintType derives CanEqual {
    case ScalaType
    case ScalaJSType
    case ScalaMethod
    case JavaMethod
    case JavaTypeErrorParam
    case ScalaJSMethod
  }

  protected final def writeTypeParameters(tps: Seq[TypeParameter], prefix: String = "", constraintType: ConstraintType = ConstraintType.ScalaType): ZIO[CodeWriter, NobleIDLCompileErrorException, Unit] =
    (
      for
        _ <- write("[")

        _ <- ZIO.foreachDiscard(tps.zipWithIndex) {
          case (tp: TypeParameter.Type, index) =>
            write(", ").whenDiscard(index > 0) *>
              writeTypeParameter(tp, prefix, constraintType)
        }

        _ <- write("]")
      yield ()
    ).whenDiscard(tps.nonEmpty)
    
  protected final def writeJavaMethodTypeParameters(typeTypeParams: Seq[TypeParameter], methodTypeParams: Seq[TypeParameter]): ZIO[CodeWriter, NobleIDLCompileErrorException, Unit] =
    (
      for
        _ <- write("[")

        _ <- ZIO.foreachDiscard(
          Seq(
            typeTypeParams
              .filter {
                case tpt: TypeParameter.Type =>
                  tpt.constraints.exists { case _: TypeParameterTypeConstraint.Exception => true }
              }
              .map { tp =>
                writeTypeParameter(tp, "E", ConstraintType.JavaTypeErrorParam)
              },

            methodTypeParams
              .map { tp =>
                writeTypeParameter(tp, "T", ConstraintType.JavaMethod)
              }
          ).view.flatten.zipWithIndex
        ) {
          case (action, index) =>
            write(", ").whenDiscard(index > 0) *>
              action
        }

        _ <- write("]")
      yield ()
    ).whenDiscard(
      methodTypeParams.nonEmpty ||
        typeTypeParams
          .exists {
            case tpt: TypeParameter.Type =>
              tpt.constraints.exists { case _: TypeParameterTypeConstraint.Exception => true }
          }
    )

  protected def writeTypeParameter(tp: TypeParameter, prefix: String, constraintType: ConstraintType): ZIO[CodeWriter, NobleIDLCompileErrorException, Unit] =
    tp match {
      case tp: TypeParameter.Type =>
        for
          _ <- write(prefix)
          _ <- write(convertIdPascal(tp.name))
          _ <- constraintType match {
            case ConstraintType.ScalaType => ZIO.unit
            case ConstraintType.ScalaJSType => ZIO.unit
            case ConstraintType.ScalaMethod => write(": _root_.nobleidl.core.ErrorType").whenDiscard(tp.constraints.contains(TypeParameterTypeConstraint.Exception()))
            case ConstraintType.JavaMethod =>
              (
                write(", E") *>
                  write(convertIdPascal(tp.name)) *>
                  write(" <: _root_.java.lang.Throwable")
                ).whenDiscard(tp.constraints.contains(TypeParameterTypeConstraint.Exception()))

            case ConstraintType.JavaTypeErrorParam => write(" <: _root_.java.lang.Throwable")
            case ConstraintType.ScalaJSMethod => ZIO.unit
          }
        yield ()
    }
  
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
            write(", ").whenDiscard(index > 0) *>
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

  protected def getExceptionTypeName(name: QualifiedName): String =
    (name.`package`.parts :+ name.name).mkString(".")


  protected final def convertIdPascal(kebab: String): String =
    kebab.split("-")
      .nn
      .view
      .map(_.nn)
      .map { segment => segment.substring(0, 1).nn.toUpperCase(Locale.ROOT).nn + segment.substring(1).nn }
      .mkString

  protected final def convertIdCamel(kebab: String): String =
    escapeIdentifier(convertIdCamelNoEscape(kebab))
  
  protected final def convertIdCamelNoEscape(kebab: String): String =
    val pascal = convertIdPascal(kebab)
    pascal.substring(0, 1).nn.toLowerCase(Locale.ROOT).nn + pascal.substring(1).nn
  end convertIdCamelNoEscape
  
  protected final def escapeIdentifier(id: String): String =
    if keywords.contains(id) then
      s"`$id`"
    else
      id
    

  protected final def convertIdConst(kebab: String): String =
    kebab.replace("-", "_").nn.toUpperCase(Locale.ROOT).nn

}

private[compiler] object ScalaBackendBase {

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

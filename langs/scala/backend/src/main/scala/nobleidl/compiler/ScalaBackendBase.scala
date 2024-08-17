package nobleidl.compiler

import nobleidl.compiler.CodeWriter.Operations.*
import nobleidl.compiler.api.*
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

  protected final lazy val packageMapping = packageMappingRaw.mapping.dict
    .view
    .map { (k, v) => PackageName.fromString(k) -> v }
    .toMap

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
    
  protected final def writeTypeParametersAsArguments(tps: Seq[TypeParameter]): ZIO[CodeWriter, NobleIDLCompileErrorException, Unit] =
    writeTypeParameters(tps)

  protected final def writeTypeExpr(expr: TypeExpr): ZIO[CodeWriter, NobleIDLCompileErrorException, Unit] =
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
}

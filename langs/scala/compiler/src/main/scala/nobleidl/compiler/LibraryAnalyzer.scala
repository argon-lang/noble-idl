package nobleidl.compiler

import cats.Monoid
import cats.data.OptionT
import dev.argon.util.async.ErrorWrapper
import esexpr.{ESExpr, ESExprBinaryDecoder, ESExprCodec, ESExprFormatException}
import zio.*
import zio.interop.catz.core.given
import zio.stream.*
import org.objectweb.asm.{AnnotationVisitor, ClassReader, ClassVisitor, Opcodes}

import java.io.IOException
import java.nio.file.{FileSystems, Files, Path}
import scala.jdk.CollectionConverters.*
import scala.reflect.TypeTest
import scala.util.Using

object LibraryAnalyzer {

  type Error = IOException | ESExprFormatException | ESExprCodec.DecodeError

  final case class LibraryResults(
    scalaPackageMapping: Map[String, String],
    javaPackageMapping: Map[String, String],
    scalaJSPackageMapping: Map[String, String],
    scalaJSImportMapping: Map[String, String],
    sourceFiles: Seq[String],
  )

  object LibraryResults {
    def empty: LibraryResults =
      LibraryResults(
        scalaPackageMapping = Map.empty,
        javaPackageMapping = Map.empty,
        scalaJSPackageMapping = Map.empty,
        scalaJSImportMapping = Map.empty,
        sourceFiles = Seq.empty,
      )

    def merge(a: LibraryResults, b: LibraryResults): LibraryResults =
      LibraryResults(
        scalaPackageMapping = a.scalaPackageMapping ++ b.scalaPackageMapping,
        javaPackageMapping = a.javaPackageMapping ++ b.javaPackageMapping,
        scalaJSPackageMapping = a.scalaJSPackageMapping ++ b.scalaJSPackageMapping,
        scalaJSImportMapping = a.scalaJSImportMapping ++ b.scalaJSImportMapping,
        sourceFiles = a.sourceFiles ++ b.sourceFiles
      )
  }

  def scan(libraries: Seq[Path], platform: String): IO[Error, LibraryResults] =
    ZStream.fromIterable(libraries)
      .flatMap { lib =>
        ZStream.scoped(libFileSystemRoot(lib))
      }
      .mapZIO(scanLibrary(platform))
      .runFold(LibraryResults.empty)(LibraryResults.merge)

  private val errorContext = ErrorWrapper.Context[Error]
  import errorContext.given
  

  private def scanLibrary(platform: String)(lib: Path): IO[Error, LibraryResults] =
    ZStream.fromJavaStream(Files.walk(lib))
      .refineToOrDie[IOException]
      .filterZIO { file =>
        ZIO.attempt {
          Files.isRegularFile(file) && {
            val fileName: Path = file.getFileName
            fileName != null && fileName.toString == "package-info.class"
          }
        }.refineToOrDie[IOException]
      }
      .mapZIO(scanPackageInfo)
      .runFold(LibraryResults.empty)(LibraryResults.merge)
      .flatMap { libRes =>
        val hasMapping = platform match {
          case "java" => libRes.javaPackageMapping.nonEmpty
          case "scala" => libRes.scalaPackageMapping.nonEmpty
          case "scalajs" => libRes.scalaJSPackageMapping.nonEmpty
          case _ => false
        }

        val nidlDir = lib.resolve("nobleidl")

        if hasMapping then
          ZStream.fromJavaStream(Files.walk(nidlDir))
            .refineToOrDie[IOException]
            .filterZIO { file =>
                ZIO.attempt {
                  Files.isRegularFile(file) && {
                    val fileName = file.getFileName
                    fileName != null && fileName.toString.endsWith(".nidl")
                  }
                }.refineToOrDie[IOException]
              }
            .mapZIO { path =>
              ZIO.attempt {
                Files.readString(path)
              }.refineToOrDie[IOException]
            }
            .runCollect
            .whenZIO {
              ZIO.attempt {
                Files.isDirectory(nidlDir)
              }.refineToOrDie[IOException]
            }
            .map { files =>
              libRes.copy(sourceFiles = files.toSeq.flatten)
            }
        else
          ZIO.succeed(libRes)
      }

  private def scanPackageInfo(file: Path): IO[IOException, LibraryResults] =
    ZIO.attempt {
      Using.resource(Files.newInputStream(file)) { is =>

        val reader = new ClassReader(is)

        var packageName: Option[String] = None
        var javaIdlPackage: Option[String] = None
        var scalaIdlPackage: Option[String] = None
        var sjsIdlPackage: Option[String] = None
        var sjsImport: Option[String] = None

        val visitor = new ClassVisitor(Opcodes.ASM9) {
          override def visit(version: RuntimeFlags, access: RuntimeFlags, name: String, signature: String, superName: String, interfaces: Array[String]): Unit =
            val lastSlashIndex: Int = name.lastIndexOf('/')
            val packageNameSlash =
              if lastSlashIndex >= 0 then
                name.substring(0, lastSlashIndex)
              else
                ""

            packageName = Some(packageNameSlash.replace('/', '.'))
          end visit

          override def visitAnnotation(descriptor: String, visible: Boolean): AnnotationVisitor | Null =
            descriptor match {
              case "Ldev/argon/nobleidl/runtime/NobleIDLPackage;" =>
                new AnnotationVisitor(Opcodes.ASM9) {
                  override def visit(name: String, value: Any): Unit =
                    (name, value.asInstanceOf[Matchable]) match {
                      case ("value", value: String) =>
                        javaIdlPackage = Some(value)

                      case _ =>
                    }
                }

              case "Lnobleidl/core/NobleIDLScalaPackage;" =>
                new AnnotationVisitor(Opcodes.ASM9) {
                  override def visit(name: String, value: Any): Unit =
                    (name, value.asInstanceOf[Matchable]) match {
                      case ("value", value: String) =>
                        scalaIdlPackage = Some(value)

                      case _ =>
                    }
                }
                
              case "Lnobleidl/sjs/core/NobleIDLScalaJSPackage;" =>
                new AnnotationVisitor(Opcodes.ASM9) {
                  override def visit(name: String, value: Any): Unit =
                    (name, value.asInstanceOf[Matchable]) match {
                      case ("value", value: String) =>
                        sjsIdlPackage = Some(value)

                      case _ =>
                    }
                }

              case "Lnobleidl/sjs/core/NobleIDLScalaJSImport;" =>
                new AnnotationVisitor(Opcodes.ASM9) {
                  override def visit(name: String, value: Any): Unit =
                    (name, value.asInstanceOf[Matchable]) match {
                      case ("value", value: String) =>
                        sjsImport = Some(value)

                      case _ =>
                    }
                }
                
              case _ => null
            }
        }

        reader.accept(visitor, ClassReader.SKIP_CODE)

        def makeMap(idlPackage: Option[String]): Map[String, String] =
          (packageName, idlPackage) match {
            case (Some(packageName), Some(idlPackage)) => Map(idlPackage -> packageName)
            case _ => Map()
          }

        LibraryResults(
          scalaPackageMapping = makeMap(scalaIdlPackage),
          javaPackageMapping = makeMap(javaIdlPackage),
          scalaJSPackageMapping = makeMap(sjsIdlPackage),
          scalaJSImportMapping = (sjsIdlPackage, sjsImport) match {
            case (Some(idlPackage), Some(jsImport)) => Map(idlPackage -> jsImport)
            case _ => Map()
          },
          sourceFiles = Seq()
        )
      }
    }.refineToOrDie[IOException]

  private def libFileSystemRoot(lib: Path): ZIO[Scope, IOException, Path] =
    ZIO.ifZIO(ZIO.attempt { Files.isDirectory(lib) })(
      onTrue = ZIO.succeed(lib),
      onFalse =
        if lib.getFileName.toString.endsWith(".jar") then
          ZIO.fromAutoCloseable(
            ZIO.attempt {
              FileSystems.newFileSystem(lib).nn
            }
          )
            .flatMap { zipFS => ZIO.attempt { zipFS.getRootDirectories.nn.iterator.nn.next.nn } }
        else
          ZIO.die(RuntimeException("Unknown library type. Expected directory or jar. Path: " + lib))
    ).refineToOrDie[IOException]
}

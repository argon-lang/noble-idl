package nobleidl.compiler

import nobleidl.compiler.backend.PackageMapping

import esexpr.Dictionary
import javax.annotation.processing.{AbstractProcessor, ProcessingEnvironment, RoundEnvironment, SupportedAnnotationTypes}
import javax.lang.model.SourceVersion
import javax.lang.model.element.*
import scala.collection.mutable
import scala.jdk.CollectionConverters.*

@SupportedAnnotationTypes(Array(
  "nobleidl.core.NobleIDLScalaPackage",
  "dev.argon.nobleidl.runtime.NobleIDLPackage",
  "nobleidl.sjs.core.NobleIDLScalaJSPackage",
  "nobleidl.sjs.core.NobleIDLScalaJSImport",
))
final class PackageMappingScannerProcessor extends AbstractProcessor {
  private final val scalaPackageMapping = mutable.Map[String, String]()
  private final val javaPackageMapping = mutable.Map[String, String]()
  private final val scalaJSPackageMapping = mutable.Map[String, String]()
  private final val scalaJSImportMapping = mutable.Map[String, String]()

  def getLibraryResults: LibraryAnalyzer.LibraryResults =
    LibraryAnalyzer.LibraryResults(
      scalaPackageMapping = scalaPackageMapping.toMap,
      javaPackageMapping = javaPackageMapping.toMap,
      scalaJSPackageMapping = scalaJSPackageMapping.toMap,
      scalaJSImportMapping = scalaJSImportMapping.toMap,
      sourceFiles = Seq.empty,
    )

  override def getSupportedSourceVersion: SourceVersion = SourceVersion.latestSupported

  override def process(annotations: java.util.Set[? <: TypeElement], roundEnv: RoundEnvironment): Boolean = {
    def processMappingFunc(annName: String)(addMapping: (String, String) => Unit): Unit =
      for annotation <- annotations.asScala.view.filter(_.getQualifiedName.toString == annName) do
        val annotatedElements = roundEnv.getElementsAnnotatedWith(annotation)
        
        for
          case element: PackageElement <- annotatedElements.asScala
          mappingAnn <- getAnnotation(element.getAnnotationMirrors.nn.asScala, annName)
          idlPackageValue <- getAnnotationArgument(mappingAnn, "value")
          case idlPackage: String <- Seq(idlPackageValue.getValue)
        do addMapping(idlPackage, element.getQualifiedName.toString)
      end for
      
    def processMapping(annName: String, mapping: mutable.Map[String, String]): Unit =
      processMappingFunc(annName)(mapping.update)

    processMapping("nobleidl.core.NobleIDLScalaPackage", scalaPackageMapping)
    processMapping("dev.argon.nobleidl.runtime.NobleIDLPackage", javaPackageMapping)
    processMapping("nobleidl.sjs.core.NobleIDLScalaJSPackage", scalaJSPackageMapping)

    processMappingFunc("nobleidl.sjs.core.NobleIDLScalaJSImport") { (importPath, pkg) =>
      scalaJSPackageMapping.find(_._2 == pkg).foreach { (idlPackage, _) =>
        scalaJSImportMapping(idlPackage) = importPath
      }
    }

    true
  }

  private def getAnnotation(annotations: Iterable[AnnotationMirror], name: String): Option[AnnotationMirror] =
    annotations.find { ann => ann.getAnnotationType.asElement.asInstanceOf[TypeElement].getQualifiedName.toString == name }

  private def getAnnotationArgument(ann: AnnotationMirror, name: String): Option[AnnotationValue] =
    ann.getElementValues.nn
      .asScala
      .find { (k, _) => k.getSimpleName.toString == name }
      .map { (_, v) => v }
}
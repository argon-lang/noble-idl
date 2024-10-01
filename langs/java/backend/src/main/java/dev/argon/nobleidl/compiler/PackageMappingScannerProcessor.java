package dev.argon.nobleidl.compiler;

import dev.argon.esexpr.KeywordMapping;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.RoundEnvironment;
import javax.annotation.processing.SupportedAnnotationTypes;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.*;
import java.util.*;

@SupportedAnnotationTypes(PackageMappingScannerProcessor.packageAnnName)
final class PackageMappingScannerProcessor extends AbstractProcessor {

	final static String packageAnnName = "dev.argon.nobleidl.runtime.NobleIDLPackage";

	private final Map<String, String> packageMapping = new HashMap<>();

	public PackageMapping getPackageMapping() {
		return new PackageMapping(new KeywordMapping<>(new HashMap<>(packageMapping)));
	}


	@Override
	public SourceVersion getSupportedSourceVersion() {
		return SourceVersion.latestSupported();
	}

	@Override
	public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
		for(TypeElement annotation : annotations) {
			if(!annotation.getQualifiedName().toString().equals(PackageMappingScannerProcessor.packageAnnName)) {
				continue;
			}

			Set<? extends Element> annotatedElements = roundEnv.getElementsAnnotatedWith(annotation);

			for(var element : annotatedElements) {
				if(!(element instanceof PackageElement packageElement)) {
					continue;
				}

				var mappingAnn = getAnnotation(element.getAnnotationMirrors(), PackageMappingScannerProcessor.packageAnnName).orElse(null);
				if(mappingAnn == null) {
					continue;
				}

				var idlPackage = getAnnotationArgument(mappingAnn, "value").orElse(null);
				if(idlPackage == null) {
					continue;
				}

				if(!(idlPackage.getValue() instanceof String idlPackageValue)) {
					continue;
				}

				packageMapping.put(idlPackageValue, packageElement.getQualifiedName().toString());
			}

		}

		return true;
	}


	private static Optional<? extends AnnotationMirror> getAnnotation(List<? extends AnnotationMirror> annotations, String name) {
		return annotations.stream()
			.filter(ann -> ((TypeElement)ann.getAnnotationType().asElement()).getQualifiedName().toString().equals(name))
			.findFirst();
	}

	private static Optional<AnnotationValue> getAnnotationArgument(AnnotationMirror ann, String name) {
		return ann.getElementValues()
			.entrySet()
			.stream()
			.filter(entry -> entry.getKey().getSimpleName().toString().equals(name))
			.findFirst()
			.map(Map.Entry::getValue);
	}
}

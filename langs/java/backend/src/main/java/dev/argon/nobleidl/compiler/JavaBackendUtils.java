package dev.argon.nobleidl.compiler;

import javax.lang.model.SourceVersion;
import java.util.Arrays;
import java.util.Locale;
import java.util.stream.Collectors;

public final class JavaBackendUtils {
	private JavaBackendUtils() {}

	public static String convertIdPascal(String kebab) {
		return Arrays.stream(kebab.split("-"))
			.map(segment -> segment.substring(0, 1).toUpperCase(Locale.ROOT) + segment.substring(1))
			.collect(Collectors.joining());
	}

	public static String convertIdCamel(String kebab) {
		var camel = convertIdCamelNoEscape(kebab);

		if(SourceVersion.isKeyword(camel)) {
			camel = "_" + camel;
		}

		return camel;
	}

	public static String convertIdCamelNoEscape(String kebab) {
		var pascal = convertIdPascal(kebab);
		return pascal.substring(0, 1).toLowerCase(Locale.ROOT) + pascal.substring(1);
	}

	public static String convertIdConst(String kebab) {
		return kebab.replace("-", "_").toUpperCase(Locale.ROOT);
	}
}

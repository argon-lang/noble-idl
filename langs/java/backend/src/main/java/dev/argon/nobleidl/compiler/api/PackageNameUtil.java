package dev.argon.nobleidl.compiler.api;


import java.util.List;

public class PackageNameUtil {
	private PackageNameUtil() {}

	public static PackageName fromString(String name) {
		if(name.isEmpty()) {
			return new PackageName(List.of());
		}
		else {
			return new PackageName(List.of(name.split("\\.")));
		}
	}

	public static String display(PackageName name) {
		return String.join(".", name.parts());
	}
}

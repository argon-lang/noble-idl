package dev.argon.nobleidl.compiler;

import java.io.Closeable;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;

import dev.argon.esexpr.DecodeException;
import dev.argon.esexpr.ESExpr;
import dev.argon.esexpr.ESExprBinaryReader;
import dev.argon.esexpr.SyntaxException;
import org.objectweb.asm.AnnotationVisitor;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.Opcodes;

class LibraryAnalyzer implements Closeable {
	private LibraryAnalyzer(FileSystem fs, Path libraryPath) {
		this.fs = fs;
		this.libraryPath = libraryPath;
	}

	private final FileSystem fs;
	private final Path libraryPath;

	public final Map<String, String> packageMapping = new HashMap<>();
	public final List<String> sourceFiles = new ArrayList<>();

	public static LibraryAnalyzer fromPath(Path libraryPath) throws IOException {
		if(Files.isDirectory(libraryPath)) {
			return new LibraryAnalyzer(null, libraryPath);
		}
		else if(libraryPath.getFileName().toString().endsWith(".jar")) {
			var zipFS = FileSystems.newFileSystem(libraryPath);
			var rootPath = zipFS.getRootDirectories().iterator().next();
			return new LibraryAnalyzer(zipFS, rootPath);
		}
		else {
			throw new RuntimeException("Unknown library type. Expected directory or jar.");
		}
	}



	public void scan() throws IOException {
		boolean libHasMapping = false;
		try(var files = Files.walk(libraryPath)) {
			for(Iterator<Path> it = files.iterator(); it.hasNext(); ) {
				var file = it.next();

				if(!Files.isRegularFile(file)) {
					continue;
				}

				var fileName = file.getFileName();
				if(fileName == null || !fileName.toString().equals("package-info.class")) {
					continue;
				}

				libHasMapping |= scanPackageInfo(file);
			}
		}

		if(!libHasMapping) {
			return;
		}

		var nobleIdlDir = libraryPath.resolve("nobleidl");
		if(Files.isDirectory(nobleIdlDir)) {
			try(var files = Files.walk(nobleIdlDir)) {
				for(Iterator<Path> it = files.iterator(); it.hasNext(); ) {
					var file = it.next();
					
					if(!Files.isRegularFile(file)) {
						continue;
					}
					
					var fileName = file.getFileName();
					if(fileName == null || !file.getFileName().toString().endsWith(".nidl")) {
						continue;
					}

					var content = Files.readString(file, StandardCharsets.UTF_8);
					sourceFiles.add(content);
				}
			}
		}
	}

	private boolean scanPackageInfo(Path file) throws IOException {
		boolean hasMapping = false;
		try(var is = Files.newInputStream(file)) {
			var reader = new ClassReader(is);

			var visitor = new ClassVisitor(Opcodes.ASM9) {
				public String packageName = null;
				public String idlPackage = null;

				@Override
				public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
					String packageNameSlash;
					int lastSlashIndex = name.lastIndexOf('/');
					if(lastSlashIndex >= 0) {
						packageNameSlash = name.substring(0, lastSlashIndex);
					}
					else {
						packageNameSlash = "";
					}

					packageName = packageNameSlash.replace('/', '.');
				}

				@Override
				public AnnotationVisitor visitAnnotation(String descriptor, boolean visible) {
					if(!descriptor.equals("Ldev/argon/nobleidl/runtime/NobleIDLPackage;")) {
						return null;
					}

					return new AnnotationVisitor(Opcodes.ASM9) {
						@Override
						public void visit(String name, Object value) {
							if(name.equals("value") && value instanceof String idlPkg) {
								idlPackage = idlPkg;
							}
						}
					};
				}
			};

			reader.accept(visitor, ClassReader.SKIP_CODE);

			if(visitor.packageName != null && visitor.idlPackage != null) {
				packageMapping.put(visitor.idlPackage, visitor.packageName);
				hasMapping = true;
			}
		}

		return hasMapping;
	}


	@Override
	public void close() throws IOException {
		if(fs != null) {
			fs.close();
		}
	}
}

package dev.argon.nobleidl.gradleplugin;

import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.tasks.InputFiles;
import org.gradle.api.tasks.SourceSetContainer;
import java.io.File;

final class NobleIDLCodeGenPlugin implements Plugin<Project> {
	@Override
	public void apply(Project project) {
		var codeGenTask = project.getTasks().register("generateNobleIDLJava", NobleIDLCodeGenTask.class, task -> {
			task.setGroup("Code Generation");
			task.setDescription("Generate Java sources from NobleIDL");

			var buildDir = project.getLayout().getBuildDirectory().get().getAsFile();
			task.getInputFiles().from(
				project.fileTree(new File(project.getProjectDir(), "src/main/nobleidl"))
					.include("**/*.nidl")
			);
			task.getJavaOutputDir().set(new File(buildDir, "nobleidl/gen/java"));
			task.getResourceOutputDir().set(new File(buildDir, "nobleidl/gen/resources"));
		});

		project.afterEvaluate(_ -> {
			var sourceSets = (SourceSetContainer)project.getExtensions().getByName("sourceSets");
			sourceSets.named("main", sourceSet -> {
				sourceSet.getJava().srcDir(codeGenTask.get().getJavaOutputDir());
				sourceSet.getResources().srcDir(codeGenTask.get().getResourceOutputDir());
			});
		});

		project.getTasks().named("compileJava", task -> {
			task.dependsOn(codeGenTask);
		});
	}
}
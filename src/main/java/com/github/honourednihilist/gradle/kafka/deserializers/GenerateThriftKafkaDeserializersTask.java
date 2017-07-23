package com.github.honourednihilist.gradle.kafka.deserializers;

import com.github.javaparser.JavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.PackageDeclaration;
import com.github.javaparser.ast.expr.SimpleName;
import com.github.javaparser.ast.type.ClassOrInterfaceType;

import org.gradle.api.DefaultTask;
import org.gradle.api.Task;
import org.gradle.api.file.FileCollection;
import org.gradle.api.plugins.JavaPlugin;
import org.gradle.api.plugins.JavaPluginConvention;
import org.gradle.api.tasks.InputFiles;
import org.gradle.api.tasks.OutputDirectory;
import org.gradle.api.tasks.SourceSet;
import org.gradle.api.tasks.TaskAction;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.StandardOpenOption;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import lombok.Data;

public class GenerateThriftKafkaDeserializersTask extends DefaultTask {

	static final String NAME = "generateThriftKafkaDeserializers";

	private static final String CLASS_NAME_TEMPLATE = "%%THRIFT_TYPE_NAME%%Thrift%%THRIFT_PROTOCOL%%ToJsonDeserializer";
	private static final String CLASS_CODE_TEMPLATE = ResourceUtils.getResourceAsString("ThriftToJsonDeserializer.template");

	private FileCollection sourceItems;

	private Optional<String> targetPackage;

	private final File outputDir;

	public GenerateThriftKafkaDeserializersTask() {
		super();
		setGroup("Code generation");
		setDescription("Generates kafka deserializers for thrift classes");

		outputDir = new File(getProject().getBuildDir().getAbsolutePath() +
				File.separatorChar + "generated-sources" +
				File.separatorChar + "thrift-kafka-deserializers");

		if (getProject().getPlugins().hasPlugin(JavaPlugin.class)) {
			assignJavaPluginDependencies();
		} else {
			getProject().getPlugins().whenPluginAdded(plugin -> {
				if (plugin instanceof JavaPlugin) {
					assignJavaPluginDependencies();
				}
			});
		}
	}

	private void assignJavaPluginDependencies() {
		Task compileJava = getProject().getTasks().getByName(JavaPlugin.COMPILE_JAVA_TASK_NAME);

		if (compileJava == null) return;

		compileJava.dependsOn(this);

		JavaPluginConvention javaPlugin = getProject().getConvention().getPlugin(JavaPluginConvention.class);
		javaPlugin.getSourceSets().getByName(SourceSet.MAIN_SOURCE_SET_NAME).getJava().srcDir(outputDir);
	}

	public void updateProperties() {
		KafkaDeserializersExtension extension = getProject().getExtensions().getByType(KafkaDeserializersExtension.class);
		sourceItems = extension.getSourceItems();
		targetPackage = Optional.ofNullable(extension.getTargetPackage());
	}

	@InputFiles
	public FileCollection getSourceItems() {
		return sourceItems;
	}

	@OutputDirectory
	public File getOutputDir() {
		return outputDir;
	}

	@TaskAction
	public void action() throws IOException {
		updateProperties();

		List<SourceClassInfo> sourceThriftClasses = parseSourceItems(sourceItems);

		List<GeneratedClassInfo> generated = sourceThriftClasses.stream()
				.flatMap(source -> Stream.of("Binary", "Compact")
						.map(protocol -> {
							GeneratedClassInfo ret = new GeneratedClassInfo();
							ret.setTargetPackage(targetPackage.orElse(source.getPackageName()));
							ret.setThriftTypeName(source.getClassName());
							ret.setThriftTypeFullName(source.getPackageName() + "." + source.getClassName());
							ret.setThriftProtocol(protocol);
							ret.setGeneratedName(renderTemplate(CLASS_NAME_TEMPLATE, ret));
							ret.setGeneratedCode(renderTemplate(CLASS_CODE_TEMPLATE, ret));
							return ret;
						}))
				.collect(Collectors.toList());

		removeDir(outputDir);

		for (GeneratedClassInfo info : generated) {
			File file = new File(outputDir.getAbsolutePath() + File.separatorChar +
					info.getTargetPackage().replaceAll("\\.", File.separator) + File.separatorChar +
					info.getGeneratedName() + ".java");
			file.getParentFile().mkdirs();
			Files.write(file.toPath(), info.getGeneratedCode().getBytes(), StandardOpenOption.CREATE,
					StandardOpenOption.WRITE, StandardOpenOption.TRUNCATE_EXISTING);
		}
	}

	private static List<SourceClassInfo> parseSourceItems(FileCollection sourceItems) {
		List<SourceClassInfo> ret = new ArrayList<>();

		sourceItems.getAsFileTree()
				.filter(File::exists)
				.filter(File::isFile)
				.filter(f -> f.getName().endsWith(".java"))
				.forEach(file -> {
					try {
						SourceClassInfo info = new SourceClassInfo();
						info.setSourceFile(file);
						info.setCompilationUnit(JavaParser.parse(file));
						info.setPackageName(info.getCompilationUnit()
								.getPackageDeclaration()
								.map(PackageDeclaration::getNameAsString)
								.orElse(null));
						info.setClassName(info.getSourceFile().getName().replaceAll(".java", ""));

						boolean isThriftClass = info.getCompilationUnit()
								.getClassByName(info.getClassName())
								.map(declaration -> declaration.getImplementedTypes()
										.stream()
										.map(ClassOrInterfaceType::getName)
										.map(SimpleName::getIdentifier)
										.anyMatch(name -> name.equals("TBase")))
								.orElse(false);

						if (isThriftClass) {
							ret.add(info);
						}
					} catch (FileNotFoundException ignored) {}
				});

		return ret;
	}

	private static String renderTemplate(String template, GeneratedClassInfo info) {
		return template.replaceAll("%%TARGET_PACKAGE%%", info.getTargetPackage())
				.replaceAll("%%THRIFT_TYPE_FULL_NAME%%", info.getThriftTypeFullName())
				.replaceAll("%%THRIFT_TYPE_NAME%%", info.getThriftTypeName())
				.replaceAll("%%THRIFT_PROTOCOL%%", info.getThriftProtocol());
	}

	private static void removeDir(File dir) throws IOException {
		Files.walkFileTree(dir.toPath(), new SimpleFileVisitor<Path>() {
			@Override
			public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
				FileVisitResult visitResult = super.visitFile(file, attrs);
				Files.delete(file);
				return visitResult;
			}

			@Override
			public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
				FileVisitResult visitResult = super.postVisitDirectory(dir, exc);
				Files.delete(dir);
				return visitResult;
			}
		});
	}

	@Data
	private static class SourceClassInfo {
		private File sourceFile;
		private CompilationUnit compilationUnit;
		private String packageName;
		private String className;
	}

	@Data
	private static class GeneratedClassInfo {
		private String targetPackage;
		private String thriftTypeName;
		private String thriftTypeFullName;
		private String thriftProtocol;
		private String generatedCode;
		private String generatedName;
	}
}

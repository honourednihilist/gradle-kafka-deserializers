package com.github.honourednihilist.gradle.kafka.deserializers;

import org.gradle.api.Plugin;
import org.gradle.api.Project;

public class GradleKafkaDeserializersPlugin implements Plugin<Project> {

	@Override
	public void apply(Project project) {
		project.getExtensions().create(KafkaDeserializersExtension.NAME, KafkaDeserializersExtension.class);
		GenerateThriftKafkaDeserializersTask task = project.getTasks().create(GenerateThriftKafkaDeserializersTask.NAME, GenerateThriftKafkaDeserializersTask.class);

		project.afterEvaluate(pro -> task.updateProperties());
	}
}

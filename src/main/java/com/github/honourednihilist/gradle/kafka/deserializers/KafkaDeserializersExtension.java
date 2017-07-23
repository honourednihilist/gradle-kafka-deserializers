package com.github.honourednihilist.gradle.kafka.deserializers;

import org.gradle.api.file.FileCollection;

import lombok.Data;

@Data
public class KafkaDeserializersExtension {
	static final String NAME = "kafkaDeserializers";

	private FileCollection sourceItems;

	private String targetPackage;
}

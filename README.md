# gradle-kafka-deserializers

Gradle plugin to generate kafka deserializers for thrift classes.

## Motivation

Let's say you use [Apache Kafka](https://kafka.apache.org) and you pick [Apache Thrift](http://thrift.apache.org) as a data format for messages.
What if you want to take a look at a couple of messages or dump them in a human readable format just using command line interface.
This plugin will help. It generates deserializers from compact and binary thrift protocols to JSON. So you can use these deserializers with Kafka console consumer.  

## Installation

Build script snippet for use in all Gradle versions:
```groovy
buildscript {
  repositories {
    maven {
      url "https://plugins.gradle.org/m2/"
    }
  }
  dependencies {
    classpath "gradle.plugin.com.github.honourednihilist:gradle-kafka-deserializers:0.1.0"
  }
}

apply plugin: "com.github.honourednihilist.gradle-kafka-deserializers"
```

Build script snippet for new, incubating, plugin mechanism introduced in Gradle 2.1:
```groovy
plugins {
    id "com.github.honourednihilist.gradle-kafka-deserializers" version "0.1.0"
}
```

## Usage

The gradle-kafka-deserializers plugin adds only one task - _generateThriftKafkaDeserializers_. This task generates kafka deserializers for thrift classes. 
Here is an example of how the plugin is used in a 'real-world' project - [gradle-kafka-deserializers-example](https://github.com/honourednihilist/gradle-kafka-deserializers-example).


### Configuration

These are all possible configuration options and its default values:

```groovy
kafkaDeserializers {
    sourceItems = compileThriftTask.getOutputs().files
    targetPackage = "com.github.honourednihilist.gradle.kafka.deserializers.example.gen"
}
```

_sourceItems_ are a set of sources (FileCollection), which will be used for generating deserializers for thrift classes. 
In case a source is a directory, the directory will be scanned recursively for thrift classes.

_targetPackage_ will be applied for generated classes. If it is not specified, a deserializer will have the same package as the corresponding thrift class.

import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

buildscript {
    dependencies {
        classpath("org.openapitools:openapi-generator-gradle-plugin:6.2.1")
    }
}

plugins {
    kotlin("jvm") version "1.8.0"
    application
    id ("org.openapi.generator") version "6.2.1"
    id ("org.springframework.boot") version "3.0.2"
    id ("io.spring.dependency-management") version "1.1.0"
    id ("org.jetbrains.kotlin.plugin.noarg") version "1.8.0"
    id ("org.jetbrains.kotlin.plugin.allopen") version "1.8.0"
    id ("org.jetbrains.kotlin.plugin.spring") version "1.8.0"
    id ("org.jetbrains.kotlin.plugin.jpa") version "1.8.0"
}

apply(plugin = "java")
apply(plugin = "idea")
apply(plugin = "org.jetbrains.kotlin.plugin.noarg")

group = "org.example"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    implementation("com.fasterxml.jackson.dataformat:jackson-dataformat-csv:2.13.4")
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.boot:spring-boot-starter-data-jpa")
    implementation("org.springframework.kafka:spring-kafka")
    implementation("org.apache.avro:avro-tools:1.11.1")
    implementation("org.apache.avro:avro:1.11.1")

    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation(kotlin("test"))
}

tasks.test {
    useJUnitPlatform()
}

tasks.withType<KotlinCompile> {
    kotlinOptions.jvmTarget = "19"
}

application {
    mainClass.set("MainKt")
}

openApiGenerate {
    generatorName.set("kotlin-spring")
    inputSpec.set("${projectDir}/src/main/resources/spec/ce-pii-v3.yaml")
    outputDir.set("${buildDir}/generated")
    apiPackage.set("org.openapi.example.api")
    modelPackage.set("org.openapi.example.model")
}

tasks.register("generateAvroClasses", JavaExec::class) {
    main = "org.apache.avro.tool.Main"
//    classpath = configurations.avro
    classpath = configurations.compileClasspath.get() + configurations.runtimeClasspath.get()
    args = listOf("compile", "schema", "${projectDir}/src/main/resources/avro/customeronboardingsnapshot.avsc", "${buildDir}/avro/generated")
}




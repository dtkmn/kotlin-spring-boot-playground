import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

buildscript {
    dependencies {
        classpath("org.openapitools:openapi-generator-gradle-plugin:6.2.1")
    }
}

plugins {
    id ("java")
    id ("org.openapi.generator") version "6.2.1"
    id ("org.springframework.boot") version "3.0.2"
    id ("io.spring.dependency-management") version "1.1.0"
    kotlin("jvm") version "1.7.22"
    kotlin("plugin.spring") version "1.7.22"
    kotlin("plugin.allopen") version "1.7.22"
    kotlin("plugin.jpa") version "1.7.22"
}

group = "playground"
version = "1.0-SNAPSHOT"
java.sourceCompatibility = JavaVersion.VERSION_17

repositories {
    mavenCentral()
    maven {
        url = uri("https://packages.confluent.io/maven")
    }
}

dependencies {
//    implementation("com.fasterxml.jackson.dataformat:jackson-dataformat-csv")

    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.boot:spring-boot-starter-data-jpa")
    implementation("org.springframework.kafka:spring-kafka")

    implementation("org.apache.avro:avro-tools:1.11.1") {
//        exclude(group = "ch.qos.logback", module = "logback-classic")
//        exclude(group = "org.slf4j", module = "slf4j-reload4j")
    }
//    implementation("org.apache.avro:avro:1.11.1")

    implementation("com.fasterxml.jackson.module:jackson-module-kotlin")
    implementation("org.jetbrains.kotlin:kotlin-reflect")
    implementation("io.confluent:kafka-avro-serializer:7.3.1")

    runtimeOnly("com.h2database:h2")

    testImplementation("org.springframework.boot:spring-boot-starter-test") {
        exclude(module = "mockito-core")
    }
    testImplementation("org.junit.jupiter:junit-jupiter-api")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine")
    testImplementation("com.ninja-squad:springmockk:4.0.0")
}

configurations.all {
    exclude(group = "ch.qos.logback", module = "logback-classic")
}

tasks.test {
    useJUnitPlatform()
}

tasks.withType<KotlinCompile> {
//    kotlinOptions.jvmTarget = "19"
    kotlinOptions {
        freeCompilerArgs += "-Xjsr305=strict"
        jvmTarget = "17"
    }
}

allOpen {
    annotation("jakarta.persistence.Entity")
    annotation("jakarta.persistence.Embeddable")
    annotation("jakarta.persistence.MappedSuperclass")
}

openApiGenerate {
    generatorName.set("kotlin-spring")
    inputSpec.set("${projectDir}/src/main/resources/spec/ce-pii-v3.yaml")
    outputDir.set("${buildDir}/generated")
    apiPackage.set("org.openapi.example.api")
    modelPackage.set("org.openapi.example.model")
    println("Finishing openApiGenerate ... ")
}

tasks.register("generateAvroClasses", JavaExec::class) {
    main = "org.apache.avro.tool.Main"
    classpath = configurations.compileClasspath.get() + configurations.runtimeClasspath.get()
    args = listOf("compile", "schema", "${projectDir}/src/main/resources/avro/customeronboardingsnapshot.avsc", "${buildDir}/classes/kotlin")
    println("Finishing generateAvroClasses ... ")
}


tasks.getByName("build")
    .dependsOn("generateAvroClasses")
    .dependsOn("openApiGenerate")
    .didWork


sourceSets {
    main {
        java {
            srcDirs("${buildDir}/classes/kotlin")
        }
    }
}

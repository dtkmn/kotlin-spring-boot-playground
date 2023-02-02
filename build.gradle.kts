import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

buildscript {
    dependencies {
        classpath("org.openapitools:openapi-generator-gradle-plugin:6.2.1")
    }
}

plugins {
    id ("java")
//    application
    id ("org.openapi.generator") version "6.2.1"
    id ("org.springframework.boot") version "3.0.2"
    id ("io.spring.dependency-management") version "1.1.0"
    kotlin("jvm") version "1.7.22"
    kotlin("plugin.spring") version "1.7.22"
    kotlin("plugin.allopen") version "1.7.22"
    kotlin("plugin.jpa") version "1.7.22"
//    id ("org.jetbrains.kotlin.plugin.noarg") version "1.8.0"
}

group = "playground"
version = "1.0-SNAPSHOT"
java.sourceCompatibility = JavaVersion.VERSION_17

repositories {
    mavenCentral()
}

dependencies {
//    implementation("com.fasterxml.jackson.dataformat:jackson-dataformat-csv")

    implementation("org.springframework.boot:spring-boot-starter-web") {
        exclude(group = "org.slf4j", module = "slf4j-log4j12")
    }
//    implementation("org.springframework.boot:spring-boot-starter-data-jpa")
    implementation("org.springframework.kafka:spring-kafka")

//    implementation("org.apache.avro:avro-tools:1.11.1")
    implementation("org.apache.avro:avro:1.11.1")

    implementation("com.fasterxml.jackson.module:jackson-module-kotlin")
    implementation("org.jetbrains.kotlin:kotlin-reflect")

    testImplementation("org.springframework.boot:spring-boot-starter-test")
//    testImplementation(kotlin("test"))
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
    generatorName.set("kotlin")
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

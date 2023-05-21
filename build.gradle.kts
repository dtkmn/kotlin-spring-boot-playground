import com.github.davidmc24.gradle.plugin.avro.GenerateAvroJavaTask
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

buildscript {
    dependencies {
        classpath("org.openapitools:openapi-generator-gradle-plugin:6.5.0")
    }
}

plugins {
    id ("java")
    id ("org.openapi.generator") version "6.5.0"
    id ("org.springframework.boot") version "3.0.2"
    id ("io.spring.dependency-management") version "1.1.0"
    id ("com.github.davidmc24.gradle.plugin.avro") version "1.3.0"
    kotlin("jvm") version "1.8.0"
    kotlin("plugin.spring") version "1.8.0"
    kotlin("plugin.allopen") version "1.8.0"
    kotlin("plugin.jpa") version "1.8.0"
}

group = "playground"
version = "1.0-SNAPSHOT"
java.sourceCompatibility = JavaVersion.VERSION_19

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

    implementation("org.apache.avro:avro:1.11.1")

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



allOpen {
    annotation("jakarta.persistence.Entity")
    annotation("jakarta.persistence.Embeddable")
    annotation("jakarta.persistence.MappedSuperclass")
}

openApiGenerate {
    generatorName.set("java")
    inputSpec.set("${projectDir}/src/main/resources/spec/ce-pii-v3.yaml")
    outputDir.set("${buildDir}/generated/openapi")
    apiPackage.set("org.openapi.example.api")
    modelPackage.set("org.openapi.example.model")
    configOptions.set(
        mapOf(
            "useJakartaEe" to "true"
        )
    )
    println("Finishing openApiGenerate ... ")
}

avro {
    fieldVisibility.set("PRIVATE")
    outputCharacterEncoding.set("UTF-8")
    stringType.set("String")
}

//tasks.register("generateAvroClasses", JavaExec::class) {
//    main = "org.apache.avro.tool.Main"
//    classpath = configurations.compileClasspath.get() + configurations.runtimeClasspath.get()
//    args = listOf("compile", "schema", "${projectDir}/src/main/resources/avro/customeronboardingsnapshot.avsc", "${buildDir}/classes/kotlin")
//    println("Finishing generateAvroClasses ... ")
//}

val avroSourceDir = "src/main/resources/avro"
val generateAvro = tasks.register<GenerateAvroJavaTask>("generateAvro") {
    source = fileTree(avroSourceDir).matching {
        include("**/*.avsc")
    }
    setOutputDir(file("${buildDir}/generated/avro"))
}


tasks.test {
    useJUnitPlatform()
}

tasks {
    withType<KotlinCompile> {
        kotlinOptions.jvmTarget = "19"
        kotlinOptions {
            freeCompilerArgs += "-Xjsr305=strict"
            jvmTarget = "19"
        }
        dependsOn(generateAvro)
        dependsOn(openApiGenerate)
    }
}

//tasks.getByName("build")
//    .dependsOn("generateAvro")
//    .dependsOn("openApiGenerate")
//    .didWork


sourceSets {
    main {
        java {
            srcDir("src/main/kotlin")
            srcDir("${buildDir}/generated/avro")
            srcDir("${buildDir}/generated/openapi/src/main/kotlin")
        }
    }
}

import com.github.davidmc24.gradle.plugin.avro.GenerateAvroJavaTask
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

buildscript {
    dependencies {
        classpath("org.openapitools:openapi-generator-gradle-plugin:6.5.0")
        classpath("io.spring.gradle:dependency-management-plugin:0.5.2.RELEASE")
    }
}

plugins {
    id ("java")
    id ("org.openapi.generator") version "6.5.0"
    id ("org.springframework.boot") version "3.2.5"
    id ("io.spring.dependency-management") version "1.1.0"
    id ("com.github.davidmc24.gradle.plugin.avro") version "1.3.0"
    kotlin("jvm") version "1.9.0"
    kotlin("plugin.spring") version "1.9.0"
    kotlin("plugin.allopen") version "1.9.0"
    kotlin("plugin.jpa") version "1.9.0"
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

dependencyManagement {
    imports {
        mavenBom("org.springframework.cloud:spring-cloud-dependencies:2021.0.5")
        mavenBom("org.springframework.cloud:spring-cloud-sleuth-otel-dependencies:1.1.2")
    }
}

dependencies {
//    implementation("com.fasterxml.jackson.dataformat:jackson-dataformat-csv")

    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.boot:spring-boot-starter-logging")
    implementation("org.springframework.boot:spring-boot-starter-data-jpa")
    implementation("org.springframework.boot:spring-boot-starter-actuator")
    implementation("org.springframework.boot:spring-boot-starter-validation")

    implementation("org.springframework.boot:spring-boot-starter-webflux")
    implementation("org.springframework.boot:spring-boot-starter-data-r2dbc")

    implementation("org.springframework.cloud:spring-cloud-starter-sleuth") {
        exclude(group = "org.springframework.cloud", module = "spring-cloud-sleuth-brave")
    }
    implementation("org.springframework.cloud:spring-cloud-sleuth-otel-autoconfigure")
    implementation("io.opentelemetry:opentelemetry-exporter-otlp:1.39.0")


    implementation("org.springframework.security:spring-security-config")

    implementation("org.springframework.kafka:spring-kafka")

    implementation("io.sentry:sentry-spring-boot-starter-jakarta:6.20.0")
    implementation("io.sentry:sentry-logback:6.20.0")

    implementation("org.apache.avro:avro:1.11.3")

    implementation("com.fasterxml.jackson.module:jackson-module-kotlin")
    implementation("org.jetbrains.kotlin:kotlin-reflect")
    implementation("io.confluent:kafka-avro-serializer:7.3.1")

    implementation("net.logstash.logback:logstash-logback-encoder:7.4")
    implementation("org.apache.httpcomponents:httpclient:4.5.14")
    implementation("org.springframework.security:spring-security-oauth2-jose:6.2.4")

    implementation("com.datadoghq:dd-trace-api:1.33.0")

    implementation("io.micrometer:micrometer-core:1.13.1")
//    implementation("io.opentelemetry:opentelemetry-sdk:1.26.0")

    implementation("org.apache.commons:commons-lang3:3.14.0")

    implementation("org.javamoney:moneta:1.4.2")

    implementation("org.apache.httpcomponents.client5:httpclient5:5.1.2")

    implementation("com.github.ben-manes.caffeine:caffeine")
    implementation("org.springframework:spring-context-support")
    implementation("org.springframework.security:spring-security-oauth2-resource-server")

    implementation("io.r2dbc:r2dbc-postgresql:0.8.13.RELEASE")
    runtimeOnly("org.postgresql:postgresql:42.7.3")
//    runtimeOnly("com.h2database:h2")

    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("org.junit.jupiter:junit-jupiter-api")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine")
    testImplementation("com.ninja-squad:springmockk:4.0.2")
    testImplementation("io.projectreactor:reactor-test")
}

//configure<com.palantir.gradle.docker.DockerExtension> {
//    name.set("your-docker-username/your-microservice:latest")
//    files("build/libs")
//}


allOpen {
    annotation("jakarta.persistence.Entity")
    annotation("jakarta.persistence.Embeddable")
    annotation("jakarta.persistence.MappedSuperclass")
}

openApiGenerate {
    generatorName.set("java")
    inputSpec.set("${projectDir}/src/main/resources/spec/ce-pii-v3.yaml")
    outputDir.set("${layout.buildDirectory}/generated/openapi")
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

val avroSourceDir = "src/main/resources/avro"
val generateAvro = tasks.register<GenerateAvroJavaTask>("generateAvro") {
    source = fileTree(avroSourceDir).matching {
        include("**/*.avsc")
    }
    setOutputDir(file("${layout.buildDirectory}/generated/avro"))
}


tasks.test {
    useJUnitPlatform()
}

tasks {
    withType<KotlinCompile> {
        kotlinOptions.jvmTarget = "17"
        kotlinOptions {
            freeCompilerArgs += "-Xjsr305=strict"
            jvmTarget = "17"
        }
        dependsOn(generateAvro)
        dependsOn(openApiGenerate)
    }
}

sourceSets {
    main {
        java {
            srcDir("src/main/kotlin")
            srcDir("${layout.buildDirectory}/generated/avro")
            srcDir("${layout.buildDirectory}/generated/openapi/src/main/kotlin")
        }
    }
}

plugins {
    jacoco
    id("org.springframework.boot") version "3.5.0"
    id("io.spring.dependency-management") version "1.1.7"
    kotlin("jvm") version "1.9.25"
    kotlin("plugin.spring") version "1.9.25"
    kotlin("plugin.jpa") version "1.9.25"
}

group = "com.ganeshl"
version = "0.0.1-SNAPSHOT"

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(17)
    }
}

repositories {
    mavenCentral()
}

dependencies {
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.boot:spring-boot-starter-validation")
    implementation("org.springframework.boot:spring-boot-starter-security")
    implementation("org.springframework.boot:spring-boot-starter-actuator")
    implementation("org.springframework.boot:spring-boot-starter-data-jdbc")

    // Kotlin
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin")
    implementation("org.jetbrains.kotlin:kotlin-reflect")

    // JPA
    implementation("org.springframework.boot:spring-boot-starter-data-jpa")

    // DB
    runtimeOnly("com.h2database:h2")

    // API Documentation
    implementation("org.springdoc:springdoc-openapi-starter-webmvc-ui:2.2.0")

    // Testing
    testImplementation("org.springframework.boot:spring-boot-starter-test") {
        exclude(group = "org.junit.vintage", module = "junit-vintage-engine") // Exclude JUnit 4
    }
    testImplementation("org.springframework.security:spring-security-test")
    testImplementation("com.ninja-squad:springmockk:4.0.2")
    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("org.jetbrains.kotlin:kotlin-test-junit5")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

kotlin {
    compilerOptions {
        freeCompilerArgs.addAll("-Xjsr305=strict")
    }
}

// JaCoCo Configuration
jacoco {
    toolVersion = "0.8.11" // Use the latest stable JaCoCo version
}

tasks.test {
    useJUnitPlatform()

    finalizedBy(tasks.jacocoTestReport)
}

tasks.jacocoTestReport {
    // Configure the report to generate HTML, XML, or CSV
    reports {
        xml.required.set(true)
        csv.required.set(false)
        html.required.set(true) // HTML report is very user-friendly
    }

    sourceSets(sourceSets.main.get()) // Ensure it points to your main source set

    classDirectories.setFrom(
        fileTree(layout.buildDirectory.dir("classes/kotlin/main").get().asFile) {
//            include("com/ganeshl/luciditycircuitbreaker/CB/**/*.class")
        },
        // Option 2 (if you have Java classes too):
        fileTree(layout.buildDirectory.dir("classes/java/main").get().asFile) {
//            include("com/ganeshl/luciditycircuitbreaker/CB/**/*.class")
        }
    )
}

// Configuration for Kotlin JPA plugin
allOpen {
    annotation("jakarta.persistence.Entity")
    annotation("jakarta.persistence.Embeddable")
    annotation("jakarta.persistence.MappedSuperclass")
}

noArg {
    annotation("jakarta.persistence.Entity")
}
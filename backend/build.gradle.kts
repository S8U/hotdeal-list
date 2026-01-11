plugins {
	kotlin("jvm") version "2.2.21"
	kotlin("plugin.spring") version "2.2.21"
	id("org.springframework.boot") version "4.0.1"
	id("io.spring.dependency-management") version "1.1.7"
	kotlin("plugin.jpa") version "2.2.21"
}

group = "com.github.s8u.hotdeallist"
version = "0.0.1-SNAPSHOT"
description = "Demo project for Spring Boot"

java {
	toolchain {
		languageVersion = JavaLanguageVersion.of(21)
	}
}

repositories {
	mavenCentral()
}

dependencies {
	// Kotlin
	implementation("tools.jackson.module:jackson-module-kotlin")
	implementation("org.jetbrains.kotlin:kotlin-reflect")

	// Spring Boot
	implementation("org.springframework.boot:spring-boot-starter-webmvc")
	implementation("org.springframework.boot:spring-boot-starter-data-jpa")

	// Database
	runtimeOnly("com.mysql:mysql-connector-j")
	runtimeOnly("org.mariadb.jdbc:mariadb-java-client")

	// API Documentation
	// OpenAPI 3.1부터는 nullable 속성이 삭제되어 수동으로 타입을 명시해야 함
	// springdoc-openapi 2.8.x 버전은 이를 자동으로 대응하지 못하여 2.7.0 버전을 사용함
	implementation("org.springdoc:springdoc-openapi-starter-webmvc-ui:2.7.0")

    // HTML Parser
    implementation("org.jsoup:jsoup:1.18.3")

    // AI
    implementation("org.springframework.ai:spring-ai-starter-model-openai:2.0.0-M1")

	// Test
	testImplementation("org.springframework.boot:spring-boot-starter-data-jpa-test")
	testImplementation("org.springframework.boot:spring-boot-starter-webmvc-test")
	testImplementation("org.jetbrains.kotlin:kotlin-test-junit5")
	testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

kotlin {
	compilerOptions {
		freeCompilerArgs.addAll("-Xjsr305=strict", "-Xannotation-default-target=param-property")
	}
}

allOpen {
	annotation("jakarta.persistence.Entity")
	annotation("jakarta.persistence.MappedSuperclass")
	annotation("jakarta.persistence.Embeddable")
}

tasks.withType<Test> {
	useJUnitPlatform()
}

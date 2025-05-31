plugins {
	kotlin("jvm") version "2.1.21"
	kotlin("plugin.spring") version "2.1.21"
	id("org.springframework.boot") version "3.5.0"
	id("io.spring.dependency-management") version "1.1.7"
}

group = "no.roar"
version = "0.0.1-SNAPSHOT"

java {
	toolchain {
		languageVersion = JavaLanguageVersion.of(21)
	}
}

repositories {
	mavenCentral()
}

val mockitoAgent = configurations.create("mockitoAgent")

dependencies {
	implementation("org.springframework.boot:spring-boot-starter")
	implementation("org.springframework.kafka:spring-kafka")
	implementation("org.apache.kafka:kafka-streams")
	runtimeOnly("com.fasterxml.jackson.module:jackson-module-kotlin")

	testImplementation("org.mockito.kotlin:mockito-kotlin:5.4.0")
	testImplementation("org.springframework.boot:spring-boot-starter-test")
	testImplementation("org.springframework.kafka:spring-kafka-test")
	testRuntimeOnly("org.junit.platform:junit-platform-launcher")

	testImplementation(libs.mockito)
	mockitoAgent(libs.mockito) { isTransitive = false }
}

kotlin {
	compilerOptions {
		freeCompilerArgs.addAll("-Xjsr305=strict")
	}
}

tasks.test {
	jvmArgs("-javaagent:${mockitoAgent.asPath}")
	useJUnitPlatform()
	systemProperty("spring.profiles.active", "test")
}

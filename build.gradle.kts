import com.google.protobuf.gradle.*

plugins {
	java
	id("org.springframework.boot")            version "3.3.2"
	id("io.spring.dependency-management")     version "1.1.5"
	id("org.graalvm.buildtools.native")       version "0.9.28"
	id("com.adarshr.test-logger")             version "4.0.0"
	id("com.google.protobuf")                 version "0.9.4"
	id("jacoco")
}

	group 					= "com.miscsb"
	version 				= "0.0.1-SNAPSHOT"
val protobufVersion 		= "3.25.3" // ver. 4 breaks this project
val grpcVersion 			= "1.66.0"
val generatedFilesBaseDir 	= "build/generated/source/proto"

java {
	sourceCompatibility = JavaVersion.VERSION_21
}

configurations {
	compileOnly {
		extendsFrom(configurations.annotationProcessor.get())
	}
}

repositories {
	mavenCentral()
}

dependencies {
	// gRPC / protobuf dependencies
	compileOnly("io.grpc:grpc-protobuf:${grpcVersion}")
	compileOnly("io.grpc:grpc-stub:${grpcVersion}")
	compileOnly("io.grpc:protoc-gen-grpc-java:${grpcVersion}")
	implementation("com.google.protobuf:protobuf-java:${protobufVersion}")
	implementation("net.devh:grpc-server-spring-boot-starter:2.13.1.RELEASE")

	// Spring Boot dependencies
	implementation("org.springframework.boot:spring-boot-starter")
	implementation("org.springframework.boot:spring-boot-starter-data-redis")
	implementation("org.springframework.boot:spring-boot-starter-security")
	implementation("org.springframework.boot:spring-boot-starter-web")

	compileOnly("org.projectlombok:lombok")
	developmentOnly("org.springframework.boot:spring-boot-devtools")
	annotationProcessor("org.projectlombok:lombok")

	// Test dependencies
	testImplementation("org.springframework.boot:spring-boot-starter-test")
	testImplementation("org.springframework.security:spring-security-test")
	testImplementation("org.testcontainers:testcontainers")
	testImplementation("org.testcontainers:junit-jupiter")
	testRuntimeOnly("org.junit.platform:junit-platform-launcher")

	// Other dependencies
	implementation("javax.annotation:javax.annotation-api:1.3.2")
	implementation("org.slf4j:slf4j-simple:1.7.32")
	testImplementation("ch.qos.logback:logback-classic:1.5.6")
	runtimeOnly("io.netty:netty-resolver-dns-native-macos:4.1.110.Final:osx-aarch_64")
}

protobuf {
	protoc {
		artifact = "com.google.protobuf:protoc:${protobufVersion}"
	}
	plugins {
		id("grpc") {
			artifact = "io.grpc:protoc-gen-grpc-java:${grpcVersion}"
		}
	}
	generateProtoTasks {
		ofSourceSet("main").forEach {
			it.plugins {
				id("grpc") { }
			}
		}
	}
}

tasks.withType<Test> {
	useJUnitPlatform()
	jvmArgs("-XX:+EnableDynamicAgentLoading")
	testLogging.showStandardStreams = true
	finalizedBy(tasks.withType<JacocoReport>())
}

tasks.withType<JacocoReport> {
	reports {
		xml.required = true
	}
	dependsOn(tasks.withType<Test>())
	afterEvaluate {
		classDirectories.setFrom(files(classDirectories.files.map {
			fileTree(it) { exclude("**/com/miscsb/bubble/api/proto/**") }
		}))
	}
}

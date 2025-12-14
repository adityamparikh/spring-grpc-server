import com.google.protobuf.gradle.id

plugins {
    java
    id("org.springframework.boot") version "4.0.0"
    id("io.spring.dependency-management") version "1.1.7"
    id("com.google.protobuf") version "0.9.5"
    id("org.graalvm.buildtools.native") version "0.10.4"
}

group = "com.example"
version = "0.0.1-SNAPSHOT"
description = "spring-grpc-server"

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(25)
    }
}

repositories {
    mavenCentral()
}

extra["springGrpcVersion"] = "1.0.0"

dependencies {
    // Spring gRPC - provides gRPC server infrastructure and Spring integration
    implementation("io.grpc:grpc-services")
    implementation("org.springframework.grpc:spring-grpc-spring-boot-starter")

    // JPA and Database - for persistent Todo storage
    implementation("org.springframework.boot:spring-boot-starter-data-jpa")
    runtimeOnly("com.h2database:h2")

    // Validation - for request validation
    implementation("org.springframework.boot:spring-boot-starter-validation")

    // Observability - metrics, tracing, and health endpoints
    implementation("org.springframework.boot:spring-boot-starter-actuator")
    implementation("io.micrometer:micrometer-tracing-bridge-brave")

    // Testing
    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("org.springframework.grpc:spring-grpc-test")
    testImplementation("org.awaitility:awaitility:4.2.0")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

dependencyManagement {
    imports {
        mavenBom("org.springframework.grpc:spring-grpc-dependencies:${property("springGrpcVersion")}")
    }
}

protobuf {
    protoc {
        artifact = "com.google.protobuf:protoc"
    }
    plugins {
        id("grpc") {
            artifact = "io.grpc:protoc-gen-grpc-java"
        }
    }
    generateProtoTasks {
        all().forEach {
            it.plugins {
                id("grpc") {
                    option("@generated=omit")
                }
            }
        }
    }
}

tasks.withType<Test> {
    useJUnitPlatform()
}

// GraalVM Native Image configuration
// This configures the native-image build process for optimal performance
graalvmNative {
    binaries {
        named("main") {
            // Build a mostly static native image (includes all dependencies)
            buildArgs.add("--static")

            // Initialize classes at build time for faster startup
            // Specific classes are initialized based on Spring's AOT analysis
            buildArgs.add("-H:+ReportExceptionStackTraces")

            // Enable monitoring features in native image
            buildArgs.add("-H:+AllowVMInspection")

            // Verbose output during build (useful for troubleshooting)
            buildArgs.add("--verbose")
        }
    }
}

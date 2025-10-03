plugins {
    java
    alias(libs.plugins.spring.boot)
    alias(libs.plugins.spring.dependency.management)
    jacoco
}

group = "com.searchlight"
version = "0.1.0"

java {
    sourceCompatibility = JavaVersion.VERSION_21
    targetCompatibility = JavaVersion.VERSION_21
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
    // Spring Boot
    implementation(libs.spring.boot.starter.web)
    implementation(libs.spring.boot.starter.actuator)
    implementation(libs.spring.boot.starter.validation)
    
    // Lucene
    implementation(libs.lucene.core)
    implementation(libs.lucene.analysis.common)
    implementation(libs.lucene.queryparser)
    implementation(libs.lucene.facet)
    
    // Observability
    implementation(libs.micrometer.registry.prometheus)
    implementation(libs.opentelemetry.instrumentation.annotations)
    
    // OpenAPI
    implementation(libs.springdoc.openapi.starter.webmvc.ui)
    
    // Utilities
    implementation(libs.jsoup)
    implementation(libs.rome)
    implementation(libs.onnxruntime)
    
    // Lombok
    compileOnly(libs.lombok)
    annotationProcessor(libs.lombok)
    
    // Testing
    testImplementation(libs.spring.boot.starter.test)
    testImplementation(libs.wiremock)
    testImplementation(libs.testcontainers.junit)
    testImplementation(libs.assertj.core)
    testCompileOnly(libs.lombok)
    testAnnotationProcessor(libs.lombok)
}

tasks.withType<Test> {
    useJUnitPlatform()
    finalizedBy(tasks.jacocoTestReport)
}

tasks.jacocoTestReport {
    dependsOn(tasks.test)
    reports {
        xml.required.set(true)
        html.required.set(true)
        csv.required.set(false)
    }
    classDirectories.setFrom(
        files(classDirectories.files.map {
            fileTree(it) {
                exclude(
                    "**/config/**",
                    "**/dto/**",
                    "**/SearchlightApplication.class"
                )
            }
        })
    )
}

tasks.jacocoTestCoverageVerification {
    violationRules {
        rule {
            limit {
                minimum = "0.90".toBigDecimal()
            }
        }
    }
}

tasks.withType<JavaCompile> {
    options.compilerArgs.add("-parameters")
}

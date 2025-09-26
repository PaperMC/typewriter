import org.gradle.api.tasks.testing.logging.TestExceptionFormat
import org.gradle.api.tasks.testing.logging.TestLogEvent

plugins {
    java
    `maven-publish`
}

java {
    withSourcesJar()
    withJavadocJar()
}

java.toolchain.languageVersion.set(JavaLanguageVersion.of(21))

repositories {
    mavenCentral()
}

val testData = sourceSets.create("testData")

dependencies {
    implementation("com.google.guava:guava:33.3.1-jre")
    implementation("org.apache.logging.log4j:log4j-core:2.24.3")
    implementation("org.slf4j:slf4j-api:2.0.17")

    compileOnly("org.checkerframework:checker-qual:3.49.2")
    compileOnly("org.jetbrains:annotations:26.0.2")

    testImplementation("org.junit.jupiter:junit-jupiter:5.12.2")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
    testImplementation("org.yaml:snakeyaml:2.4")
    testImplementation(testData.output)
}

tasks {
    test {
        useJUnitPlatform {
            if (System.getenv()["CI"]?.toBoolean() == true) {
                excludeTags("parser")
            }
        }
        inputs.files(testData.output.files)

        testLogging {
            showStackTraces = true
            exceptionFormat = TestExceptionFormat.FULL
            events(TestLogEvent.STANDARD_OUT)
        }
    }

    compileTestJava {
        options.compilerArgs.add("-parameters")
    }

    javadoc {
        (options as StandardJavadocDocletOptions).tags("apiNote:a:API Note:")
    }
}

publishing {
    publications.create<MavenPublication>("maven") {
        artifactId = rootProject.name
        from(components["java"])
    }

    repositories {
        val isSnapshot = rootProject.version.toString().endsWith("-SNAPSHOT")
        val url = if (isSnapshot) {
            "https://artifactory.papermc.io/artifactory/snapshots/"
        } else {
            "https://artifactory.papermc.io/artifactory/releases/"
        }
        maven(url) {
            name = "papermc"
            credentials(PasswordCredentials::class)
        }
    }
}

tasks.register("printVersion") {
    val version = project.version
    doFirst {
        println(version)
    }
}

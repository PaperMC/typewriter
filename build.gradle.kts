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
    implementation("org.apache.logging.log4j:log4j-core:2.24.2")
    implementation("org.slf4j:slf4j-api:2.0.16")

    compileOnly("org.checkerframework:checker-qual:3.48.3")
    compileOnly("org.jetbrains:annotations:26.0.1")

    testImplementation("org.junit.jupiter:junit-jupiter:5.11.3")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
    testImplementation("org.yaml:snakeyaml:2.3")
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
            "https://repo.papermc.io/repository/maven-snapshots/"
        } else {
            "https://repo.papermc.io/repository/maven-releases/"
        }
        maven(url) {
            name = "papermc"
            credentials(PasswordCredentials::class)
        }
    }
}

tasks.register("printVersion") {
    doFirst {
        println(version)
    }
}

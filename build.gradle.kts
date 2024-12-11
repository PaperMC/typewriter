import org.gradle.api.tasks.testing.logging.TestExceptionFormat
import org.gradle.api.tasks.testing.logging.TestLogEvent

plugins {
    java
    `maven-publish`
}

val classpathAware = sourceSets.create("classpathAware")

java {
    withSourcesJar()
    withJavadocJar()
    toolchain {
        languageVersion = JavaLanguageVersion.of(21)
    }
    registerFeature("classpathAware") {
        usingSourceSet(classpathAware)
        withSourcesJar()
        withJavadocJar()
    }
}

repositories {
    mavenCentral()
}

val testData = sourceSets.create("testData")
val testJavaPreviewVersion = providers.gradleProperty("testJavaPreviewVersion") // enable preview to test JEPs

dependencies {
    // Gradle doesn't seem to handle properly MRJ at the moment: https://github.com/gradle/gradle/issues/19587
    // so that dependency must be explicitly applied on top for modular application that
    // want the extension
    "classpathAwareImplementation"("io.github.classgraph:classgraph:4.8.179")
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
        failFast = true
        inputs.files(testData.output.files)

        if (testJavaPreviewVersion.isPresent) {
            jvmArgs("--enable-preview")
            javaLauncher = project.javaToolchains.launcherFor {
                languageVersion = JavaLanguageVersion.of(testJavaPreviewVersion.get())
            }
        }

        testLogging {
            showStackTraces = true
            exceptionFormat = TestExceptionFormat.FULL
            events(TestLogEvent.STANDARD_OUT)
        }
    }

    jar {
        manifest {
            attributes("Automatic-Module-Name" to "io.papermc.typewriter")
        }
    }

    if (testJavaPreviewVersion.isPresent) {
        listOf(compileTestJava.name, testData.compileJavaTaskName).forEach {
            named<JavaCompile>(it).configure {
                options.compilerArgs.add("--enable-preview")
                javaCompiler = project.javaToolchains.compilerFor {
                    languageVersion = JavaLanguageVersion.of(testJavaPreviewVersion.get())
                }
            }
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

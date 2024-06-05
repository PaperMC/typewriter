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
    implementation("com.google.guava:guava:33.2.1-jre")
    implementation("org.apache.logging.log4j:log4j-core:2.23.1")
    implementation("org.slf4j:slf4j-api:2.0.13")

    compileOnly("org.checkerframework:checker-qual:3.44.0")
    compileOnly("org.jetbrains:annotations:24.1.0")

    testImplementation("org.junit.jupiter:junit-jupiter:5.10.2")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
    testImplementation("org.yaml:snakeyaml:2.2")
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
}

tasks.register("printVersion") {
    doFirst {
        println(version)
    }
}

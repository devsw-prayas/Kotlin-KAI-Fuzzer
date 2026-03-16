plugins {
    id("java")
}

group = "io.kai"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

java{
    toolchain {languageVersion = JavaLanguageVersion.of(24)}
}

dependencies{
    implementation("org.jetbrains.kotlin:kotlin-compiler-embeddable:2.3.10")
    implementation("com.fasterxml.jackson.core:jackson-databind:2.17.0")
    implementation("info.picocli:picocli:4.7.5")
    testImplementation("org.junit.jupiter:junit-jupiter:5.10.2")
    testImplementation("org.mockito:mockito-core:5.11.0")
}

tasks.test {
    useJUnitPlatform()
}

tasks.jar {
    manifest {
        attributes["Main-Class"] = "io.kai.CLI"
    }
    // include all dependencies in the jar
    from(configurations.runtimeClasspath.get().map {
        if (it.isDirectory) it else zipTree(it)
    })
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
}
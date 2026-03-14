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
    // PSI Injection
    implementation("org.jetbrains.kotlin:kotlin-compiler-embeddable:2.3.10")
    // Artifact serialization (chain.json, verdict.json)
    implementation("com.fasterxml.jackson.core:jackson-databind:2.17.0")

    // CLI parsing
    implementation("info.picocli:picocli:4.7.5")

    // Testing
    testImplementation("org.junit.jupiter:junit-jupiter:5.10.2")
    testImplementation("org.mockito:mockito-core:5.11.0")
}

tasks.test {
    useJUnitPlatform()
}
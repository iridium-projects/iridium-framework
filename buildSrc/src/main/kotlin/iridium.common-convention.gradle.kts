plugins {
    `java-library`
    id("com.gradleup.shadow")
}

group = "cc.asylum"

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(25))
    }
}

repositories {
    mavenCentral()
}

dependencies() {
    implementation("org.projectlombok:lombok:1.18.48")
}

tasks.withType<JavaCompile> {
    options.encoding = "UTF-8"
}

tasks.withType<Test> {
    useJUnitPlatform()
}

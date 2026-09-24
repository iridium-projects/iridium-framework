plugins {
    id("java")
    id("iridium.common-convention")
}

repositories {
    mavenCentral()
}

dependencies() {
    api("org.slf4j:slf4j-api:2.0.16")

    testImplementation(platform("org.junit:junit-bom:5.10.0"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

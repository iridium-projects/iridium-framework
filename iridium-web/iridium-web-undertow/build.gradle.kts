plugins {
    id("java")
    id("iridium.module-convention")
}

repositories {
    mavenCentral()
}

dependencies() {
    api(project(":iridium-web:iridium-web-core"))
    api(project(":iridium-json:iridium-json-core"))

    implementation("io.undertow:undertow-core:2.4.3.Final")

    api("org.slf4j:jul-to-slf4j:2.0.16")
}

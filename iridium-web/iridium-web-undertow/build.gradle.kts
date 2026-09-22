plugins {
    id("java")
    id("iridium.module-convention")
}

repositories {
    mavenCentral()
}

dependencies() {
    api(project(":iridium-web:iridium-web-core"))

    implementation("io.undertow:undertow-core:2.4.3.Final")
}

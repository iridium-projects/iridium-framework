plugins {
    id("java")
    id("iridium.module-convention")
}

repositories {
    mavenCentral()
}

dependencies() {
    api(project(":iridium-json:iridium-json-core"))
}

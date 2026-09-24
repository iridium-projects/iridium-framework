plugins {
    id("java")
    id("iridium.common-convention")
}

repositories {
    mavenCentral()
}

dependencies() {
    api(project(":iridium-core"))
    api(project(":iridium-json"))
}

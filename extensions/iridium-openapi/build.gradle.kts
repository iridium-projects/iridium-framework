plugins {
    id("java")
    id("iridium.module-convention")
}

repositories {
    mavenCentral()
}

dependencies() {
    api(project(":iridium-web"))
    api(project(":iridium-log"))
}

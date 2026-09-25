plugins {
    id("java")
    id("iridium.common-convention")
}

repositories {
    mavenCentral()
}

dependencies() {
    api(project(":iridium-core"))
    api("io.avaje:avaje-jsonb:3.16")
}

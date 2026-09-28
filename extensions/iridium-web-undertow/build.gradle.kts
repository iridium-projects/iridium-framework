plugins {
    id("java")
    id("iridium.module-convention")
}

repositories {
    mavenCentral()
}

dependencies() {
    api(project(":iridium-web"))

    implementation(libs.undertow.core)

    api(libs.slf4j.jul)
}

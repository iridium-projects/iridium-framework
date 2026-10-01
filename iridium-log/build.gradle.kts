plugins {
    id("java")
    id("iridium.common-convention")
    id("iridium.test")
}

repositories {
    mavenCentral()
}

dependencies() {
    api(libs.slf4j.api)
}

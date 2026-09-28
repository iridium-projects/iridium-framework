plugins {
    id("java")
    id("iridium.common-convention")
}

repositories {
    mavenCentral()
}

dependencies() {
    api(libs.slf4j.api)
}

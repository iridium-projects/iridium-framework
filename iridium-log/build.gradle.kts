plugins {
    id("java")
    id("iridium.common-convention")
    alias(libs.plugins.iridium.test)
}

repositories {
    mavenCentral()
}

dependencies() {
    api(libs.slf4j.api)
}

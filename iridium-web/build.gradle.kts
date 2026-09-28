plugins {
    id("java")
    id("iridium.common-convention")
}

repositories {
    mavenCentral()
}

dependencies() {
    api(project(":iridium-core"))
    compileOnly(libs.undertow.core)
    compileOnly(project(":iridium-codegen"))
    annotationProcessor(project(":iridium-codegen"))
}

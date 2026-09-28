plugins {
    id("java")
    id("iridium.common-convention")
}

repositories {
    mavenCentral()
}

dependencies() {
    api(project(":iridium-core"))
    compileOnly("io.undertow:undertow-core:2.4.3.Final")
    compileOnly(project(":iridium-codegen"))
    annotationProcessor(project(":iridium-codegen"))
}

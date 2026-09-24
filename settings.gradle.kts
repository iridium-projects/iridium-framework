pluginManagement {
    includeBuild("iridium-gradle-plugin")
}

plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}

rootProject.name = "iridium"

include("iridium")
include("iridium-core")
include("iridium-web")
include("iridium-json")
include("iridium-codegen")
include("iridium-config")
include("iridium-tests")

fun extension(name: String) {
    include(name)
    project(":$name").projectDir = file("extensions/$name")
}

fun demo(name: String) {
    include(name)
    project(":$name").projectDir = file("demos/$name")
}

extension("iridium-web-undertow")
extension("iridium-json-avaje")
extension("iridium-log")
extension("iridium-data-jimmer")

demo("iridium-demo-hello")

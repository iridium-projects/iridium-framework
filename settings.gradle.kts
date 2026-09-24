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
include("iridium-test")

fun extension(name: String) {
    include(name)
    project(":$name").projectDir = file("extensions/$name")
}

extension("iridium-web-undertow")
extension("iridium-json-avaje")

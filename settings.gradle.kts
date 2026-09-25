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
include("iridium-codegen")
include("iridium-config")

fun extension(name: String) {
    include(name)
    project(":$name").projectDir = file("extensions/$name")
}

fun demo(name: String) {
    include(name)
    project(":$name").projectDir = file("demos/$name")
}

extension("iridium-web-undertow")
extension("iridium-log")
extension("iridium-data-jimmer")
extension("iridium-openapi")
extension("iridium-hot-reloading")

demo("iridium-demo-hello")
demo("iridium-demo-shop")

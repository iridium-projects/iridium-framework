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

fun demo(name: String) {
    include(name)
    project(":$name").projectDir = file("demos/$name")
}

demo("iridium-demo-hello")

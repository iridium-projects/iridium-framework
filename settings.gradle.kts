pluginManagement {
    includeBuild("iridium-gradle-plugin")
    includeBuild("../iridium-conventions")
}

plugins {
    id("iridium.dev")
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}

rootProject.name = "iridium"

include("iridium")
include("iridium-core")
include("iridium-log")
include("iridium-web")
include("iridium-codegen")
include("iridium-config")

gradle.beforeProject {
    group = "cc.asylum"
    version = "0.0.1"
}

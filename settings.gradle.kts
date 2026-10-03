pluginManagement {
    plugins {
        id("iridium.test") version "0.0.5"
    }

    if (settings.rootDir.name != "iridium") {
        val local = file("../iridium-conventions")
        if (local.resolve("settings.gradle.kts").isFile) {
            includeBuild(local)
        }
    }

    repositories {
        maven {
            url = uri("https://maven.iridium4j.io/releases")
        }
        gradlePluginPortal()
    }
}

plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}

rootProject.name = "iridium"

include("iridium-core")
include("iridium-log")
include("iridium-web")
include("iridium-codegen")
include("iridium-config")

gradle.beforeProject {
    group = "cc.asylum"
    version = "0.0.1"
}

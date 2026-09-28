plugins {
  `kotlin-dsl`
  `java-gradle-plugin`
}

group = "cc.asylum"
version = "0.1.0"

repositories {
  gradlePluginPortal()
  mavenCentral()
}

dependencies {
  implementation(libs.shadow)
}

gradlePlugin {
  plugins {
    create("iridium") {
      id = "cc.asylum.iridium"
      implementationClass = "cc.asylum.iridium.gradle.IridiumPlugin"
    }
  }
}

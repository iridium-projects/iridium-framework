plugins {
  id("java")
  id("iridium.module-convention")
}

repositories {
  mavenCentral()
}

dependencies {
  api(project(":iridium-config"))
  api(project(":iridium-log"))
}

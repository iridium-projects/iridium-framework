plugins {
  id("java")
  id("iridium.module-convention")
}

repositories {
  mavenCentral()
}

dependencies() {
  api(project(":iridium-web"))
  api(libs.iridium.web.undertow)
  api(project(":iridium-log"))
  api(project(":iridium-codegen"))
  api(project(":iridium-config"))
}

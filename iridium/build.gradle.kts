plugins {
  id("java")
  id("iridium.module-convention")
}

repositories {
  mavenCentral()
}

dependencies() {
  api(project(":iridium-web"))
  api(project(":iridium-web-undertow"))
  api(project(":iridium-json"))
  api(project(":iridium-json-avaje"))
  api(project(":iridium-codegen"))
  api(project(":iridium-codegen-web"))
}

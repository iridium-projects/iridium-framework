plugins {
  id("java")
  id("iridium.module-convention")
}

repositories {
  mavenCentral()
}

dependencies() {
  api(project(":iridium-web:iridium-web-core"))
  api(project(":iridium-web:iridium-web-undertow"))
  api(project(":iridium-json:iridium-json-core"))
  api(project(":iridium-json:iridium-json-avaje"))
  api(project(":iridium-codegen:iridium-codegen-core"))
  api(project(":iridium-codegen:iridium-codegen-web"))
}

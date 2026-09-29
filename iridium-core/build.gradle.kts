plugins {
  id("java")
  id("iridium.common-convention")
}

repositories {
  mavenCentral()
}

dependencies() {
  annotationProcessor(libs.avaje.jsonb.generator)
  api(libs.slf4j.api)
  api(libs.avaje.jsonb)
  api(libs.iridium.log)
  compileOnly(project(":iridium-codegen"))
  annotationProcessor(project(":iridium-codegen"))
}

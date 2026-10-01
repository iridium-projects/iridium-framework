plugins {
  id("java")
  id("iridium.common-convention")
  id("iridium.test")
}

repositories {
  mavenCentral()
}

dependencies() {
  annotationProcessor(libs.avaje.jsonb.generator)
  api(libs.slf4j.api)
  api(libs.avaje.jsonb)
  compileOnly(project(":iridium-codegen"))
  testImplementation(project(":iridium-codegen"))
  compileOnly(project(":iridium-log"))
  annotationProcessor(project(":iridium-codegen"))
}

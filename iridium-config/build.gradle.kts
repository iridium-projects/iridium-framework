plugins {
  id("java")
  id("iridium.common-convention")
  id("iridium.test")
}

repositories {
  mavenCentral()
}

dependencies {
  api(project(":iridium-core"))
  implementation(libs.snakeyaml)
  compileOnly(project(":iridium-codegen"))
  testImplementation(project(":iridium-codegen"))
  annotationProcessor(project(":iridium-codegen"))
}

plugins {
  id("java")
  id("iridium.common-convention")
}

repositories {
  mavenCentral()
}

dependencies {
  api(project(":iridium-core"))
  implementation("org.yaml:snakeyaml:2.3")
  compileOnly(project(":iridium-codegen"))
  annotationProcessor(project(":iridium-codegen"))
}

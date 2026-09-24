plugins {
    id("java")
    id("iridium.module-convention")
}

repositories {
    mavenCentral()
}

dependencies() {
  api(project(":iridium-web"))
  api(project(":iridium-log"))
  api(project(":iridium-config"))

  api("io.swagger.core.v3:swagger-core:2.2.54")
}

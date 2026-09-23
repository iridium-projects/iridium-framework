plugins {
  id("java")
  id("iridium.module-convention")
}

repositories {
  mavenCentral()
}

dependencies() {
  api(project(":iridium-json"))
  api("io.avaje:avaje-jsonb:3.16")
}

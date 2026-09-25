plugins {
  id("java")
  id("iridium.common-convention")
}

repositories {
  mavenCentral()
}

dependencies() {
  annotationProcessor("io.avaje:avaje-jsonb-generator:3.16")
  api("org.slf4j:slf4j-api:2.0.16")
  api("io.avaje:avaje-jsonb:3.16")
  api(project(":iridium-log"))
}

plugins {
  id("java")
  id("iridium.module-convention")
}

repositories {
  mavenCentral()
}

dependencies() {
  implementation("com.io7m.jodist:com.io7m.jodist.core:2.0.1")
  api(project(":iridium-web:iridium-web-core"))
}

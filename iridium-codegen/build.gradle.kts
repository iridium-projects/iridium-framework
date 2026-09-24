plugins {
  id("java")
  id("iridium.common-convention")
}

repositories {
  mavenCentral()
}

dependencies() {
  api(project(":iridium-core"))
  api(project(":iridium-web"))
  api(project(":iridium-config"))
  api("com.io7m.jodist:com.io7m.jodist.core:2.0.1")
}

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
  api("com.io7m.jodist:com.io7m.jodist.core:2.0.1")

  testImplementation(platform("org.junit:junit-bom:5.10.0"))
  testImplementation("org.junit.jupiter:junit-jupiter")
  testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

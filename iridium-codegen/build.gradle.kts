plugins {
  id("java")
  id("iridium.common-convention")
  id("iridium.test")
}

repositories {
  mavenCentral()
  maven {
    url = uri("https://maven.iridium4j.io/releases")
  }
}

dependencies() {
  api(libs.forgery.jodist)
}

plugins {
  id("java")
  id("iridium.common-convention")
}

repositories {
  mavenCentral()
  maven {
    url = uri("https://maven.iridium4j.io/releases")
  }
}

dependencies() {
  api("cc.asylum:forgery-jodist:0.0.1")
}

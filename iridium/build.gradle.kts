plugins {
  id("java")
  id("iridium.common-convention")
}

repositories {
  mavenCentral()
  maven("http://mvn.int.revived.club/releases") {
    isAllowInsecureProtocol = true
  }
}

dependencies() {
  implementation(project(":shared"))

  api("de.yyuh.libs:celery:1.0.2-SNAPSHOT")
  api("com.fasterxml.jackson.core:jackson-databind:2.21.3")
  api("com.fasterxml.jackson.datatype:jackson-datatype-jsr310:2.21.3")
}

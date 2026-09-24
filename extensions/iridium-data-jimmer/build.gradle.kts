plugins {
  id("java")
  id("iridium.module-convention")
}

repositories {
  mavenCentral()
}

dependencies {
  api(project(":iridium-config"))
  api(project(":iridium-log"))
  api("org.babyfish.jimmer:jimmer-sql:0.12.3")
  api("com.fasterxml.jackson.core:jackson-databind:2.18.2")

  implementation("com.zaxxer:HikariCP:6.3.3")
}

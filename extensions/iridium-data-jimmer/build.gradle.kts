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
  compileOnly(project(":iridium-web"))
  api(libs.jimmer.sql)
  api(libs.jackson.databind)

  implementation(libs.hikaricp)
}

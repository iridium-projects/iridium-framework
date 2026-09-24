plugins {
  id("java")
  id("iridium.common-convention")
}

dependencies {
  testImplementation(project(":iridium-core"))
  testImplementation(project(":iridium-web"))
  testImplementation(project(":iridium-config"))
  testImplementation(project(":iridium-codegen"))
  testImplementation(project(":iridium-log"))
  testImplementation(project(":iridium-web-undertow"))
  testImplementation(project(":iridium-data-jimmer"))
  testImplementation(project(":iridium-openapi"))
  testImplementation(project(":iridium-hot-reloading"))

  testImplementation("org.yaml:snakeyaml:2.3")
  testImplementation("com.zaxxer:HikariCP:6.3.3")
  testImplementation("io.undertow:undertow-core:2.4.3.Final")
  testImplementation("com.h2database:h2:2.3.232")

  testAnnotationProcessor("org.babyfish.jimmer:jimmer-apt:0.12.3")

  testImplementation(platform("org.junit:junit-bom:5.10.0"))
  testImplementation("org.junit.jupiter:junit-jupiter")
  testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

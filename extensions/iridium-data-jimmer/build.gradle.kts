plugins {
  id("java")
  id("iridium.module-convention")
}

repositories {
  mavenCentral()
}

dependencies {
  api(project(":iridium-config"))
  api("org.babyfish.jimmer:jimmer-sql:0.12.3")
  api("com.fasterxml.jackson.core:jackson-databind:2.18.2")

  implementation("com.zaxxer:HikariCP:6.3.3")

  testImplementation(project(":iridium-codegen"))
  testAnnotationProcessor("org.babyfish.jimmer:jimmer-apt:0.12.3")
  testImplementation("com.h2database:h2:2.3.232")
  testImplementation(platform("org.junit:junit-bom:5.10.0"))
  testImplementation("org.junit.jupiter:junit-jupiter")
  testRuntimeOnly("org.junit.platform:junit-platform-launcher")
  testRuntimeOnly("org.slf4j:slf4j-nop:1.7.36")
}

plugins {
  id("java")
  id("iridium.common-convention")
}

dependencies() {
  implementation(project(":shared"))
  api(project(":iridium"))
}

plugins {
  id("java")
  id("iridium.module-convention")
}

repositories {
  mavenCentral()
}

dependencies() {
  api(project(":iridium-log"))
  implementation(project(":iridium-web"))
}

tasks.jar {
  manifest {
    attributes(
      "Premain-Class" to "cc.asylum.iridium.reloading.agent.HotReloadingAgent",
      "Agent-Class" to "cc.asylum.iridium.reloading.agent.HotReloadingAgent",
      "Can-Redefine-Classes" to "true",
    )
  }
}

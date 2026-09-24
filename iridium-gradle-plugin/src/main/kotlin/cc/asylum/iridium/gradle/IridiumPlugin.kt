package cc.asylum.iridium.gradle

import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.file.DuplicatesStrategy
import org.gradle.api.plugins.JavaPluginExtension
import org.gradle.api.tasks.application.CreateStartScripts
import org.gradle.api.tasks.compile.JavaCompile
import org.gradle.jvm.toolchain.JavaLanguageVersion

class IridiumPlugin : Plugin<Project> {

  override fun apply(project: Project) {
    project.pluginManager.apply("java")
    project.pluginManager.apply("application")
    project.pluginManager.apply("com.gradleup.shadow")

    project.extensions.getByType(JavaPluginExtension::class.java).toolchain {
      languageVersion.set(JavaLanguageVersion.of(25))
    }

    project.repositories.mavenCentral()
    project.tasks.withType(JavaCompile::class.java).configureEach {
      options.compilerArgs.add("-parameters")
    }
    addDependencies(project)

    project.tasks.named("jar").configure {
      enabled = false
    }

    project.tasks.named("shadowJar", ShadowJar::class.java).configure {
      archiveClassifier.set("")
      duplicatesStrategy = DuplicatesStrategy.INCLUDE
      mergeServiceFiles()
    }

    val shadowJar = project.tasks.named("shadowJar", ShadowJar::class.java)
    project.tasks.named("startScripts", CreateStartScripts::class.java).configure {
      dependsOn(shadowJar)
      classpath = project.files(shadowJar)
    }
  }

  private fun addDependencies(project: Project) {
    val local = project.rootProject.findProject(":iridium")
    if (local != null) {
      project.dependencies.add("implementation", local)
      project.dependencies.add(
        "annotationProcessor",
        project.rootProject.project(":iridium-codegen"),
      )
      return
    }

    val version = project.findProperty("iridium.version") as String? ?: "0.1.0"
    project.dependencies.add("implementation", "cc.asylum:iridium:$version")
    project.dependencies.add("annotationProcessor", "cc.asylum:iridium-codegen:$version")
  }
}

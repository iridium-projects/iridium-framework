package cc.asylum.iridium.gradle

import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.artifacts.ProjectDependency
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
    wireData(project)
    wireJson(project)

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
    } else {
      val version = project.findProperty("iridium.version") as String? ?: "0.1.0"
      project.dependencies.add("implementation", "cc.asylum:iridium:$version")
      project.dependencies.add("annotationProcessor", "cc.asylum:iridium-codegen:$version")
    }
  }

  private fun wireData(project: Project) {
    project.afterEvaluate {
      val declared = project.configurations.flatMap { it.dependencies }
      val jimmer = declared.firstOrNull { it.group == "org.babyfish.jimmer" }
      val data = declared.firstOrNull { it.name == "iridium-data-jimmer" }
      if (jimmer == null && data == null) {
        return@afterEvaluate
      }
      val processors = project.configurations.getByName("annotationProcessor")
      if (data != null && processors.dependencies.none { it.name == "iridium-data-jimmer" }) {
        when (data) {
          is ProjectDependency -> project.dependencies.add("annotationProcessor", project.project(data.path))
          else -> project.dependencies.add(
            "annotationProcessor",
            "${data.group}:${data.name}:${data.version}",
          )
        }
      }
      if (processors.dependencies.none { it.name == "jimmer-apt" }) {
        val version = jimmer?.version?.takeUnless { it.isBlank() } ?: "0.12.3"
        project.dependencies.add("annotationProcessor", "org.babyfish.jimmer:jimmer-apt:$version")
      }
    }
  }

  private fun wireJson(project: Project) {
    project.dependencies.add("annotationProcessor", "io.avaje:avaje-jsonb-generator:3.16")
  }
}

plugins {
    id("iridium.common-convention")
}

dependencies {
    add("api", project(":iridium-core"))

    if (project.path != ":iridium-codegen") {
        add("compileOnly", project(":iridium-codegen"))
        add("annotationProcessor", project(":iridium-codegen"))
    }
}

tasks.withType<JavaCompile>().configureEach {
    options.compilerArgs.add("-Airidium.module=${project.name}")
}

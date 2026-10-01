plugins {
    id("iridium.common-convention")
}

dependencies {
    add("api", project(":iridium-core"))

    val skip = setOf(":iridium-codegen", ":iridium-core", ":iridium-web", ":iridium-config", "iridium-log")
    if (project.path !in skip) {
        add("compileOnly", project(":iridium-codegen"))
        add("annotationProcessor", project(":iridium-codegen"))
        add("annotationProcessor", project(":iridium-core"))
        add("annotationProcessor", project(":iridium-web"))
        add("annotationProcessor", project(":iridium-config"))
    }
}

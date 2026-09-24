plugins {
    id("iridium.common-convention")
}

dependencies {
    add("api", project(":iridium-core"))

    val codegenPaths = setOf(
        ":iridium-codegen"
    )
    if (project.path !in codegenPaths) {
        add("compileOnly", project(":iridium-codegen"))
        add("annotationProcessor", project(":iridium-codegen"))
    }
}

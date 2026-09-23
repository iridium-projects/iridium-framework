plugins {
    id("iridium.common-convention")
}

dependencies {
    add("api", project(":iridium-core"))

    val codegenPaths = setOf(
        ":iridium-codegen",
        ":iridium-codegen-web"
    )
    if (project.path !in codegenPaths) {
        add("compileOnly", project(":iridium-codegen"))
        add("annotationProcessor", project(":iridium-codegen"))
        add("compileOnly", project(":iridium-codegen-web"))
        add("annotationProcessor", project(":iridium-codegen-web"))
    }
}

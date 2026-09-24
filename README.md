<div align="center">
  <img src="img/logo.png" alt="Iridium" height="240">
</div>

<br>

# Iridium
Hardwired Java. Full-stack framework built to endure and work in every environment. Zero Reflection. Zero Guesswork.
<br>
Inspired by Spring Boot. Built to stay small.

## Getting started

```kotlin
plugins {
    id("cc.asylum.iridium")
}

application {
    mainClass = "example.Application"
}
```

```java
@WebApplication
public final class Application {

    public static void main(final String[] args) {
        Iridium.run(Application.class, args);
    }
}
```

```java
@RestController
public final class HelloController {

    @GET("/hello")
    public Response<?> hello(@RequestParam(defaultValue = "world") final String name) {
        return Response.ok("Hello, " + name + "!");
    }
}
```

```bash
./gradlew run
```

## Features

- Compile-time dependency injection (`@Component`)
- Configuration binding (`@ConfigurationProperties`, `@Value`) from `application.yml` and `application.properties`
- Annotation-based HTTP routing (`@RestController`, `@GET`, `@POST`, …)
- Request binding (`@PathVariable`, `@RequestParam`, `@RequestBody`, …)
- Bean Validation–style constraints, generated
- Pluggable HTTP server
- Gradle plugin that wires processors, the default stack, and a fat JAR
- Hot Reloading

Default stack: Undertow + Avaje Jsonb. Swap in another server by depending on a different extension.

## Hot Reloading

```gradle

dependencies {
    runtimeOnly project(':iridium-hot-reloading')
}

tasks.named('run') {
    def agent = project(':iridium-hot-reloading').tasks.named('jar')
    dependsOn agent
    jvmArgs "-javaagent:${agent.get().archiveFile.get().asFile.absolutePath}"
}

```

## Requirements

- Java 25
- Gradle 9

## License

Licensed under the [Apache License 2.0](LICENSE.txt).

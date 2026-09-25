# iridium-hot-reloading

A hot-reloading Java agent for [iridium](https://codeberg.org/iridium-projects/iridium-framework/).

## Setup

Add the hot-reloading agent as a runtime dependency:

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

The `run` task automatically builds the agent before starting the application and attaches it as a Java agent.

## Usage

The iridium hot-reloading agent automatically detects newly built classes and reloads them at runtime.

To apply your latest changes, simply rebuild the project:

```bash
./gradlew build
```

Once the build completes, the agent picks up the newly built classes and applies the changes without requiring a full application restart.

## Credits

This project was inspired by [Bernd Müller](https://www.pdbm.de/) and his talk at **JavaForumNord 2026**.

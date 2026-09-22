package de.yyuh.iridium.core.hook;

@FunctionalInterface
public interface ShutdownHook {

    void run();

    default int priority() {
        return 0;
    }
}

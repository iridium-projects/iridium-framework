package dev.yyuh.iridium.test;

import de.yyuh.iridium.core.component.Component;

@Component
public final class GreetingService {

    public String greet() {
        return "Hello from GreetingService";
    }
}

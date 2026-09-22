package dev.yyuh.iridium.web.undertow;

import de.yyuh.iridium.core.component.Component;
import de.yyuh.iridium.core.result.Result;
import de.yyuh.iridium.core.result.Unit;
import dev.yyuh.iridium.web.WebServer;
import dev.yyuh.iridium.web.response.ResponseWriter;
import io.undertow.Undertow;
import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public final class UndertowWebServer implements WebServer {

    private final ResponseWriter responseWriter;

    @Override
    public Result<Unit, Exception> start() {
        return Result.of(() -> {
            final Undertow undertow = Undertow.builder()
                    .build();

            undertow.start();

            return Unit.INSTANCE;
        });
    }

    @Override
    public Result<Unit, Exception> registerRoutes() {
        return null;
    }
}

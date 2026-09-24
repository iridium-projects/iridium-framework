package cc.asylum.iridium.web.openapi.controller;

import cc.asylum.iridium.web.controller.RestController;
import cc.asylum.iridium.web.openapi.config.OpenAPIConfig;
import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
public class SwaggerUIController {

  private final OpenAPIConfig openAPIConfig;
}

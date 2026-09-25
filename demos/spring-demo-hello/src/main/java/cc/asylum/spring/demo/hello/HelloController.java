package cc.asylum.spring.demo.hello;

import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public final class HelloController {

  private final GreetingService service;

  public HelloController(final GreetingService service) {
    this.service = service;
  }

  @GetMapping("/api/hello")
  public String hello(final @RequestParam(defaultValue = "world") String name) {
    return service.greet(name);
  }

  @GetMapping("/api/hello/{name}")
  public String helloPath(final @PathVariable("name") String name) {
    return service.greet(name);
  }

  @PostMapping("/api/echo")
  public HelloDto echo(final @RequestBody @Valid HelloDto helloDto) {
    return helloDto;
  }
}

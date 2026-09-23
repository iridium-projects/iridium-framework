package cc.asylum.iridium.test.controller;

import cc.asylum.iridium.test.dto.CreateUserRequest;
import cc.asylum.iridium.test.dto.User;
import cc.asylum.iridium.test.service.UserService;
import cc.asylum.iridium.web.controller.RestController;
import cc.asylum.iridium.web.controller.mapping.DELETE;
import cc.asylum.iridium.web.controller.mapping.GET;
import cc.asylum.iridium.web.controller.mapping.HEAD;
import cc.asylum.iridium.web.controller.mapping.OPTIONS;
import cc.asylum.iridium.web.controller.mapping.PATCH;
import cc.asylum.iridium.web.controller.mapping.POST;
import cc.asylum.iridium.web.controller.mapping.PUT;
import cc.asylum.iridium.web.controller.parameter.CookieValue;
import cc.asylum.iridium.web.controller.parameter.PathVariable;
import cc.asylum.iridium.web.controller.parameter.RequestBody;
import cc.asylum.iridium.web.controller.parameter.RequestHeader;
import cc.asylum.iridium.web.controller.parameter.RequestParam;
import cc.asylum.iridium.web.response.Response;
import cc.asylum.iridium.web.router.Request;

import java.util.Optional;

@RestController
public final class UserController {

    private final UserService userService;

    public UserController(final UserService userService) {
        this.userService = userService;

        System.out.println("UserController created");
    }

    @GET("/users")
    public Response<?> list(@RequestParam(value = "limit", defaultValue = "50") final int limit) {
        System.out.println("UserController.list called");
        return Response.ok(userService.list(limit));
    }

    @GET("/users/{id}")
    public Response<?> get(@PathVariable("id") final long id) {
        return userService.get(id)
                .<Response<?>>map(Response::ok)
                .orElseGet(() -> Response.notFound().body("User " + id + " not found"));
    }

    @POST("/users")
    public Response<?> create(@RequestBody final CreateUserRequest request) {
        final User created = userService.create(request);
        return Response.created("/users/" + created.id()).body(created);
    }

    @PUT("/users/{id}")
    public Response<?> update(@PathVariable("id") final long id, @RequestBody final CreateUserRequest request) {
        return userService.update(id, request)
                .<Response<?>>map(Response::ok)
                .orElseGet(() -> Response.notFound().body("User " + id + " not found"));
    }

    @PATCH("/users/{id}/email")
    public Response<?> updateEmail(@PathVariable("id") final long id, @RequestBody final String email) {
        return userService.updateEmail(id, email)
                .<Response<?>>map(Response::ok)
                .orElseGet(() -> Response.notFound().body("User " + id + " not found"));
    }

    @DELETE("/users/{id}")
    public Response<?> delete(@PathVariable("id") final long id) {
        return userService.delete(id)
                ? Response.noContent().build()
                : Response.notFound().body("User " + id + " not found");
    }

    @OPTIONS("/users")
    public Response<?> options() {
        return Response.noContent().header("Allow", "GET, POST, PUT, PATCH, DELETE, OPTIONS").build();
    }

    @HEAD("/users/{id}")
    public Response<?> head(@PathVariable("id") final long id) {
        return userService.get(id).isPresent()
                ? Response.ok().build()
                : Response.notFound().build();
    }

    @GET("/users/{id}/profile")
    public Response<?> profile(
            @PathVariable("id") final long id,
            @RequestHeader("Authorization") final String auth,
            @CookieValue(value = "session", defaultValue = "none") final String session,
            @RequestParam(value = "verbose", required = false) final Optional<Boolean> verbose
    ) {
        return Response.ok("profile of " + id + " auth=" + auth + " session=" + session
                + " verbose=" + verbose.orElse(false));
    }

    @GET("/echo")
    public Response<?> echo(
            @RequestParam("value") final String required,
            @RequestParam(value = "count", required = false) final Integer count,
            @RequestParam(value = "flag", defaultValue = "true") final boolean flag
    ) {
        return Response.ok("required=" + required + " count=" + count + " flag=" + flag);
    }

    @GET("/inspect")
    public Response<?> inspect(final Request request) {
        return Response.ok("method=" + request.method() + " path=" + request.path());
    }
}

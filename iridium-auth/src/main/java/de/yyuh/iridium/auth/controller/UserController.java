package de.yyuh.iridium.auth.controller;

import org.jspecify.annotations.NullMarked;
import de.yyuh.iridium.boot.controller.type.RestController;
import de.yyuh.iridium.boot.request.RequestContext;
import de.yyuh.iridium.boot.request.type.DELETE;
import de.yyuh.iridium.boot.request.type.GET;
import de.yyuh.iridium.boot.request.type.PATCH;
import de.yyuh.iridium.boot.request.type.POST;
import de.yyuh.iridium.boot.response.Response;

@NullMarked
@RestController
public final class UserController {

  public UserController() {
  }

  @GET("/users")
  public Response getUsers(final RequestContext ctx) {
    return Response.ok();
  }

  @GET("/users/{id}")
  public Response getUserById(final RequestContext ctx) {
    return Response.ok();
  }

  @POST("/users")
  public Response createUser(final RequestContext ctx) {
    return Response.ok();
  }

  @PATCH("/users/{id}")
  public Response updateUser(final RequestContext ctx) {
    return Response.ok();
  }

  @PATCH("/users/{id}/password")
  public Response updateUserPassword(final RequestContext ctx) {
    return Response.ok();
  }

  @PATCH("/users/{id}/email")
  public Response updateUserEmail(final RequestContext ctx) {
    return Response.ok();
  }

  @PATCH("/users/{id}/role")
  public Response updateUserRole(final RequestContext ctx) {
    return Response.ok();
  }

  @DELETE("/users/{id}")
  public Response deleteUser(final RequestContext ctx) {
    return Response.ok();
  }
}

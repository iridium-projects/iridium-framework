package de.yyuh.iridium.auth.controller;

import org.jspecify.annotations.NullMarked;
import de.yyuh.iridium.boot.controller.type.RestController;
import de.yyuh.iridium.boot.request.RequestContext;
import de.yyuh.iridium.boot.request.type.DELETE;
import de.yyuh.iridium.boot.request.type.GET;
import de.yyuh.iridium.boot.request.type.POST;
import de.yyuh.iridium.boot.response.Response;

@NullMarked
@RestController
public final class AuthController {

  public AuthController() {
  }

  @POST("/auth/login")
  public Response login(final RequestContext ctx) {
    return Response.ok();
  }

  @POST("/auth/register")
  public Response register(final RequestContext ctx) {
    return Response.ok();
  }

  @POST("/auth/logout")
  public Response logout(final RequestContext ctx) {
    return Response.ok();
  }

  @POST("/auth/refresh")
  public Response refresh(final RequestContext ctx) {
    return Response.ok();
  }

  @POST("/auth/forgot-password")
  public Response forgotPassword(final RequestContext ctx) {
    return Response.ok();
  }

  @POST("/auth/reset-password")
  public Response resetPassword(final RequestContext ctx) {
    return Response.ok();
  }

  @POST("/auth/verify-email")
  public Response verifyEmail(final RequestContext ctx) {
    return Response.ok();
  }

  @GET("/auth/me")
  public Response me(final RequestContext ctx) {
    return Response.ok();
  }

  @DELETE("/auth/account")
  public Response deleteAccount(final RequestContext ctx) {
    return Response.ok();
  }
}

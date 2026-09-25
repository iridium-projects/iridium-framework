package cc.asylum.iridium.codegen.binding;

import cc.asylum.iridium.core.annotation.Internal;
import cc.asylum.iridium.core.util.Strings;
import cc.asylum.iridium.web.controller.Parameters;
import cc.asylum.iridium.web.response.Response;

@Internal
public final class WebRequestValues implements RequestValues {

  private static final String PARAMETERS = Parameters.class.getCanonicalName();
  private static final String RESPONSE = Response.class.getCanonicalName();

  @Override
  public String one(final boolean header, final String name, final String fallback) {
    return PARAMETERS + (header ? ".header" : ".query")
        + "(_request, " + Strings.quote(name) + ", " + fallback + ")";
  }

  @Override
  public String many(final boolean header, final String name) {
    return PARAMETERS + (header ? ".headers" : ".queries") + "(_request, " + Strings.quote(name) + ")";
  }

  @Override
  public String invalid(final String rawVariable, final String param) {
    if (rawVariable == null) {
      return RESPONSE + ".badRequest().body(\"Invalid value for '" + param + "'\")";
    }
    return RESPONSE + ".badRequest().body(\"Invalid value '\" + " + rawVariable + " + \"' for '" + param + "'\")";
  }

}

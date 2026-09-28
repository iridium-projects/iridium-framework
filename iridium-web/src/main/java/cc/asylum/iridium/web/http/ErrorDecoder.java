package cc.asylum.iridium.web.http;

@FunctionalInterface
public interface ErrorDecoder {

  Exception decode(final int status, final String reason, final byte[] body);

  ErrorDecoder DEFAULT = (status, reason, body) -> new IllegalStateException(status + " " + reason + (body == null || body.length == 0 ? "" : ": " + new String(body)));
}

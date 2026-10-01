package cc.asylum.iridium.core.html;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

final class HtmlTest {

  @Test
  void escapeReplacesMarkup() {
    assertNull(Html.escape(null));
    assertEquals("a&amp;b&lt;c&gt;d&quot;e&#39;", Html.escape("a&b<c>d\"e'"));
    assertEquals("plain", Html.escape("plain"));
  }
}

package cc.asylum.iridium.core.html;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class HtmlTest {

  @Test
  void escapesNullAndPlainText() {
    assertNull(Html.escape(null));
    assertEquals("", Html.escape(""));
    assertEquals("plain", Html.escape("plain"));
  }

  @Test
  void escapesMarkupAndQuotesInOrder() {
    assertEquals("&amp;&lt;&gt;&quot;&#39;", Html.escape("&<>\"'"));
    assertEquals("&amp;amp;", Html.escape("&amp;"));
    assertEquals("a&amp;b&lt;c&gt;d&quot;e&#39;f", Html.escape("a&b<c>d\"e'f"));
  }
}

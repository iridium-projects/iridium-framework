package de.yyuh.iridium.core.html;

import de.yyuh.iridium.core.annotation.Internal;

@Internal
public final class Html {

    private Html() {
    }

    public static String escape(final String value) {
        if (value == null) {
            return null;
        }

        return value
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&#39;");
    }
}

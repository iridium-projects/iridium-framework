package cc.asylum.iridium.web;

import cc.asylum.iridium.core.annotation.Internal;

@Internal
public final class Banner {

  private static final String RESET = "\u001B[0m";
  private static final String GRAY = "\u001B[38;2;209;213;219m";
  private static final String TEAL = "\u001B[38;2;94;234;212m";
  private static final String WHITE = "\u001B[38;2;248;250;252m";
  private static final String BOLD_WHITE = WHITE + "\u001B[1m";

  private static final String[] SLOGAN = {
      "Iridium",
      "Hardwired Java"
  };

  private static final int LEFT = 8;
  private static final int MARK_WIDTH = 40;
  private static final int SLOGAN_START = 6;

  private static final String[] MARK = {
      "             .;.                .;.",
      "              ,odc'.         'cdo,",
      "          ..     .:odoolloodo:.     ..",
      "          .lo.         ..         .xd.",
      "            'k,                  ,0,",
      "             .k'     lOXX0l.    '0.",
      "              cd    OMMMMMM0    xl",
      "              cd    OMMMMMM0    xo",
      "             .k'     cOKXOl.    .0.",
      "            'k'                  '0;",
      "          .lo.         ..         .dd.",
      "           .     .;lllcccclll;.     ..",
      "              ,lo:.          .:ol,",
      "             .,.                .,. "
  };

  private Banner() {
  }

  public static void print() {
    final StringBuilder out = new StringBuilder(1024);

    out.append('\n');

    for (int y = 0; y < MARK.length; y++) {
      final String line = MARK[y];

      appendPainted(out, line, y);
      out.append(" ".repeat(MARK_WIDTH - line.length() + 3));

      if (y >= SLOGAN_START && y < SLOGAN_START + SLOGAN.length) {
        final int index = y - SLOGAN_START;

        out.append(index == 0 ? BOLD_WHITE : GRAY)
            .append(SLOGAN[index])
            .append(RESET);
      }

      out.append('\n');
    }

    out.append('\n');

    System.out.print(out);
  }

  private static void appendPainted(
      final StringBuilder out,
      final String line,
      final int y) {
    final boolean top = y <= 2;
    final boolean bottom = y >= 11;

    for (int x = LEFT, length = line.length(); x < length; x++) {
      final char glyph = line.charAt(x);

      if (glyph == ' ') {
        out.append(' ');
        continue;
      }

      final String color;

      if (x >= 20 && x <= 27 && y >= 5 && y <= 8) {
        color = WHITE;
      } else if (top) {
        color = GRAY;
      } else if (bottom) {
        color = TEAL;
      } else {
        color = x < 24 ? TEAL : GRAY;
      }

      out.append(color)
          .append(glyph)
          .append(RESET);
    }
  }
}

package cc.asylum.iridium.web;

public final class Banner {

  private static final String RESET = "\u001B[0m";
  private static final String GRAY = "\u001B[38;2;209;213;219m";
  private static final String TEAL = "\u001B[38;2;94;234;212m";
  private static final String WHITE = "\u001B[38;2;248;250;252m";
  private static final String[] SLOGAN = { "Iridium", "Hardwired Java" };
  private static final int LEFT = 8;

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
      "             .,.                .,."
  };

  private Banner() {
  }

  public static void print() {
    final int markWidth = markWidth();
    final int start = (MARK.length - SLOGAN.length) / 2;

    System.out.println();

    for (int y = 0; y < MARK.length; y++) {
      final String line = MARK[y];
      System.out.print(paint(line, y));
      System.out.print(" ".repeat(markWidth - line.length() + 3));

      final int index = y - start;
      if (index >= 0 && index < SLOGAN.length) {
        System.out.print(slogan(SLOGAN[index], index == 0));
      }

      System.out.println();

    }
    System.out.println();
  }

  private static String paint(final String line, final int y) {
    final StringBuilder out = new StringBuilder();

    for (int x = LEFT; x < line.length(); x++) {
      final char glyph = line.charAt(x);

      if (glyph == ' ') {
        out.append(' ');
        continue;
      }

      out.append(foreground(x, y)).append(glyph).append(RESET);
    }

    return out.toString();
  }

  private static String slogan(final String text, final boolean title) {
    return (title ? WHITE + "\u001B[1m" : GRAY) + text + RESET;
  }

  private static int markWidth() {
    int width = 0;
    for (final String line : MARK) {
      width = Math.max(width, line.length());
    }
    return width;
  }

  private static String foreground(final int x, final int y) {
    if (x >= 20 && x <= 27 && y >= 5 && y <= 8) {
      return WHITE;
    }
    if (y <= 2) {
      return GRAY;
    }
    if (y >= 11) {
      return TEAL;
    }
    return x < 24 ? TEAL : GRAY;
  }
}

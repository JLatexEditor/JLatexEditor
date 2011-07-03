package util;

/**
 * This is a PrintStream with colored/styled output.
 */
public class Shell {

// static

  public enum Colored {on, auto, off}

  // variables for the style of the text
  public final static int STYLE_NORMAL      = 0;
  public final static int STYLE_BOLD        = 1;
  public final static int STYLE_LIGHT = 1;
  public final static int STYLE_UNDERLINE   = 4;
  public final static int STYLE_FLASHING    = 5;
  public final static int STYLE_INVERT      = 7;

  // variables for color
  public final static int COLOR_BLACK       = 30;
  public final static int COLOR_RED         = 31;
  public final static int COLOR_GREEN       = 32;
  public final static int COLOR_YELLOW      = 33;
  public final static int COLOR_BLUE        = 34;
  public final static int COLOR_VIOLETT     = 35;
  public final static int COLOR_CYAN        = 36;
  public final static int COLOR_WHITE       = 37;

  // variables for color
  public final static int BGCOLOR_BLACK     = 40;
  public final static int BGCOLOR_RED       = 41;
  public final static int BGCOLOR_GREEN     = 42;
  public final static int BGCOLOR_YELLOW    = 43;
  public final static int BGCOLOR_BLUE      = 44;
  public final static int BGCOLOR_VIOLETT   = 45;
  public final static int BGCOLOR_CYAN      = 46;
  public final static int BGCOLOR_WHITE     = 47;

	private static boolean colored = false;

  /**
   * Tests if the terminal supports colored styled output and checks if the user
   * has set the command line argument "--color".
   *
   * @param args command line arguments
   * @return true if colored
   */
  public static boolean setColoredOrNot(String args[]) {
    // default
    Colored colored = Colored.auto;

    // check if "--color" is specified
    for (String s : args) {
      if(s.startsWith("--color=")) {
        String valueString = s.substring("--color=".length());
        try {
          colored = Colored.valueOf(valueString);
        } catch(IllegalArgumentException ignored) { }
        break;
      }
    }

    return setColoredOrNot(colored);
  }

  public static boolean setColoredOrNot(Colored c) {
    switch(c) {

      case on:
        colored = true;
        break;

      case off:
        colored = false;
        break;

      case auto:
        String term = System.getenv("TERM");
        if(term != null) {
          if(term.startsWith("xterm") || term.startsWith("rxvt")) {
            colored = true;
          }
        }
        break;
    }

	  return colored;
  }

  public static boolean isColored() {
    return colored;
  }

	public static String bold(String text) {
		return style(text, STYLE_BOLD);
	}
	public static String underline(String text) {
		return style(text, STYLE_UNDERLINE);
	}

  /**
   * Returns the styled/colored text.
   *
   * @param text input text
   * @param styles styles
   * @return styled/colored text
   */
  public static String style(String text, int... styles) {
    if(!colored) return text;

    String styleString = "" + styles[0];
    for (int i = 1; i < styles.length; i++) {
      styleString += ";" + styles[i];
    }

    return '\033'+"[" + styleString + "m" + text + '\033' + "[0m";
  }

  public static String resetStyle() {
    return colored ? '\033' + "[0m" : "";
  }

  public static String clearScreen() {
    return colored ? '\033' + "[2J" : "";
  }

  public static String clearToEndOfLine() {
	  return colored ? '\033' + "[K" : "";
  }
}

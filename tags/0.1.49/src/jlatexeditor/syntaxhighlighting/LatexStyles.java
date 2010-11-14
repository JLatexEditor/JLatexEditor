/**
 * @author Jörg Endrullis
 */

package jlatexeditor.syntaxhighlighting;

import jlatexeditor.gproperties.StyleLoader;
import sce.component.SCEDocument;

import java.awt.font.TextAttribute;
import java.util.HashMap;
import java.util.Map;

public class LatexStyles {
  public static final byte TEXT = 0;
  public static final byte COMMAND = 1;
  public static final byte COMMENT = 2;

  public static final byte BRACKET = 3;

  public static final byte NUMBER = 4;

  public static final byte MATH = 5;
  public static final byte MATH_COMMAND = 6;

  public static final byte TODO = 7;

  public static final byte ERROR = 8;

  public static final byte USER = 50;

  public static final byte U_NORMAL = 100;
  public static final byte U_MISSPELLED = 101;

  private static Map<TextAttribute, Object>[] stylesMap;

  private static Map<String, Byte> name2Id = new HashMap<String, Byte>();

  private static String styleFile = "data/styles/user.xml";

  static {
	  init();
    load();
  }

  public static void init() {
    name2Id.put("text", TEXT);
    name2Id.put("command", COMMAND);
    name2Id.put("comment", COMMENT);
    name2Id.put("bracket", BRACKET);
    name2Id.put("number", NUMBER);
    name2Id.put("math", MATH);
    name2Id.put("math_command", MATH_COMMAND);
    name2Id.put("todo", TODO);
    name2Id.put("error", ERROR);
    name2Id.put("user", USER);
  }

	public static void load() {
	  stylesMap = StyleLoader.load(styleFile, name2Id);
	}

  public static void addStyles(SCEDocument document) {
    Map<TextAttribute, Object> styleText = stylesMap[TEXT];
    for (int id = 0; id < stylesMap.length; id++) {
      if (stylesMap[id] == null) continue;
      Map<TextAttribute, Object> style = document.addStyle((byte) id, styleText);
      style.putAll(stylesMap[id]);
    }

    //// underline styles ////

    // normal
    Map<TextAttribute, Object> underlineNormal = document.addStyle(U_NORMAL, null);

    // misspelling
    Map<TextAttribute, Object> underlineMisspelling = document.addStyle(U_MISSPELLED, null);
    underlineMisspelling.put(TextAttribute.UNDERLINE, TextAttribute.UNDERLINE_LOW_GRAY);
  }

	public static Byte getStyle(String style) {
		return name2Id.get(style);
	}

  public static void save(String fileName) {
    // TODO
  }
}

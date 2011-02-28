/**
 * @author Jörg Endrullis
 */

package jlatexeditor.changelog;

import jlatexeditor.gproperties.StyleLoader;
import sce.component.SCEDocument;

import java.awt.font.TextAttribute;
import java.util.HashMap;
import java.util.Map;

public class ChangeLogStyles {
  public static final byte TEXT = 0;
  public static final byte ITEM1 = 1;
  public static final byte ITEM2 = 2;
  public static final byte ITEM3 = 3;
	public static final byte HEADLINE1 = 4;
	public static final byte HEADLINE2 = 5;
  public static final byte UNIMPORTANT = 6;
  public static final byte COMMENT = 7;
  public static final byte SHORTCUT = 8;
  public static final byte MENU = 9;
  public static final byte STRING = 10;

  public static final byte USER = 50;

  public static final byte U_NORMAL = 100;
  public static final byte U_MISSPELLED = 101;

  private static Map<TextAttribute, Object>[] stylesMap;

  private static Map<String, Byte> name2Id = new HashMap<String, Byte>();

  private static String styleFile = "data/changelog/styles/user.xml";

  static {
	  init();
	  load();
  }

  public static void init() {
    name2Id.put("text", TEXT);
    name2Id.put("item1", ITEM1);
    name2Id.put("item2", ITEM2);
    name2Id.put("item3", ITEM3);
    name2Id.put("headline1", HEADLINE1);
    name2Id.put("headline2", HEADLINE2);
    name2Id.put("unimportant", UNIMPORTANT);
    name2Id.put("comment", COMMENT);
	  name2Id.put("shortcut", SHORTCUT);
	  name2Id.put("menu", MENU);
	  name2Id.put("string", STRING);
    name2Id.put("user", USER);
  }

	public static void load() {
	  stylesMap = StyleLoader.load(styleFile, name2Id);
	}

	public static void save(String fileName) {
	  // TODO
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
}

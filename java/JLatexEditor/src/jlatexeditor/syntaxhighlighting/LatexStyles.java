
/**
 * @author Jörg Endrullis
 */

package jlatexeditor.syntaxhighlighting;

import jlatexeditor.GProperties;
import my.XML.XMLDocument;
import my.XML.XMLElement;
import my.XML.XMLParser;
import sce.component.SCEDocument;
import util.StreamUtils;

import java.awt.*;
import java.awt.font.TextAttribute;
import java.util.HashMap;
import java.util.Map;

public class LatexStyles{
  public static final byte TEXT = 0;
  public static final byte COMMAND = 1;
  public static final byte COMMENT = 2;

  public static final byte BRACKET = 3;

  public static final byte NUMBER = 4;

  public static final byte MATH = 5;

  public static final byte ERROR = 6;

  public static final byte USER = 50;

  public static final byte U_NORMAL     = 100;
  public static final byte U_MISSPELLED = 101;

  private static Map<TextAttribute, Object>[] stylesMap = new Map[Byte.MAX_VALUE+1];
  private static Map<String, Byte> commandsMap = new HashMap<String, Byte>();

  private static Map<String,Byte> name2Id = new HashMap<String, Byte>();

  static {
    init();
    load("data/styles/user.xml");
  }

  private static void init() {
    name2Id.put("text", TEXT);
    name2Id.put("command", COMMAND);
    name2Id.put("comment", COMMENT);
    name2Id.put("bracket", BRACKET);
    name2Id.put("number", NUMBER);
    name2Id.put("math", MATH);
    name2Id.put("error", ERROR);

    for(int i = 0; i < stylesMap.length; i++) stylesMap[i] = null;
  }

	public static void addStyles(SCEDocument document){
    Map<TextAttribute, Object> styleText = stylesMap[TEXT];
    for(int id = 0; id < stylesMap.length; id++) {
      if(stylesMap[id] == null) continue;
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

  public static Byte getCommandStyle(String command) {
    Byte style = commandsMap.get(command);
    return style != null ? style : COMMAND;
  }

  public static void load(String filename) {
    init();

    XMLParser xmlParser = new XMLParser();
    XMLDocument stylesDocument;
    try {
      stylesDocument = xmlParser.parse(StreamUtils.readFile(filename));
    } catch (Exception e) {
      e.printStackTrace();
      return;
    }

    XMLElement stylesXML = stylesDocument.getRootElement();
    if(!stylesXML.getName().equalsIgnoreCase("styles")){
      System.out.println("Error in [styles].xml: root element must be 'styles'");
      return;
    }

    int userStyle = USER;
    for(XMLElement styleElement : stylesXML.getChildElements()) {
      if(styleElement.getName().equals("special")) {
        for(XMLElement element : styleElement.getChildElements()) {
          if(element.getName().equals("command")) {
            commandsMap.put(element.getAttribute("name"), name2Id.get(element.getAttribute("style")));
          }
        }
        continue;
      }

      Byte id = name2Id.get(styleElement.getAttribute("name"));
      if(id == null) {
        id = (byte) userStyle++;
        name2Id.put(styleElement.getAttribute("name"), id);
      }

      stylesMap[id] = getStyle(styleElement);
    }
  }

  public static void save(String fileName) {
    // TODO
  }

  private static Map<TextAttribute, Object> getStyle(XMLElement styleElement) {
    Map<TextAttribute, Object> style = new HashMap<TextAttribute, Object>();

    Font font = GProperties.getEditorFont();
    Font fontBold = GProperties.getEditorFont().deriveFont(Font.BOLD);
    Font fontItalic = GProperties.getEditorFont().deriveFont(Font.ITALIC);

    style.put(TextAttribute.FONT, font);
    if(styleElement.getAttribute("style").equals("bold")) style.put(TextAttribute.FONT, fontBold);
    if(styleElement.getAttribute("style").equals("italic")) style.put(TextAttribute.FONT, fontItalic);

    XMLElement foreground = styleElement.getChildElement("foreground");
    if(foreground != null) {
      style.put(TextAttribute.FOREGROUND, getColor(foreground.getChildElement("color")));
    }

    XMLElement background = styleElement.getChildElement("background");
    if(background != null) {
      style.put(TextAttribute.BACKGROUND, getColor(background.getChildElement("color")));
    }

    return style;
  }

  private static Color getColor(XMLElement color) {
    int r = Integer.parseInt(color.getAttribute("r"));
    int g = Integer.parseInt(color.getAttribute("g"));
    int b = Integer.parseInt(color.getAttribute("b"));
    return new Color(r,g,b);
  }
}

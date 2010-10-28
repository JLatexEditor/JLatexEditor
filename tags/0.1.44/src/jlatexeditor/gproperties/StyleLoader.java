package jlatexeditor.gproperties;

import my.XML.XMLDocument;
import my.XML.XMLElement;
import my.XML.XMLParser;
import util.StreamUtils;

import java.awt.*;
import java.awt.font.TextAttribute;
import java.util.HashMap;
import java.util.Map;

/**
 * Loads the font styles from a file.
 *
 * @author Stefan Endrullis &lt;stefan@endrullis.de&gt;
 */
public class StyleLoader {
	public static Map<TextAttribute, Object>[] load(String filename, Map<String, Byte> name2Id) {
		Map<TextAttribute, Object>[] stylesMap = new Map[Byte.MAX_VALUE + 1];
		for (int i = 0; i < stylesMap.length; i++) stylesMap[i] = null;

	  XMLParser xmlParser = new XMLParser();
	  XMLDocument stylesDocument;
	  try {
	    stylesDocument = xmlParser.parse(StreamUtils.readFile(filename));
	  } catch (Exception e) {
	    e.printStackTrace();
	    return stylesMap;
	  }

	  XMLElement stylesXML = stylesDocument.getRootElement();
	  if (!stylesXML.getName().equalsIgnoreCase("styles")) {
	    System.out.println("Error in [styles].xml: root element must be 'styles'");
	    return stylesMap;
	  }

	  int userStyle = name2Id.get("user");
	  for (XMLElement styleElement : stylesXML.getChildElements()) {
	    Byte id = name2Id.get(styleElement.getAttribute("name"));
	    if (id == null) {
	      id = (byte) userStyle++;
	      name2Id.put(styleElement.getAttribute("name"), id);
	    }

	    stylesMap[id] = getStyle(styleElement);
	  }

		return stylesMap;
	}

	private static Map<TextAttribute, Object> getStyle(XMLElement styleElement) {
		Map<TextAttribute, Object> style = new HashMap<TextAttribute, Object>();

		Font font = GProperties.getEditorFont();
		Font fontBold = GProperties.getEditorFont().deriveFont(Font.BOLD);
		Font fontItalic = GProperties.getEditorFont().deriveFont(Font.ITALIC);
		Font fontBoldItalic = GProperties.getEditorFont().deriveFont(Font.BOLD | Font.ITALIC);

		style.put(TextAttribute.FONT, font);
		if (styleElement.getAttribute("style").equals("bold")) style.put(TextAttribute.FONT, fontBold);
		if (styleElement.getAttribute("style").equals("italic")) style.put(TextAttribute.FONT, fontItalic);
		if (styleElement.getAttribute("style").equals("bold,italic") ||
		        styleElement.getAttribute("style").equals("italic,bold")) style.put(TextAttribute.FONT, fontBoldItalic);

		String foreground = styleElement.getAttribute("foreground");
		if (foreground != null) {
		  style.put(TextAttribute.FOREGROUND, hex2color(foreground));
		}

		String background = styleElement.getAttribute("background");
		if (background != null) {
		  style.put(TextAttribute.BACKGROUND, hex2color(background));
		}

		return style;
	}

	private static Color hex2color(String hexColor) {
		return new Color(Integer.parseInt(hexColor, 16));
	}
}

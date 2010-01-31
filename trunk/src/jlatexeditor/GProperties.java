package jlatexeditor;

import de.endrullis.utils.BetterProperties2;
import de.endrullis.utils.BetterProperties2.Comment;
import de.endrullis.utils.BetterProperties2.Def;
import de.endrullis.utils.BetterProperties2.Range;
import de.endrullis.utils.BetterProperties2.PSet;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;

/**
 * Global properties for the editor.
 */
public class GProperties {
	public static final File CONFIG_FILE = new File(System.getProperty("user.home") + "/.jlatexeditor/global.properties");
  public static HashMap<String,Object> TEXT_ANTIALIAS_MAP = new HashMap<String, Object>();
  public static String[] TEXT_ANTIALIAS_KEYS;

  private static ArrayList<String> MONOSPACE_FONTS = new ArrayList<String>();

	private static BetterProperties2 properties = new BetterProperties2();
	
  // properties
  private static Font editorFont;
  private static Object textAntiAliasing = RenderingHints.VALUE_TEXT_ANTIALIAS_OFF;

	public static final Range INT     = BetterProperties2.INT;
	public static final Range INT_GT0 = BetterProperties2.INT_GT0;
	public static final Range DOUBLE  = BetterProperties2.DOUBLE;
	public static final Range BOOLEAN = BetterProperties2.BOOLEAN;
	public static final Range STRING  = BetterProperties2.STRING;

	private static final String EDITOR_FONT_NAME = "editor.font.name";
	private static final String EDITOR_FONT_SIZE = "editor.font.size";
	private static final String EDITOR_FONT_ANTIALIASING = "editor.font.antialiasing";

	static {
    // text anti-aliasing
    TEXT_ANTIALIAS_KEYS = new String[] {"On", "Off", "GASP", "LCD HBGR", "LCD HRGB", "LCD VBGR", "LCD VRGB"};

    TEXT_ANTIALIAS_MAP.put("On", RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
    TEXT_ANTIALIAS_MAP.put("Off", RenderingHints.VALUE_TEXT_ANTIALIAS_OFF);
    TEXT_ANTIALIAS_MAP.put("GASP", RenderingHints.VALUE_TEXT_ANTIALIAS_GASP);
    TEXT_ANTIALIAS_MAP.put("LCD HBGR", RenderingHints.VALUE_TEXT_ANTIALIAS_LCD_HBGR);
    TEXT_ANTIALIAS_MAP.put("LCD HRGB", RenderingHints.VALUE_TEXT_ANTIALIAS_LCD_HRGB);
    TEXT_ANTIALIAS_MAP.put("LCD VBGR", RenderingHints.VALUE_TEXT_ANTIALIAS_LCD_VBGR);
    TEXT_ANTIALIAS_MAP.put("LCD VRGB", RenderingHints.VALUE_TEXT_ANTIALIAS_LCD_VRGB);

    // monospace fonts
    GraphicsEnvironment graphicsEnvironment = GraphicsEnvironment.getLocalGraphicsEnvironment();
    String[] fontNames = graphicsEnvironment.getAvailableFontFamilyNames();

    BufferedImage image = new BufferedImage(64, 64, BufferedImage.TYPE_INT_RGB);
    Graphics graphics = image.createGraphics();
    for(String fontName : fontNames) {
      Font font = new Font(fontName, Font.PLAIN, 13);
      FontMetrics fontMetrics = graphics.getFontMetrics(font);
      double widthW = fontMetrics.getStringBounds("W", graphics).getWidth();
      double widthi = fontMetrics.getStringBounds("i", graphics).getWidth();
      if(widthi == widthW) {
        MONOSPACE_FONTS.add(fontName);
      }
    }

		String[] MONOSPACE_FONTS_ARRAY = new String[MONOSPACE_FONTS.size()];
		MONOSPACE_FONTS.toArray(MONOSPACE_FONTS_ARRAY);

	  // set default for the properties file
	  properties.addEntry(new Comment(" Font settings"));
	  properties.addEntry(new Def(EDITOR_FONT_NAME, new PSet(MONOSPACE_FONTS_ARRAY), "Monospaced"));
	  properties.addEntry(new Def(EDITOR_FONT_SIZE, INT_GT0, "13"));
	  properties.addEntry(new Def(EDITOR_FONT_ANTIALIASING, new PSet(TEXT_ANTIALIAS_KEYS), "Off"));
	  //properties.addEntry(new Def("xwinfo", STRING, null, "xwinfo/xwinfo"));

		load();
		save();
  }

	public static Def getDef(String key) {
		return properties.getDefMap().get(key);
	}

	public static Range getRange(String key) {
		return properties.getDefMap().get(key).getRange();
	}

	public static void load() {
		if (CONFIG_FILE.exists()) {
			try {
				properties.load(new FileReader(CONFIG_FILE));
			} catch (IOException e) {
				properties.loadDefaults();
				e.printStackTrace();
			}
		} else {
			properties.loadDefaults();
		}

		extractProperties();
	}

	private static void extractProperties() {
		editorFont = new Font(properties.getProperty(EDITOR_FONT_NAME), 0, properties.getInt(EDITOR_FONT_SIZE));
		textAntiAliasing = TEXT_ANTIALIAS_MAP.get(properties.getProperty(EDITOR_FONT_ANTIALIASING));
	}

	public static void save() {
		CONFIG_FILE.getParentFile().mkdirs();
		try {
			properties.store(new FileOutputStream(CONFIG_FILE),
					" JLatexEditor properties - use control+space to get completion for values\n"+
					" Default values are commented out\n");
		} catch (Exception e) {
			e.printStackTrace();
		}
	}


  public static ArrayList<String> getMonospaceFonts() {
    return MONOSPACE_FONTS;
  }

  public static Object getTextAntiAliasing() {
    return textAntiAliasing;
  }

  public static void setTextAntiAliasing(Object textAntiAliasing) {
    GProperties.textAntiAliasing = textAntiAliasing;
	  // TODO: set the value in the properties
	  save();
  }

  public static Font getEditorFont() {
    return editorFont;
  }

  public static void setEditorFont(Font editorFont) {
    GProperties.editorFont = editorFont;
	  properties.setProperty(EDITOR_FONT_NAME, editorFont.getPSName());
	  properties.setProperty(EDITOR_FONT_SIZE, "" + editorFont.getSize());
	  save();
  }
}

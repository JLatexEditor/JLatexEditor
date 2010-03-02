package jlatexeditor.gproperties;

import de.endrullis.utils.BetterProperties2;
import de.endrullis.utils.BetterProperties2.Comment;
import de.endrullis.utils.BetterProperties2.Def;
import de.endrullis.utils.BetterProperties2.Range;
import de.endrullis.utils.BetterProperties2.PSet;
import util.Aspell;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.util.*;
import java.util.List;

/**
 * Global properties for the editor.
 */
public class GProperties {
  public static final File CONFIG_FILE = new File(System.getProperty("user.home") + "/.jlatexeditor/global.properties");
  public static HashMap<String, Object> TEXT_ANTIALIAS_MAP = new HashMap<String, Object>();
  public static String[] TEXT_ANTIALIAS_KEYS;

  private static ArrayList<String> monospaceFonts = new ArrayList<String>();

  private static BetterProperties2 properties = new BetterProperties2();
  private static HashMap<String, Set<PropertyChangeListener>> changeListeners = new HashMap<String, Set<PropertyChangeListener>>();

  // properties
  private static Font editorFont;
  private static Object textAntiAliasing = RenderingHints.VALUE_TEXT_ANTIALIAS_OFF;

  public static final Range INT = BetterProperties2.INT;
  public static final Range INT_GT_0 = BetterProperties2.INT_GT_0;
  public static final Range DOUBLE = BetterProperties2.DOUBLE;
  public static final Range DOUBLE_0_TO_1 = BetterProperties2.DOUBLE_0_TO_1;
  public static final Range BOOLEAN = BetterProperties2.BOOLEAN;
  public static final Range STRING = BetterProperties2.STRING;
  public static final Range SHORTCUT = BetterProperties2.SHORTCUT;

  private static final String EDITOR_FONT_NAME = "editor.font.name";
  private static final String EDITOR_FONT_SIZE = "editor.font.size";
  private static final String EDITOR_FONT_ANTIALIASING = "editor.font.antialiasing";

  private static final String ASPELL_LANG = "aspell.lang";

  static {
    // text anti-aliasing
    TEXT_ANTIALIAS_KEYS = new String[]{"On", "Off", "GASP", "LCD HBGR", "LCD HRGB", "LCD VBGR", "LCD VRGB"};

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
    for (String fontName : fontNames) {
      Font font = new Font(fontName, Font.PLAIN, 13);
      FontMetrics fontMetrics = graphics.getFontMetrics(font);
      double widthW = fontMetrics.getStringBounds("W", graphics).getWidth();
      double widthi = fontMetrics.getStringBounds("i", graphics).getWidth();
      if (widthi == widthW) {
        monospaceFonts.add(fontName);
      }
    }

    String[] MONOSPACE_FONTS_ARRAY = new String[monospaceFonts.size()];
    monospaceFonts.toArray(MONOSPACE_FONTS_ARRAY);

    List<String> dictList;
    try {
      dictList = Aspell.availableDicts();
    } catch (IOException e) {
      dictList = new ArrayList<String>();
    }
    String[] aspellDicts = new String[dictList.size()];
    dictList.toArray(aspellDicts);

    // set default for the properties file
    properties.addEntry(new Comment(" Check for updates"));
    properties.addEntry(new Def("check_for_updates", BOOLEAN, "true"));
    properties.addEntry(new Comment(" Font settings"));
    properties.addEntry(new Def(EDITOR_FONT_NAME, new PSet(MONOSPACE_FONTS_ARRAY), "Monospaced"));
    properties.addEntry(new Def(EDITOR_FONT_SIZE, INT_GT_0, "13"));
    properties.addEntry(new Def(EDITOR_FONT_ANTIALIASING, new PSet(TEXT_ANTIALIAS_KEYS), "Off"));
    properties.addEntry(new Def("editor.columns_per_row", INT_GT_0, "80"));
    properties.addEntry(new Comment(" Spell checker settings"));
    properties.addEntry(new Def(ASPELL_LANG, new PSet(aspellDicts), "en_GB"));
    properties.addEntry(new Comment(" Width of the symbols panel as part of the main window"));
    properties.addEntry(new Def("symbols_panel.width", INT_GT_0, "220"));
    properties.addEntry(new Comment(" Height of the tools panel as part of the main window"));
    properties.addEntry(new Def("tools_panel.height", DOUBLE_0_TO_1, "0.15"));
    properties.addEntry(new Comment(" Number of parent directories of the open file shown in the window title"));
    properties.addEntry(new Def("window.title.number_of_parent_dirs_shown", INT_GT_0, "2"));

    properties.addEntry(new Comment("\n## Shortcuts"));
    properties.addEntry(new Comment(" File menu"));
    properties.addEntry(new Def("shortcut.new", SHORTCUT, "control N"));
    properties.addEntry(new Def("shortcut.open", SHORTCUT, "control O"));
    properties.addEntry(new Def("shortcut.save", SHORTCUT, "control S"));
    properties.addEntry(new Def("shortcut.close", SHORTCUT, "control W"));
    properties.addEntry(new Def("shortcut.exit", SHORTCUT, ""));
    properties.addEntry(new Comment(" Edit menu"));
    properties.addEntry(new Def("shortcut.undo", SHORTCUT, "control Z"));
    properties.addEntry(new Def("shortcut.redo", SHORTCUT, "control shift Z"));
    properties.addEntry(new Def("shortcut.find", SHORTCUT, "control F"));
    properties.addEntry(new Def("shortcut.replace", SHORTCUT, "control R"));
    properties.addEntry(new Def("shortcut.find next", SHORTCUT, "F3"));
    properties.addEntry(new Def("shortcut.find previous", SHORTCUT, "shift F3"));
    properties.addEntry(new Def("shortcut.cut", SHORTCUT, "control X"));
    properties.addEntry(new Def("shortcut.copy", SHORTCUT, "control C"));
    properties.addEntry(new Def("shortcut.paste", SHORTCUT, "control V"));
    properties.addEntry(new Def("shortcut.comment", SHORTCUT, "control D"));
    properties.addEntry(new Def("shortcut.uncomment", SHORTCUT, "control shift D"));
    properties.addEntry(new Def("shortcut.diff", SHORTCUT, "alt D"));
    properties.addEntry(new Comment(" View"));
    properties.addEntry(new Def("shortcut.symbols", SHORTCUT, "alt Y"));
    properties.addEntry(new Def("shortcut.status bar", SHORTCUT, ""));
    properties.addEntry(new Def("shortcut.status bar", SHORTCUT, ""));
    properties.addEntry(new Def("shortcut.compile", SHORTCUT, ""));
    properties.addEntry(new Def("shortcut.local history", SHORTCUT, ""));
    properties.addEntry(new Comment(" Build menu"));
    properties.addEntry(new Def("shortcut.pdf", SHORTCUT, "alt 1"));
    properties.addEntry(new Def("shortcut.dvi", SHORTCUT, "alt 2"));
    properties.addEntry(new Def("shortcut.dvi + ps", SHORTCUT, "alt 3"));
    properties.addEntry(new Def("shortcut.dvi + ps + pdf", SHORTCUT, "alt 4"));
    properties.addEntry(new Comment(" Version control menu"));
    properties.addEntry(new Def("shortcut.svn update", SHORTCUT, "alt U"));
    properties.addEntry(new Def("shortcut.svn commit", SHORTCUT, "alt C"));
    properties.addEntry(new Def("shortcut.font", SHORTCUT, ""));
    properties.addEntry(new Def("shortcut.global settings", SHORTCUT, "control alt S"));
    properties.addEntry(new Comment(" Next/previous tab"));
    properties.addEntry(new Def("shortcut.next tab", SHORTCUT, "alt RIGHT"));
    properties.addEntry(new Def("shortcut.previous tab", SHORTCUT, "alt LEFT"));
    properties.addEntry(new Comment(" Initiate update"));
    properties.addEntry(new Def("shortcut.update", SHORTCUT, ""));
    properties.addEntry(new Comment(" About screen"));
    properties.addEntry(new Def("shortcut.about", SHORTCUT, ""));

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
    Hashtable<Object, Object> oldMap = (Hashtable<Object, Object>) properties.clone();

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

    // check what values have been changed and inform listeners
    for (String key : changeListeners.keySet()) {
      Object oldValue = oldMap.get(key);
      Object newValue = properties.get(key);
      if (newValue == null && oldValue == null) continue;

      if ((newValue == null && oldValue != null) ||
              (newValue != null && oldValue == null) ||
              !newValue.equals(oldValue)) {
        for (PropertyChangeListener propertyChangeListener : changeListeners.get(key)) {
          propertyChangeListener.propertyChange(new PropertyChangeEvent(CONFIG_FILE, key, oldValue, newValue));
        }
      }
    }
  }

  private static void extractProperties() {
    editorFont = new Font(properties.getProperty(EDITOR_FONT_NAME), Font.PLAIN, properties.getInt(EDITOR_FONT_SIZE));
    textAntiAliasing = TEXT_ANTIALIAS_MAP.get(properties.getProperty(EDITOR_FONT_ANTIALIASING));
  }

  public static void save() {
    CONFIG_FILE.getParentFile().mkdirs();
    try {
      properties.store(new FileOutputStream(CONFIG_FILE),
              " JLatexEditor properties\n" +
                      " Use control+space to get completion for property values.\n" +
                      " Default values will be automatically commented out.\n");
    } catch (Exception e) {
      e.printStackTrace();
    }
  }


  public static ArrayList<String> getMonospaceFonts() {
    return monospaceFonts;
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
    properties.setProperty(EDITOR_FONT_NAME, editorFont.getFamily());
    properties.setProperty(EDITOR_FONT_SIZE, "" + editorFont.getSize());
    save();
  }

  public static void set(String key, String value) {
    properties.setProperty(key, value);
    save();
  }

  public static String getAspellLang() {
    return properties.getString(ASPELL_LANG);
  }

  public static String getString(String key) {
    return properties.getString(key);
  }

  public static boolean getBoolean(String key) {
    return properties.getBoolean(key);
  }

  public static int getInt(String key) {
    return properties.getInt(key);
  }

  public static double getDouble(String key) {
    return properties.getDouble(key);
  }

  public static void addPropertyChangeListener(String key, PropertyChangeListener listener) {
    Set<PropertyChangeListener> listeners = changeListeners.get(key);
    if (listeners == null) {
      listeners = new HashSet<PropertyChangeListener>();
    }
    listeners.add(listener);
    changeListeners.put(key, listeners);
  }

  public static void removePropertyChangeListener(String key, PropertyChangeListener listener) {
    Set<PropertyChangeListener> listeners = changeListeners.get(key);
    if (listeners != null) {
      listeners.remove(listener);
    }
  }
}

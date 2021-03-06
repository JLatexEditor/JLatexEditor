package jlatexeditor.gproperties;

import de.endrullis.utils.BetterProperties2;
import de.endrullis.utils.BetterProperties2.Comment;
import de.endrullis.utils.BetterProperties2.Def;
import de.endrullis.utils.BetterProperties2.PSet;
import de.endrullis.utils.BetterProperties2.Range;
import jlatexeditor.addon.AddOn;
import util.Aspell;
import util.Hunspell;

import javax.swing.*;
import javax.swing.plaf.FontUIResource;
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
	public static final String SETTINGS_DIR = System.getProperty("user.home") + "/.jlatexeditor";
	public static final File CONFIG_FILE = new File(SETTINGS_DIR + "/global.properties");
  public static HashMap<String, Object> TEXT_ANTIALIAS_MAP = new HashMap<String, Object>();
  public static String[] TEXT_ANTIALIAS_KEYS;

  private static ArrayList<String> monospaceFonts = new ArrayList<String>();

  private static BetterProperties2 properties = new BetterProperties2();
  private static HashMap<String, Set<PropertyChangeListener>> changeListeners = new HashMap<String, Set<PropertyChangeListener>>();
	private static boolean loadedSuccessfully = false;

  // properties
  private static Font editorFont;
  private static Object textAntiAliasing = RenderingHints.VALUE_TEXT_ANTIALIAS_OFF;

  public static final Range INT = BetterProperties2.INT;
  public static final Range INT_GT_0 = BetterProperties2.INT_GT_0;
  public static final Range INT_GT_100 = BetterProperties2.INT_GT_100;
  public static final Range DOUBLE = BetterProperties2.DOUBLE;
  public static final Range DOUBLE_0_TO_1 = BetterProperties2.DOUBLE_0_TO_1;
  public static final Range BOOLEAN = BetterProperties2.BOOLEAN;
  public static final Range STRING = BetterProperties2.STRING;
  public static final Range SHORTCUT = BetterProperties2.SHORTCUT;
  public static final Range LOGLEVEL = BetterProperties2.LOGLEVEL;

  private static final String EDITOR_FONT_NAME = "editor.font.name";
  private static final String EDITOR_FONT_SIZE = "editor.font.size";
  private static final String EDITOR_FONT_ANTIALIASING = "editor.font.antialiasing";

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

    // set default for the properties file
    properties.addEntry(new Comment("\n## General properties"));
    properties.addEntry(new Comment(" Check for updates"));
    properties.addEntry(new Def("check_for_updates", BOOLEAN, "true"));
	  properties.addEntry(new Comment(" When closing the editor"));
    properties.addEntry(new Def("ask_for_saving_files_before_closing", BOOLEAN, "true"));
	  properties.addEntry(new Comment(" Check for svn updates in the background"));
    properties.addEntry(new Def("svn.executable", STRING, "svn"));
	  properties.addEntry(new Def("check_for_svn_updates", BOOLEAN, "true"));
	  properties.addEntry(new Comment(" Interval in which JLE checks for svn updates (in seconds)"));
	  properties.addEntry(new Def("check_for_svn_updates.interval", INT_GT_0, "120"));
	  properties.addEntry(new Comment(" Show new features after upgrade"));
	  properties.addEntry(new Def("show_new_features", BOOLEAN, "true"));
	  properties.addEntry(new Def("last_program_version", STRING, ""));

	  properties.addEntry(new Comment("\n## Window properties"));
	  properties.addEntry(new Comment(" Position, width, and height of the main window"));
	  properties.addEntry(new Def("main_window.x", INT_GT_0, "0"));
	  properties.addEntry(new Def("main_window.y", INT_GT_0, "0"));
	  properties.addEntry(new Def("main_window.width", INT_GT_100, "1024"));
	  properties.addEntry(new Def("main_window.height", INT_GT_100, "800"));
	  properties.addEntry(new Def("main_window.maximized_state", INT_GT_0, "" + JFrame.MAXIMIZED_BOTH));
	  properties.addEntry(new Comment(" Width of the symbols panel as part of the main window"));
	  properties.addEntry(new Def("main_window.symbols_panel.width", INT_GT_0, "220"));
	  properties.addEntry(new Comment(" Height of the tools panel as part of the main window"));
	  properties.addEntry(new Def("main_window.tools_panel.height", DOUBLE_0_TO_1, "0.15"));
	  properties.addEntry(new Comment(" Number of parent directories of the open file shown in the window title"));
	  properties.addEntry(new Def("main_window.title.number_of_parent_dirs_shown", INT_GT_0, "2"));
		properties.addEntry(new Comment(" Increase this value if you think the GUI fonts are to small (requires program restart)"));
	  properties.addEntry(new Def("main_window.font_scale_factor", DOUBLE, "1.0"));

	  properties.addEntry(new Comment("\n## Editor properties"));
    properties.addEntry(new Comment(" Font settings"));
    properties.addEntry(new Def(EDITOR_FONT_NAME, new PSet(MONOSPACE_FONTS_ARRAY), "Monospaced"));
    properties.addEntry(new Def(EDITOR_FONT_SIZE, INT_GT_0, "13"));
    properties.addEntry(new Def(EDITOR_FONT_ANTIALIASING, new PSet(TEXT_ANTIALIAS_KEYS), "On"));
    properties.addEntry(new Def("editor.wrap", BOOLEAN, "true"));
    properties.addEntry(new Def("editor.columns_per_row", INT_GT_0, "120"));
    properties.addEntry(new Def("editor.hide_mouse_during_typing", BOOLEAN, "true"));
	  properties.addEntry(new Comment(" Code completion"));
	  properties.addEntry(new Def("editor.completion.commands.filter.level1", INT, "1000"));
	  properties.addEntry(new Def("editor.completion.commands.filter.level2", INT, "10"));
	  properties.addEntry(new Def("editor.completion.commands.filter.level3", INT, "-1"));
	  properties.addEntry(new Def("editor.completion.environments.filter.level1", INT, "100"));
	  properties.addEntry(new Def("editor.completion.environments.filter.level2", INT, "1"));
	  properties.addEntry(new Def("editor.completion.environments.filter.level3", INT, "-1"));
	  properties.addEntry(new Def("editor.completion.packages.filter.level1", INT, "100"));
	  properties.addEntry(new Def("editor.completion.packages.filter.level2", INT, "1"));
	  properties.addEntry(new Def("editor.completion.packages.filter.level3", INT, "-1"));
	  properties.addEntry(new Def("editor.completion.documentclasses.filter.level1", INT, "100"));
	  properties.addEntry(new Def("editor.completion.documentclasses.filter.level2", INT, "1"));
	  properties.addEntry(new Def("editor.completion.documentclasses.filter.level3", INT, "-1"));
	  properties.addEntry(new Comment(" Automatic code completion"));
	  properties.addEntry(new Def("editor.auto_completion.activated", BOOLEAN, "false"));
	  properties.addEntry(new Def("editor.auto_completion.min_number_of_letters", INT_GT_0, "3"));
	  properties.addEntry(new Def("editor.auto_completion.delay", INT_GT_0, "200"));
	  properties.addEntry(new Def("editor.auto_close_environment", BOOLEAN, "true"));
	  properties.addEntry(new Def("editor.auto_continue_comment", BOOLEAN, "true"));
	  properties.addEntry(new Comment(" Automatic indentation"));
	  properties.addEntry(new Def("editor.auto_indentation.after_begin", BOOLEAN, "true"));
	  properties.addEntry(new Def("editor.auto_indentation.after_item", BOOLEAN, "true"));
	  properties.addEntry(new Def("editor.auto_indentation.after_opening_brace", BOOLEAN, "true"));
	  properties.addEntry(new Comment(" Clear, reset, or keep selection when closing search"));
	  properties.addEntry(new Def("editor.when_closing_search", new PSet("keep selection", "reset selection", "clear selection"), "reset selection"));
    properties.addEntry(new Comment(" Bracket matching"));
    properties.addEntry(new Def("editor.bracket_matching.depth", new PSet("1", "2", "3", "4", "5", "6"), "2"));

    properties.addEntry(new Comment("\n## Shortcuts"));
    properties.addEntry(new Comment(" File menu"));
    properties.addEntry(new Def("shortcut.new", SHORTCUT, "control N"));
    properties.addEntry(new Def("shortcut.open", SHORTCUT, "control O"));
    properties.addEntry(new Def("shortcut.open last closed", SHORTCUT, "control shift W"));
    properties.addEntry(new Def("shortcut.save", SHORTCUT, "control S"));
    properties.addEntry(new Def("shortcut.save as", SHORTCUT, "control A"));
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
    properties.addEntry(new Def("shortcut.select all", SHORTCUT, "control A"));
    properties.addEntry(new Def("shortcut.select none", SHORTCUT, "control shift A"));
    properties.addEntry(new Def("shortcut.comment", SHORTCUT, "control D"));
    properties.addEntry(new Def("shortcut.uncomment", SHORTCUT, "control shift D"));
    properties.addEntry(new Def("shortcut.diff", SHORTCUT, "alt D"));
    properties.addEntry(new Comment(" View"));
    properties.addEntry(new Def("shortcut.symbols", SHORTCUT, "alt Y"));
    properties.addEntry(new Def("shortcut.structure", SHORTCUT, "alt X"));
    properties.addEntry(new Def("shortcut.compile", SHORTCUT, "alt L"));
    properties.addEntry(new Def("shortcut.local history", SHORTCUT, ""));
	  properties.addEntry(new Def("shortcut.status bar", SHORTCUT, ""));
    properties.addEntry(new Comment(" Build menu"));
    properties.addEntry(new Def("shortcut.pdf", SHORTCUT, "alt 1"));
    properties.addEntry(new Def("shortcut.dvi", SHORTCUT, "alt 2"));
    properties.addEntry(new Def("shortcut.dvi + ps", SHORTCUT, "alt 3"));
    properties.addEntry(new Def("shortcut.dvi + ps + pdf", SHORTCUT, "alt 4"));
	  properties.addEntry(new Comment(" LaTeX menu"));
	  properties.addEntry(new Def("shortcut.forward search", SHORTCUT, "control shift F"));
		for (AddOn addOn : AddOn.getAllAddOnsMap().values()) {
			properties.addEntry(new Def("shortcut." + addOn.getKey(), SHORTCUT, addOn.getShortcut()));
		}
    properties.addEntry(new Comment(" Version control menu"));
    properties.addEntry(new Def("shortcut.svn update", SHORTCUT, "alt U"));
    properties.addEntry(new Def("shortcut.svn commit", SHORTCUT, "alt C"));
    properties.addEntry(new Def("shortcut.font", SHORTCUT, ""));
    properties.addEntry(new Def("shortcut.template editor", SHORTCUT, "control T"));
    properties.addEntry(new Def("shortcut.global settings", SHORTCUT, "control alt S"));
    properties.addEntry(new Comment(" Editors/tabs"));
	  properties.addEntry(new Def("shortcut.set master document", SHORTCUT, ""));
    properties.addEntry(new Def("shortcut.select next tab", SHORTCUT, "alt RIGHT"));
    properties.addEntry(new Def("shortcut.select previous tab", SHORTCUT, "alt LEFT"));
	  properties.addEntry(new Def("shortcut.move tab left", SHORTCUT, "control alt LEFT"));
	  properties.addEntry(new Def("shortcut.move tab right", SHORTCUT, "control alt RIGHT"));
    properties.addEntry(new Comment(" Initiate update"));
    properties.addEntry(new Def("shortcut.update", SHORTCUT, ""));
    properties.addEntry(new Comment(" Show change log"));
    properties.addEntry(new Def("shortcut.show change log", SHORTCUT, ""));
    properties.addEntry(new Comment(" About screen"));
    properties.addEntry(new Def("shortcut.about", SHORTCUT, ""));

    properties.addEntry(new Comment(" Editor"));
    properties.addEntry(new Def("shortcut.jump left", SHORTCUT, "control LEFT"));
    properties.addEntry(new Def("shortcut.jump right", SHORTCUT, "control RIGHT"));
    properties.addEntry(new Def("shortcut.jump to front", SHORTCUT, "control HOME"));
    properties.addEntry(new Def("shortcut.jump to end", SHORTCUT, "control END"));
    properties.addEntry(new Def("shortcut.move view up", SHORTCUT, "control UP")); // alt UP
    properties.addEntry(new Def("shortcut.move view down", SHORTCUT, "control DOWN")); // alt DOWN
    properties.addEntry(new Def("shortcut.remove line", SHORTCUT, "control Y"));
    properties.addEntry(new Def("shortcut.remove line before caret", SHORTCUT, "control U"));
    properties.addEntry(new Def("shortcut.remove line behind caret", SHORTCUT, "control K"));
    properties.addEntry(new Def("shortcut.remove word before caret", SHORTCUT, "control BACK_SPACE"));
    properties.addEntry(new Def("shortcut.remove word behind caret", SHORTCUT, "control DELETE"));
    properties.addEntry(new Def("shortcut.complete", SHORTCUT, "control SPACE"));

    properties.addEntry(new Comment(" Focus Traversal"));
    properties.addEntry(new Def("shortcut.focus traversal forward", SHORTCUT, "TAB"));
    properties.addEntry(new Def("shortcut.focus traversal backward", SHORTCUT, "shift TAB"));

	  properties.addEntry(new Comment("\n## Compiler settings"));
    properties.addEntry(new Comment(" pdflatex"));
    properties.addEntry(new Def("compiler.pdflatex.executable", STRING, "pdflatex"));
    properties.addEntry(new Def("compiler.pdflatex.parameters", STRING, "-synctex=1"));
    properties.addEntry(new Comment(" latex"));
    properties.addEntry(new Def("compiler.latex.executable", STRING, "latex"));
    properties.addEntry(new Def("compiler.latex.parameters", STRING, "--src-specials"));
    properties.addEntry(new Def("compiler.dvips.executable", STRING, "dvips"));
    properties.addEntry(new Def("compiler.ps2pdf.executable", STRING, "ps2pdf"));
    properties.addEntry(new Comment(" bibtex"));
    properties.addEntry(new Def("compiler.bibtex.executable", STRING, "bibtex"));
    properties.addEntry(new Comment(" forward search"));
    properties.addEntry(new Def("forward search.viewer", STRING, ""));
    properties.addEntry(new Def("inverse search.port", INT_GT_0, "13231"));

	  properties.addEntry(new Comment("\n## Debugging"));
    properties.addEntry(new Comment(" global log level"));
	  properties.addEntry(new Def("log level.jlatexeditor", LOGLEVEL, "<default>"));
	  properties.addEntry(new Def("log level.sce", LOGLEVEL, "<default>"));

    properties.addEntry(new Comment("\n## Spell checker settings"));
    properties.addEntry(new Def("editor.spell_checker", new PSet("none", "aspell", "hunspell"), "aspell"));
    properties.addEntry(new Def("aspell.executable", STRING, "aspell"));
    properties.addEntry(new Def("hunspell.executable", STRING, "hunspell"));

  	// set executables
    load();
		Aspell.ASPELL_EXECUTABLE = GProperties.getString("aspell.executable");
		Hunspell.HUNSPELL_EXECUTABLE = GProperties.getString("hunspell.executable");

    List<String> dictList;
    try {
      dictList = Aspell.availableDicts();
    } catch (IOException e) {
      dictList = new ArrayList<String>();
    }
    String[] aspellDicts = new String[dictList.size()];
    dictList.toArray(aspellDicts);

	  try {
	    dictList = Hunspell.availableDicts();
	  } catch (IOException e) {
	    dictList = new ArrayList<String>();
	  }
	  String[] hunspellDicts = new String[dictList.size()];
	  dictList.toArray(hunspellDicts);

    properties.addEntry(new Def("aspell.lang", new PSet(aspellDicts), getFromList(aspellDicts, "en_GB")));
    properties.addEntry(new Def("hunspell.lang", new PSet(hunspellDicts), getFromList(hunspellDicts, "en_GB")));

    load();
    autoSave();
  }

	private static String getFromList(String[] dicts, String default_) {
		for (String dict : dicts) {
			if (dict.equals(default_)) {
				return dict;
			}
		}
		if (dicts.length > 0) {
			return dicts[0];
		}
		return "";
	}

	public static Def getDef(String key) {
    return properties.getDefMap().get(key);
  }

  public static Range getRange(String key) {
    return properties.getDefMap().get(key).getRange();
  }

  public static synchronized void load() {
    Hashtable<Object, Object> oldMap = (Hashtable<Object, Object>) properties.clone();

    if (CONFIG_FILE.exists()) {
      try {
        properties.load(new FileReader(CONFIG_FILE));
	      loadedSuccessfully = true;
      } catch (IOException e) {
        properties.loadDefaults();
	      loadedSuccessfully = false;
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

      if (newValue == null || oldValue == null || !newValue.equals(oldValue)) {
        for (PropertyChangeListener propertyChangeListener : changeListeners.get(key)) {
          propertyChangeListener.propertyChange(new PropertyChangeEvent(CONFIG_FILE, key, oldValue, newValue));
        }
      }
    }
  }

  private static void extractProperties() {
    try {
      editorFont = new Font(properties.getProperty(EDITOR_FONT_NAME), Font.PLAIN, properties.getInt(EDITOR_FONT_SIZE));
      textAntiAliasing = TEXT_ANTIALIAS_MAP.get(properties.getProperty(EDITOR_FONT_ANTIALIASING));
    } catch (NumberFormatException ignored) {
    }
  }

	private static void autoSave() {
		if (loadedSuccessfully) {
			save();
		}
	}

  public static synchronized void save() {
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

  /**
   * Checks whether there are user changes.
   *
   * @return true if at least one property changed
   */
  public static boolean hasChanges() {
    for(BetterProperties2.Entry entry : properties.getEntries()) {
      if(!(entry instanceof Def)) continue;


      Def def = (Def) entry;
      String key = def.getKey();
      String currValue = properties.getProperty(key);
      String defValue = def.getValue();

      if(currValue != null && !currValue.equals(defValue)) return true;
    }

    return false;
  }

  /**
   * Returns the underlying properties.
   *
   * @return underlying properties
   */
  public static BetterProperties2 getProperties() {
    return properties;
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
    return properties.getString("aspell.lang");
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

	public static void setMainWindowBounds(Rectangle mainWindowBounds, int mainWindowMaximizedState) {
		properties.setInt("main_window.x", mainWindowBounds.x);
		properties.setInt("main_window.y", mainWindowBounds.y);
		properties.setInt("main_window.width", mainWindowBounds.width);
		properties.setInt("main_window.height", mainWindowBounds.height);
		properties.setInt("main_window.maximized_state", mainWindowMaximizedState);
		//save();
	}

	public static Rectangle getMainWindowBounds() {
		return new Rectangle(
				properties.getInt("main_window.x"),
				properties.getInt("main_window.y"),
				Math.max(properties.getInt("main_window.width"), 100),
				Math.max(properties.getInt("main_window.height"), 100)
		);
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

/**
 * @author Jörg Endrullis
 * @author Stefan Endrullis
 */

package jlatexeditor;

import de.endrullis.utils.ProgramUpdater;
import de.endrullis.utils.StringUtils;
import jlatexeditor.addon.AddOn;
import jlatexeditor.codehelper.BackgroundParser;
import jlatexeditor.errorhighlighting.LatexCompiler;
import jlatexeditor.errorhighlighting.LatexErrorHighlighting;
import jlatexeditor.gproperties.GProperties;
import jlatexeditor.gui.*;
import jlatexeditor.remote.FileLineNr;
import jlatexeditor.remote.NetworkNode;
import jlatexeditor.syntaxhighlighting.LatexStyles;
import jlatexeditor.tools.SVN;
import jlatexeditor.tools.SVNView;
import jlatexeditor.tools.ThreadInfoWindow;
import sce.component.*;
import sce.component.SourceCodeEditorUI.Comp;
import util.*;
import util.diff.Diff;
import util.filechooser.SCEFileChooser;
import util.gui.SCETabbedPane;

import javax.swing.*;
import javax.swing.Timer;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;
import javax.swing.plaf.FontUIResource;
import javax.swing.plaf.basic.BasicSplitPaneUI;
import java.awt.*;
import java.awt.event.*;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.*;
import java.net.URI;
import java.util.*;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.LogManager;
import java.util.logging.Logger;

import static de.endrullis.utils.Tuple.Tuple2;

public class JLatexEditorJFrame extends JFrame implements SCEManagerInteraction, ActionListener, WindowListener, ChangeListener, MouseMotionListener, TreeSelectionListener, SCETabbedPane.CloseListener {
  public static final File FILE_LAST_SESSION = new File(GProperties.SETTINGS_DIR + "/last.session");
  public static final File FILE_RECENT = new File(GProperties.SETTINGS_DIR + "/recent");
  public static final File CHANGELOG = new File("CHANGELOG");

	/** Logger. */
	private static Logger logger = Logger.getLogger(JLatexEditorJFrame.class.getName());
  private static String version = "*Bleeding Edge*";
	private static boolean bleedingEdge = true;
  private static boolean updateDisabled = true;
  private static String windowTitleSuffix;

  static {
    try {
      version = StreamUtils.readFile("version.txt");
	    bleedingEdge = false;
	    updateDisabled = false;

	    StreamUtils.readFile("updateDisabled.txt");
	    updateDisabled = true;
    } catch (IOException ignored) {
    }
    windowTitleSuffix = "JLatexEditor " + version;
  }

  private JMenuBar menuBar = null;
	private JMenu recentFilesMenu;
	private SizeLimitedStack<String> recentFiles = new SizeLimitedStack<String>(20);
  private SCETabbedPane tabbedPane = null;
  private JSplitPane textToolsSplit = null;
  private JTabbedPane toolsTab = null;
  private ErrorView errorView = null;
  private LeftPane leftPane = null;
  private SymbolsPanel symbolsPanel = null;
  private JTree structureTree = null;
  private SVNView svnView = null;

  private StatusBar statusBar = null;

  // command line arguments
  private List<String> filesToOpen;

  // last directory of the opening dialog
  private SCEFileChooser openDialog = new SCEFileChooser(this);

  // compile thread
  private LatexCompiler latexCompiler = null;
  // main file to compile
  private SourceCodeEditor<Doc> mainEditor = null;

  private LatexErrorHighlighting errorHighlighting = new LatexErrorHighlighting();

  // file changed time
  private Timer modificationTimer = new Timer(2000, this);
  private HashMap<File, Long> lastModified = new HashMap<File, Long>();

  private final ProgramUpdater updater = new ProgramUpdater("JLatexEditor update", "http://endrullis.de/JLatexEditor/update/");
	private LinkedHashMap<String,AddOn> addOns = AddOn.getAllAddOnsMap();

  private HashMap<URI, Doc> docMap = new HashMap<URI, Doc>();
	private File lastDocDir;

	public static void main(String args[]) {
		// init logging
		initLogging();

		Shell.setColoredOrNot(args);
		JLatexEditorParams params = new JLatexEditorParams();
		params.init(args);

		if (params.help.isSet()) {
			params.printHelp();
			System.exit(0);
		}
		if (params.version.isSet()) {
			System.out.println("JLatexEditor " + version);
			System.exit(0);
		}

    System.setProperty("apple.laf.useScreenMenuBar", "true");
		UIManager.put("TabbedPaneUI", "util.gui.SCETabbedPaneUI");
    UIManager.put("List.timeFactor", 200L);

		// scale all fonts if scale factor is not 1.0
		float fontScaleFactor = (float) GProperties.getDouble("main_window.font_scale_factor");
		if (Math.abs(fontScaleFactor - 1.0) > 0.001) {
			Enumeration<Object> keys = UIManager.getDefaults().keys();
			while (keys.hasMoreElements()) {
				Object o =  keys.nextElement();
				String key = o.toString();

				if (key.endsWith(".font")) {
					Object font = UIManager.get(key);
					if (font instanceof FontUIResource) {
						FontUIResource fontResource = (FontUIResource) font;
						float newSize = fontResource.getSize2D() * fontScaleFactor;
						UIManager.put(o, fontResource.deriveFont(newSize));
					}
				}
			}
		}

    new AboutDialog(null).showAndAutoHideAfter(5000);

    JLatexEditorJFrame latexEditor = new JLatexEditorJFrame(params);
    latexEditor.setBounds(GProperties.getMainWindowBounds());
    latexEditor.setVisible(true);
  }

	private static void initLogging() {
		try {
			LogManager.getLogManager().readConfiguration(JLatexEditorJFrame.class.getResourceAsStream("/logging.properties"));
		} catch (IOException e) {
			e.printStackTrace();
		}
		String jleLogLevel = GProperties.getString("log level.jlatexeditor");
		if (!jleLogLevel.equals("<default>")) {
			Logger.getLogger("jlatexeditor").setLevel(Level.parse(jleLogLevel));
		}
		String sceLogLevel = GProperties.getString("log level.sce");
		if (!sceLogLevel.equals("<default>")) {
			Logger.getLogger("sce").setLevel(Level.parse(sceLogLevel));
		}
	}

	public JLatexEditorJFrame(JLatexEditorParams params) {
    super(windowTitleSuffix);
    SCEManager.setInstance(this);
    SCEManager.setMainWindow(this);

		// set files to open
    filesToOpen = (List<String>) params.getUnboundArguments().clone();
		// open change log if programs has been updated
		if (!bleedingEdge && !version.equals(GProperties.getString("last_program_version"))) {
			filesToOpen.add(CHANGELOG.getAbsolutePath());
			GProperties.set("last_program_version", version);
		}

    // restore executable flags after update of the start-up script
    if(SystemUtils.isLinuxOS() || SystemUtils.isMacOS()) {
      try {
        ProcessUtil.exec(new String[] {"chmod", "+x", "jlatexeditor"}, null);
      } catch(Throwable ignored) { }
    }


    setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
    addWindowListener(this);
    Runtime.getRuntime().addShutdownHook(new ShutdownHook(this));

	  initFileChooser();

		// set icon
		SCEManager.setWindowIcon(this);

    /*
    JRootPane rootPane = getRootPane();
    rootPane.setWindowDecorationStyle(JRootPane.FRAME);
    rootPane.putClientProperty("Quaqua.RootPane.isVertical", Boolean.FALSE);
    rootPane.putClientProperty("Quaqua.RootPane.isPalette", Boolean.FALSE);
    */

    // set Layout
    Container cp = getContentPane();
    cp.setLayout(new BorderLayout());

    // create menu
    menuBar = new JMenuBar();
    setJMenuBar(menuBar);

    JMenu fileMenu = new JMenu("File");
    fileMenu.setMnemonic('F');
    menuBar.add(fileMenu);

    fileMenu.add(createMenuItem("New", "new", 'N'));
    fileMenu.add(createMenuItem("Open", "open", 'O'));
    recentFilesMenu = new JMenu("Open Recent");
    recentFilesMenu.setMnemonic('R');
    fileMenu.add(recentFilesMenu);
    fileMenu.add(createMenuItem("Save", "save", 'S'));
    fileMenu.add(createMenuItem("Save As...", "save as", 'A'));
    fileMenu.add(createMenuItem("Close", "close", 'C'));
    fileMenu.addSeparator();
    fileMenu.add(createMenuItem("Exit", "exit", 'E'));

    JMenu editMenu = new JMenu("Edit");
    editMenu.setMnemonic('E');
    menuBar.add(editMenu);

    editMenu.add(createMenuItem("Undo", "undo", 'U'));
    editMenu.add(createMenuItem("Redo", "redo", 'R'));
    editMenu.addSeparator();
    editMenu.add(createMenuItem("Find", "find", 'F'));
    editMenu.add(createMenuItem("Replace", "replace", 'R'));
    editMenu.add(createMenuItem("Find Next", "find next", 'N'));
    editMenu.add(createMenuItem("Find Previous", "find previous", 'P'));
    editMenu.add(createMenuItem("Cut", "cut", null));
    editMenu.add(createMenuItem("Copy", "copy", null));
    editMenu.add(createMenuItem("Paste", "paste", null));
    editMenu.addSeparator();
    editMenu.add(createMenuItem("Select All", "select all", null));
    editMenu.add(createMenuItem("Select None", "select none", null));
    editMenu.addSeparator();
    editMenu.add(createMenuItem("Comment", "comment", 'o'));
    editMenu.add(createMenuItem("Uncomment", "uncomment", 'u'));
    editMenu.addSeparator();
    editMenu.add(createMenuItem("Compare With... (Diff)", "diff", 'D'));

    JMenu viewMenu = new JMenu("View");
    viewMenu.setMnemonic('V');
    menuBar.add(viewMenu);

    viewMenu.add(createMenuItem("Symbols", "symbols", 'y'));
    viewMenu.add(createMenuItem("Structure", "structure", 'S'));
    viewMenu.add(createMenuItem("Compile", "compile", 'C'));
    viewMenu.add(createMenuItem("Local History", "local history", 'L'));
	  viewMenu.add(createMenuItem("Status Bar", "status bar", 'B'));

    JMenu buildMenu = new JMenu("Build");
    buildMenu.setMnemonic('B');
    menuBar.add(buildMenu);

    buildMenu.add(createMenuItem("pdf", "pdf", null));
    buildMenu.add(createMenuItem("dvi", "dvi", null));
    buildMenu.add(createMenuItem("dvi + ps", "dvi + ps", null));
    buildMenu.add(createMenuItem("dvi + ps + pdf", "dvi + ps + pdf", null));

    JMenu latexMenu = new JMenu("LaTeX");
		latexMenu.add(createMenuItem("Forward Search", "forward search", null));
		latexMenu.addSeparator();
    latexMenu.setMnemonic('l');
    menuBar.add(latexMenu);

		for (AddOn addOn : addOns.values()) {
			latexMenu.add(createMenuItem(addOn.getLabel(), addOn.getKey(), null));
		}

    JMenu vcMenu = new JMenu("Version Control");
    vcMenu.setMnemonic('o');
    menuBar.add(vcMenu);

    vcMenu.add(createMenuItem("SVN Update", "svn update", 'u'));
    vcMenu.add(createMenuItem("SVN Commit", "svn commit", 'c'));

    JMenu windowMenu = new JMenu("Editors");
    windowMenu.setMnemonic('t');
    menuBar.add(windowMenu);

    windowMenu.add(createMenuItem("Set as Master Document", "set master document", 'm'));
    windowMenu.addSeparator();
    windowMenu.add(createMenuItem("Select Next Tab", "select next tab", 'n'));
    windowMenu.add(createMenuItem("Select Previous tab", "select previous tab", 'p'));
	  windowMenu.addSeparator();
    windowMenu.add(createMenuItem("Move Tab Left", "move tab left", 'l'));
    windowMenu.add(createMenuItem("Move Tab Right", "move tab right", 'r'));

    JMenu settingsMenu = new JMenu("Settings");
    settingsMenu.setMnemonic('S');
    menuBar.add(settingsMenu);

    settingsMenu.add(createMenuItem("Font", "font", 'F'));
    JMenu forwardSearch = new JMenu("Forward Search") {{
      for(Wizard.ProgramWithParameters program : Wizard.VIEWERS) {
        add(createMenuItem(program.getName(), "forward search: " + program.getExecutable() + " " + program.getParameters(), null));
      }
    }};
    settingsMenu.add(forwardSearch);
    settingsMenu.add(createMenuItem("Template Editor", "template editor", 'T'));
		// TODO
    // settingsMenu.add(createMenuItem("Quick Setup Wizard", "wizard", 'W'));
    settingsMenu.add(createMenuItem("Global Settings", "global settings", 'G'));

    JMenu helpMenu = new JMenu("Help");
    helpMenu.setMnemonic('H');
    menuBar.add(helpMenu);
    helpMenu.add(createMenuItem("Debug", "stack trace", 'D'));
		helpMenu.add(createMenuItem("Show Change Log", "show change log", 'C'));
    helpMenu.addSeparator();

    JMenuItem updateMenuItem = createMenuItem("Check for Update", "update", 'U');
    if (updateDisabled) updateMenuItem.setVisible(false);
    helpMenu.add(updateMenuItem);
    helpMenu.add(createMenuItem("About", "about", 'A'));
    helpMenu.add(createMenuItem("Acknowledgements", "acknowledgements", 'K'));

    // misc keyboard actions
    createPaneShortcut(SCEPaneUI.Actions.JUMP_LEFT, true);
    createPaneShortcut(SCEPaneUI.Actions.JUMP_RIGHT, true);
    createPaneShortcut(SCEPaneUI.Actions.JUMP_TO_FRONT, true);
    createPaneShortcut(SCEPaneUI.Actions.JUMP_TO_END, true);
    createPaneShortcut(SCEPaneUI.Actions.MOVE_VIEW_UP, true);
    createPaneShortcut(SCEPaneUI.Actions.MOVE_VIEW_DOWN, true);
    createPaneShortcut(SCEPaneUI.Actions.REMOVE_LINE, false);
    createPaneShortcut(SCEPaneUI.Actions.REMOVE_LINE_BEFORE_CARET, false);
    createPaneShortcut(SCEPaneUI.Actions.REMOVE_LINE_BEHIND_CARET, false);
    createPaneShortcut(SCEPaneUI.Actions.REMOVE_WORD_BEFORE_CARET, false);
    createPaneShortcut(SCEPaneUI.Actions.REMOVE_WORD_BEHIND_CARET, false);
    createPaneShortcut(SCEPaneUI.Actions.COMPLETE, false);

    createPaneShortcut(SCEPaneUI.Actions.UNDO, false);
    createPaneShortcut(SCEPaneUI.Actions.REDO, false);
    createPaneShortcut(SCEPaneUI.Actions.FIND, false);
    createPaneShortcut(SCEPaneUI.Actions.REPLACE, false);
    createPaneShortcut(SCEPaneUI.Actions.FIND_NEXT, false);
    createPaneShortcut(SCEPaneUI.Actions.FIND_PREVIOUS, false);
    createPaneShortcut(SCEPaneUI.Actions.CUT, false);
    createPaneShortcut(SCEPaneUI.Actions.COPY, false);
    createPaneShortcut(SCEPaneUI.Actions.PASTE, false);
    createPaneShortcut(SCEPaneUI.Actions.SELECT_ALL, false);
    createPaneShortcut(SCEPaneUI.Actions.SELECT_NONE, false);
    createPaneShortcut(SCEPaneUI.Actions.COMMENT, false);
    createPaneShortcut(SCEPaneUI.Actions.UNCOMMENT, false);

    // prevent that TabbledPane consumes the "control UP"
    Object ancestorMap = UIManager.get("TabbedPane.ancestorInputMap");
    if(ancestorMap instanceof InputMap) {
      InputMap map = (InputMap) ancestorMap;
      map.remove(KeyStroke.getKeyStroke("control UP"));
    }

    // focus traversal keys
    KeyboardFocusManager focusManager = KeyboardFocusManager.getCurrentKeyboardFocusManager();
    focusManager.setDefaultFocusTraversalKeys(KeyboardFocusManager.FORWARD_TRAVERSAL_KEYS,
            new HashSet(Arrays.asList(KeyStroke.getKeyStroke(GProperties.getString("shortcut.focus traversal forward")))));
    focusManager.setDefaultFocusTraversalKeys(KeyboardFocusManager.BACKWARD_TRAVERSAL_KEYS,
            new HashSet(Arrays.asList(KeyStroke.getKeyStroke(GProperties.getString("shortcut.focus traversal backward")))));

    // error messages
    toolsTab = new JTabbedPane();
    errorView = new ErrorView(this);
    toolsTab.addTab("Compile", errorView);
    toolsTab.addTab("Local History", new LocalHistory(this));

		// background parser
		BackgroundParser backgroundParser = new BackgroundParser(this);
    SCEManager.setBackgroundParser(backgroundParser);
		backgroundParser.start();
		
    // tabs for the files
    tabbedPane = new SCETabbedPane();
    tabbedPane.setCloseListener(this);
    try {
      addTab(new Doc.UntitledDoc(), true);
    } catch (IOException ignored) {
    }

    statusBar = new StatusBar(this);

    // symbols panel
    symbolsPanel = new SymbolsPanel(this);
    // structure view
    structureTree = new JTree();
    structureTree.getSelectionModel().addTreeSelectionListener(this);
    // svn view
    svnView = new SVNView(this, statusBar);

    leftPane = new LeftPane(tabbedPane, symbolsPanel, new JScrollPane(structureTree), svnView);

    textToolsSplit = new JSplitPane(JSplitPane.VERTICAL_SPLIT, true, leftPane, toolsTab);
    textToolsSplit.setOneTouchExpandable(true);
    textToolsSplit.setResizeWeight(1 - GProperties.getDouble("main_window.tools_panel.height"));
    ((BasicSplitPaneUI) textToolsSplit.getUI()).getDivider().addMouseListener(new MouseAdapter() {
      public void mousePressed(MouseEvent e) {
        toolsTab.setVisible(true);
      }
    });

    cp.add(textToolsSplit, BorderLayout.CENTER);
    cp.add(statusBar, BorderLayout.SOUTH);
    cp.validate();

    errorHighlighting.attach(getEditor(0), errorView);

    // file changed timer
    modificationTimer.setActionCommand("timer");
    modificationTimer.start();

    // search for updates in the background
    if (!updateDisabled) {
      new Thread() {
        public void run() {
          checkForUpdates(true);
        }
      }.start();
    }

    structureTree.setModel(backgroundParser.getStructure());

    PropertyChangeListener fontChangeListener = new PropertyChangeListener() {
      public void propertyChange(PropertyChangeEvent evt) {
        changeFont(GProperties.getString("editor.font.name"), GProperties.getInt("editor.font.size"));
      }
    };
    GProperties.addPropertyChangeListener("editor.font.name", fontChangeListener);
    GProperties.addPropertyChangeListener("editor.font.size", fontChangeListener);
    GProperties.addPropertyChangeListener("editor.font.antialiasing", fontChangeListener);

    // inverse search
	  new NetworkNode(this).start();
  }

  /**
   * Reopen the files that were open the last time.
   */
  private void reopenLast() {
    try {
      BufferedReader reader = new BufferedReader(new FileReader(FILE_LAST_SESSION));
      String line;
      while ((line = reader.readLine()) != null) {
       openSeek(line);
      }
      reader.close();
    } catch (IOException ignored) {
    }
  }

  private void openSeek(String seek) {
	  // * at end marks master document
	  boolean masterDoc = seek.endsWith(":*");
	  if (masterDoc) {
		  seek = seek.substring(0, seek.length() - 2);
	  }

    int colon = seek.lastIndexOf(':');
    if (colon == -1) colon = seek.length();

    File file = new File(seek.substring(0, colon));
    int lineNr = colon >= seek.length() ? 0 : Integer.parseInt(seek.substring(colon + 1));

    if (file.exists() && file.isFile()) {
	    Doc.FileDoc doc = new Doc.FileDoc(file);
	    SourceCodeEditor<Doc> editor = open(doc);
	    if (masterDoc) {
	      setMasterDocument(doc);
	    }
      editor.getTextPane().getCaret().moveTo(lineNr, 0, false);
    }
  }

  private void loadRecent() {
    recentFiles.clear();
    try {
      BufferedReader reader = new BufferedReader(new FileReader(FILE_RECENT));
      String line;
      while ((line = reader.readLine()) != null) recentFiles.push(line);
      reader.close();
    } catch (IOException ignored) {
    }
  }

  private void addRecent(File file) {
    try {
      String fileName = file.getCanonicalPath();
      recentFiles.push(fileName);
	    updateRecentMenu();
    } catch (IOException ignored) {}
  }

  private void removeRecent(File file) {
    try {
      String fileName = file.getCanonicalPath();
      recentFiles.remove(fileName);
	    updateRecentMenu();
    } catch (IOException ignored) {}
  }

  private void updateRecentMenu() {
    recentFilesMenu.removeAll();
    for (String name : recentFiles) {
      JMenuItem item = new JMenuItem(name);
      item.setActionCommand("open recent:" + name);
      item.addActionListener(this);
      recentFilesMenu.add(item);
    }
    recentFilesMenu.addSeparator();
    recentFilesMenu.add(createMenuItem("Open Last Closed", "open last closed", 'L'));
    recentFilesMenu.add(createMenuItem("Clear List", "clear recent", 'C'));
  }

  private JMenuItem createMenuItem(String label, String command, Character mnemonic) {
    JMenuItem menuItem = new JMenuItem(label);
    menuItem.setActionCommand(command);
	  // set mnemonic
    if (mnemonic != null) {
      menuItem.setMnemonic(mnemonic);
    }
	  // set shortcut
    String shortcutString = GProperties.getString("shortcut." + command);
    if (shortcutString != null && !shortcutString.equals("")) {
      menuItem.setAccelerator(KeyStroke.getKeyStroke(shortcutString));
    }
		// set icon
		try {
			menuItem.setIcon(SCEManager.getMappedImageIcon(command));
		} catch (Exception ignored) {
		}
    menuItem.addActionListener(this);

    // add shortcut change listener
    GProperties.addPropertyChangeListener("shortcut." + command, new ShortcutChangeListener(menuItem));

    return menuItem;
  }

  /**
   * Creates a keyboard shortcut without a menu item.
   *
   * @param command action command
   * @param withAndWithoutShift register the shortcut also in combination with shift?
   */
  private void createPaneShortcut(final String command, final boolean withAndWithoutShift) {
	  final Comp comp = SourceCodeEditorUI.globalActions.contains(command) ? Comp.global : Comp.textPane;
	  
	  // register a property change listener to update the shortcut in the menu if it has been changed in global.properties
	  PropertyChangeListener propertyChangeListener = new PropertyChangeListener() {
		  public void propertyChange(PropertyChangeEvent evt) {
			  // register new keystroke
			  String shortcutString = GProperties.getString("shortcut." + command);

			  if (shortcutString != null && !shortcutString.equals("")) {
			    KeyStroke keyStroke = KeyStroke.getKeyStroke(shortcutString);
			    SourceCodeEditorUI.replaceKeyStrokeAndAction(comp, keyStroke, command);

			    if(withAndWithoutShift) {
			      keyStroke = KeyStroke.getKeyStroke("shift " + shortcutString);
				    SourceCodeEditorUI.replaceKeyStrokeAndAction(comp, keyStroke, command + " shift");
			    }
			  }
		  }
	  };
	  GProperties.addPropertyChangeListener("shortcut." + command, propertyChangeListener);
	  // fire an initial change to load the shortcut
	  propertyChangeListener.propertyChange(null);
  }

  private void initFileChooser() {
    openDialog.addChoosableFileFilter(SCEFileChooser.FILTER_TEX);
  }

	/**
	 * Returns the current content of the file.
	 * If the file is managed in the editor the editor content is returned.
	 *
	 * @param file file
	 * @return file content or editor content
	 * @throws IOException if an I/O error occurs
	 */
	public String getCurrentContent(File file) throws IOException {
		// check if file is opened
		SourceCodeEditor editor = getEditor(new Doc.FileDoc(file));
		if (editor != null) {
			// it is opened -> return editor content
			return editor.getText();
		} else {
			// it is not opened -> return file content
			return StreamUtils.readFile(file.getAbsolutePath());
		}
	}

  public int getTab(Doc doc) {
    for (int tab = 0; tab < tabbedPane.getTabCount(); tab++) {
      if (doc.equals(getEditor(tab).getResource())) return tab;
    }
    return -1;
  }

	/**
	 * Returns the editor for the document or null if the document is not open
	 *
	 * @param doc document
	 * @return editor or null if not open
	 */
	public SourceCodeEditor<Doc> getEditor(Doc doc) {
		if (docMap.containsKey(doc.getUri())) {
			doc = docMap.get(doc.getUri());

			// is open?
			int tab = getTab(doc);
			if (tab != -1) {
				return getEditor(tab);
			}
		}

		return null;
	}

  public int getEditorCount() {
    return tabbedPane.getTabCount();
  }

  public SourceCodeEditor<Doc> getEditor(int tab) {
    return (SourceCodeEditor<Doc>) tabbedPane.getComponentAt(tab);
  }

  public SourceCodeEditor<Doc> getActiveEditor() {
    return getEditor(tabbedPane.getSelectedIndex());
  }

  public SourceCodeEditor<Doc> getMainEditor() {
    return mainEditor != null ? mainEditor : getActiveEditor();
  }

  private SourceCodeEditor<Doc> addTab(Doc doc, boolean selectTab) throws IOException {
    SourceCodeEditor<Doc> editor;
	  editor = assignDocProperties(doc);
	  editor.setResource(doc);
    tabbedPane.removeChangeListener(this);
    tabbedPane.addTab(doc.getName(), editor);
    tabbedPane.addChangeListener(this);
    tabbedPane.setTabComponentAt(tabbedPane.getTabCount() - 1, new TabLabel(doc, editor));
	  if (selectTab) {
      tabbedPane.setSelectedIndex(tabbedPane.getTabCount() - 1);
	  }
    editor.open(doc);

    return editor;
  }

	private SourceCodeEditor<Doc> assignDocProperties(Doc doc) {
		SourceCodeEditor<Doc> editor;

		if (doc.getName().endsWith("global.properties")) {
		  editor = SCEManager.createGPropertiesSourceCodeEditor();
		} else
		if (doc.getName().endsWith("CHANGELOG")) {
		  editor = SCEManager.createChangeLogSourceCodeEditor();
		} else
		if (doc.getName().endsWith(".bib")) {
		  editor = SCEManager.createBibSourceCodeEditor(this);
		} else {
		  editor = SCEManager.createLatexSourceCodeEditor();
		}

		// load the document properties for this file format
		String extension = StringUtils.stringAfter(doc.getName(), ".", 'l').getOrElse("tex");
		Properties docProperties = new Properties();
		try {
			InputStream in = StreamUtils.getInputStream("data/docproperties/" + extension + ".properties");
			docProperties.load(in);
		} catch (IOException e) {
			try {
				InputStream in = StreamUtils.getInputStream("data/docproperties/tex.properties");
				docProperties.load(in);
			} catch (IOException e1) {
				System.out.println("warning: cannot find document properties for .tex files");
			}
		}

		// set the document properties
		for (Map.Entry<Object, Object> entry : docProperties.entrySet()) {
			doc.setProperty(entry.getKey().toString(), entry.getValue().toString());
		}

		return editor;
	}

	public synchronized SourceCodeEditor<Doc> open(Doc doc) {
		return open(doc, true);
	}

	public synchronized SourceCodeEditor<Doc> open(Doc doc, boolean selectTab) {
    try {
      // is existing object if it already exists, otherwise add it to docMap
      if (docMap.containsKey(doc.getUri())) {
        doc = docMap.get(doc.getUri());
      } else {
        docMap.put(doc.getUri(), doc);
      }

      // already open?
      int tab = getTab(doc);
      if (tab != -1) {
	      if (selectTab) {
          tabbedPane.setSelectedIndex(tab);
	      }
        return getEditor(tab);
      }

      // replacing the untitled tab?
      boolean closeFirstTab = false;
      if (tabbedPane.getTabCount() == 1) {
        SourceCodeEditor<Doc> firstEditor = getEditor(0);
        if (firstEditor.getResource() instanceof Doc.UntitledDoc && !firstEditor.getTextPane().getDocument().isModified()) {
          closeFirstTab = true;
        }
      }

      SourceCodeEditor<Doc> editor = addTab(doc, selectTab);
      if (closeFirstTab) closeTab(0);

      if (doc instanceof Doc.FileDoc) {
        Doc.FileDoc fileDoc = (Doc.FileDoc) doc;
        File file = fileDoc.getFile();
        lastModified.put(file, file.lastModified());

	      removeRecent(file);
      }

      editorChanged();
      editor.getTextPane().requestFocus();
      return editor;
    } catch (IOException exc) {
      logger.log(Level.SEVERE, "Error opening file", exc);
    }
    return null;
  }

	public SourceCodeEditor<Doc> open(Doc doc, int lineNr) {
		SourceCodeEditor<Doc> editor = open(doc);
		if (lineNr > 0) {
			editor.getTextPane().getCaret().moveTo(lineNr, 0, false);
		}

		return editor;
	}

	@Override
	public boolean isProjectUnderVersionControl() {
		File mainFile = mainEditor.getFile();
		return mainFile.exists() && SVN.getInstance().isDirUnderVersionControl(mainFile.getParentFile());
	}

	/**
   * Returns true if any modifications have been done at an open file.
   *
   * @return true if any modifications have been done at an open file
   */
  public boolean anyModifications() {
    for (int tab = 0; tab < tabbedPane.getTabCount(); tab++) {
      if (getEditor(tab).getTextPane().getDocument().isModified()) {
        return true;
      }
    }
    return false;
  }

  /**
   * Saves all open documents.
   *
   * @return true if the process of saving the documents has NOT been canceled
   */
  public synchronized boolean saveAll() {
    boolean all = true;
    for (int tab = 0; tab < tabbedPane.getTabCount(); tab++) {
      SourceCodeEditor<Doc> editor = getEditor(tab);
      AbstractResource resource = editor.getResource();
      boolean save = (!(resource instanceof Doc.UntitledDoc)) || tab == tabbedPane.getSelectedIndex();
      if (save) {
        if (!save(editor)) all = false;
      }
    }

    SCEManager.getBackgroundParser().parse();
    return all;
  }

  /**
   * Saves the document given by the editor.
   *
   * @param editor editor containing the document to save
   * @return true if saving the document has NOT been canceled
   */
  private synchronized boolean save(SourceCodeEditor<Doc> editor) {
    if (!editor.getTextPane().getDocument().isModified()) return true;

    Doc doc = editor.getResource();

    boolean gPropertiesSaved = false;

    File file = null;
    if (doc instanceof Doc.UntitledDoc) {
	    if (!saveAs(editor)) return false;

	    doc = editor.getResource();
	    Doc.FileDoc fileDoc = (Doc.FileDoc) doc;
	    file = fileDoc.getFile();
    } else if (doc instanceof Doc.FileDoc) {
      Doc.FileDoc fileDoc = (Doc.FileDoc) doc;
      file = fileDoc.getFile();
      gPropertiesSaved = file.equals(GProperties.CONFIG_FILE);
    }

    String text = editor.getTextPane().getText();
    try {
      boolean history = true;
      File history_dir = LocalHistory.getHistoryDir(file);
      if (!history_dir.exists()) history = history_dir.mkdirs();

      File file_backup = LocalHistory.getBackupFile(file);
      File file_revisions = LocalHistory.getRevisionsFile(file);

      PrintWriter writer = new PrintWriter(new FileOutputStream(file));
      writer.write(text);
      writer.close();

      if (history) {
        PrintWriter diff_writer = new PrintWriter(new FileOutputStream(file_revisions, true));

        try {
          if (file_backup.exists()) {
            String backup = StreamUtils.readFile(file_backup.getCanonicalPath());

            diff_writer.write(Diff.diffPlain(text, backup));
          }
        } catch (Exception diffException) {
          logger.log(Level.SEVERE, "Local history, error starting diff" + diffException);
        }

        diff_writer.println(LocalHistory.REVISION + Calendar.getInstance().getTime());
        diff_writer.close();

        PrintWriter history_writer = new PrintWriter(new FileOutputStream(file_backup));
        history_writer.write(text);
        history_writer.close();
      }

      lastModified.put(file, new File(file.getCanonicalPath()).lastModified());
      editor.getTextPane().getDocument().setModified(false);

      if (gPropertiesSaved) {
        GProperties.load();
      }
    } catch (IOException ex) {
      ex.printStackTrace();
    }

    return true;
  }

	/**
	 * Opens a file dialog to save the file under a new name.  When "save" is clicked
	 * the file is saved and true is returned.  Otherwise the file is not saved and
	 * false is returned.
	 * 
	 * @param editor editor
	 * @return returns true if the file was saved
	 */
	private boolean saveAs(SourceCodeEditor<Doc> editor) {
		Doc doc = editor.getResource();

		openDialog.setDialogTitle("Save " + doc.getName());
		openDialog.setDialogType(SCEFileChooser.TYPE_SAVE_DIALOG);
		openDialog.setCurrentDirectory(lastDocDir);
		if (openDialog.showDialog() != SCEFileChooser.RESULT_OK) return false;
		File file = openDialog.getSelectedFile();
		if (file == null) return false;

		if (file.exists()) {
		  int choice = JOptionPane.showOptionDialog(
		          this,
		          "The file exists! Do you want to overwrite the file?",
		          "File Exists",
		          JOptionPane.WARNING_MESSAGE,
		          JOptionPane.YES_NO_OPTION,
		          null,
		          new Object[]{"Overwrite", "Cancel"},
		          2
		  );
		  if (choice == 1) return false;
		}

		TabLabel tabLabel = (TabLabel) tabbedPane.getTabComponentAt(getTab(doc));
		docMap.remove(doc.getUri());
		doc = new Doc.FileDoc(file);
		assignDocProperties(doc);
		editor.getTextPane().getDocument().setModified(true);
		docMap.put(doc.getUri(), doc);
		tabLabel.setDoc(doc);
		editor.setResource(doc);

		save(editor);
		
		return true;
	}

	public boolean hasDocumentClass(SCEDocument document) {
		// search in the first 100 lines
    SCEDocumentRow[] rows = document.getRowsModel().getRows();
		for (int lineNr = 0; lineNr < 100 && lineNr < rows.length; lineNr++) {
			// ignore comments
			String line = rows[lineNr].toString().replaceAll("%.*", "");

			if (line.contains("\\documentclass") || line.contains("\\input") || line.contains("\\include")) {
				return true;
			}
			if (line.contains("\\usepackage")) {
				return false;
			}
		}
		return false;
	}

  public void stopCompile() {
    if (latexCompiler != null) latexCompiler.halt();
  }

	public LatexCompiler compile(LatexCompiler.Type type) {
		if (!hasDocumentClass(getMainEditor().getTextPane().getDocument())) {
			if (JOptionPane.showConfirmDialog(this, "Could not find a \\documentclass in your document.  Compile anyway?",
				"Compile without \\documentclass ?", JOptionPane.YES_NO_CANCEL_OPTION) != JOptionPane.YES_OPTION) {
				return null;
			}
		}

	  showTool(0);

    SourceCodeEditor<Doc> editor = mainEditor;
    if (editor == null) {
      editor = (SourceCodeEditor<Doc>) tabbedPane.getSelectedComponent();
    }

    if (latexCompiler != null) latexCompiler.halt();
    latexCompiler = LatexCompiler.createInstance(type, editor, errorView);

    errorHighlighting.clear();
    latexCompiler.addLatexCompileListener(errorHighlighting);

    latexCompiler.start();

		return latexCompiler;
  }

  private void closeTab(int tab) {
    SourceCodeEditor<Doc> editor = getEditor(tab);
	  Doc doc = editor.getResource();
	  if (doc instanceof Doc.FileDoc) {
		  addRecent(((Doc.FileDoc) doc).getFile());
	  }
	  docMap.remove(doc.getUri());
	  if (tabbedPane.getTabCount() > 1) {
      tabbedPane.removeTabAt(tab);
    } else {
      try {
        addTab(new Doc.UntitledDoc(), true);
      } catch (IOException ignored) {
      }
      tabbedPane.removeTabAt(tab);
    }
    editor.getTextPane().setText("");
	  editor.dispose();
  }

	public boolean isOpen(File file) {
		return docMap.containsKey(file.toURI());
	}

	/**
	 * Returns true if this JLE instance is responsible for the given file.
	 *
	 * @param file file
	 * @return true if this JLE instance is responsible for the given file
	 */
	public boolean isResponsibleFor(File file) {
		// did we open the file already?                  or is file referenced by the main document?
		return isOpen(file) || SCEManager.getBackgroundParser().isResponsibleFor(file);
	}

	public void bringToFront() {
    // for Mac OS X: bring application to front
    if(SystemUtils.isMacOS()) {
      try {
        MacUtil.activateApplication();
      } catch(Throwable ignored) {}
    }

	  // bringing window to the front
    toFront();

    requestFocus();
		getActiveEditor().getFocusedPane().requestFocus();
	}

	// ActionListener methods

  public void actionPerformed(ActionEvent e) {
    String action = e.getActionCommand();

		// timer
		if (action.equals("timer")) {
			checkExternalModification(true);
		} else

	  if (addOns.containsKey(action)) {
		  addOns.get(action).run(this);
	  } else

    // new file
    if (action.equals("new")) {
      try {
        addTab(new Doc.UntitledDoc(), true);
      } catch (IOException ignored) {
      }
    } else

		// open a file
		if (action.equals("open")) {
			openDialog.setDialogTitle("Open");
			openDialog.setDialogType(SCEFileChooser.TYPE_OPEN_DIALOG);
			openDialog.setCurrentDirectory(lastDocDir);
			if (openDialog.showDialog() != SCEFileChooser.RESULT_OK) return;
			if (openDialog.getSelectedFile() == null) return;

			open(new Doc.FileDoc(openDialog.getSelectedFile()));
		} else
		// recent files list
		if (action.startsWith("open recent:")) {
			open(new Doc.FileDoc(new File(action.substring("open recent:".length()))));
		} else
		if (action.startsWith("open last closed")) {
			try {
				open(new Doc.FileDoc(new File(recentFiles.pop())));
			} catch (NoSuchElementException ignored) {}
		} else
		if (action.equals("clear recent")) {
			recentFiles.clear();
			updateRecentMenu();
		} else
		// save a file
		if (action.equals("save")) {
			saveAll();
		} else
		// save a file as...
		if (action.equals("save as")) {
			saveAs(getActiveEditor());
		} else
		// close
		if (action.equals("close")) {
			if (getActiveEditor().isDiffView()) {
				getActiveEditor().closeDiffView();
			} else {
				closeTab(tabbedPane.getSelectedIndex());
			}
		} else
		// exit
		if (action.equals("exit")) {
			quit();
		} else

		// undo
		if (action.equals("undo")) {
			getActiveEditor().getTextPane().getUndoManager().undo(false);
		} else
		// undo
		if (action.equals("redo")) {
			getActiveEditor().getTextPane().getUndoManager().redo(false);
		} else

		// find
		if (action.equals("find")) {
			// paneUI
		} else
		// replace
		if (action.equals("replace")) {
			// paneUI
		} else
		// find next
		if (action.equals("find next")) {
			// paneUI
		} else
		// find previous
		if (action.equals("find previous")) {
			// paneUI
		} else

		// cut
		if (action.equals("cut")) {
			// paneUI
		} else
		// copy
		if (action.equals("copy")) {
			// paneUI
		} else
		// paste
		if (action.equals("paste")) {
			// paneUI
		} else
		// select all
		if (action.equals("select all")) {
			// paneUI
    } else
    if (action.equals("select none")) {
			// paneUI
    } else

		// lineComment
		if (action.equals("comment")) {
			// paneUI
		} else
		// lineUncomment
		if (action.equals("uncomment")) {
			// paneUI
		} else

		// show/hide symbols
		if (action.equals("symbols")) {
			leftPane.changeView(symbolsPanel);
		} else
		// show/hide symbols
		if (action.equals("structure")) {
			leftPane.changeView(structureTree);
		} else
		// show/hide compile
		if (action.equals("compile")) {
			toggleTool(0);
		} else
		// show/hide status bar
		if (action.equals("local history")) {
			toggleTool(1);
		} else
		// show/hide status bar
		if (action.equals("status bar")) {
			statusBar.setVisible(!statusBar.isVisible());
		} else

    // forward search
    if (action.startsWith("forward search: ")) {
      String command = action.substring("forward search: ".length());
      GProperties.set("forward search.viewer", command);
    } else

    if (action.equals("forward search")) {
	    performForwardSearch();
    } else

		// diff
		if (action.equals("diff")) {
			openDialog.setDialogTitle("Diff View");
			openDialog.setDialogType(JFileChooser.OPEN_DIALOG);
			openDialog.setCurrentDirectory(lastDocDir);
			if (openDialog.showDialog() != SCEFileChooser.RESULT_OK) return;
			if (openDialog.getSelectedFile() == null) return;

			try {
				String text = SourceCodeEditor.readFile(openDialog.getSelectedFile().getCanonicalPath());
				getActiveEditor().diffView(openDialog.getSelectedFile().getCanonicalPath(), text);
			} catch (IOException e1) {
				e1.printStackTrace();
			}
		} else

		// compile
		if (action.equals("pdf")) {
			saveAll();
			compile(LatexCompiler.Type.pdf);
		} else if (action.equals("dvi")) {
			saveAll();
			compile(LatexCompiler.Type.dvi);
		} else if (action.equals("dvi + ps")) {
			saveAll();
			compile(LatexCompiler.Type.dvi_ps);
		} else if (action.equals("dvi + ps + pdf")) {
			saveAll();
			compile(LatexCompiler.Type.dvi_ps_pdf);
		} else

		// svn update
		if (action.equals("svn update")) {
			saveAll();
			ArrayList<SVN.UpdateResult> results;
			try {
				results = SVN.getInstance().update(getMainEditor().getFile().getParentFile());
        try {
          svnView.checkForUpdates();
        } catch(Throwable ignored) {}
			} catch (Exception exception) {
				exception.printStackTrace();
				statusBar.showTextError("SVN update failed", "SVN update failed: " + exception.getMessage());
				return;
			}
			StringBuilder builder = new StringBuilder();
			builder.append("<html>");
			builder.append("SVN update: " + (results.size() == 0 ? "All Quiet on the Western Front" : "<br>"));
			builder.append("<br>");
			svnList("Updated/Merged:", builder, results, new SVN.UpdateResult.Type[]{SVN.UpdateResult.Type.update, SVN.UpdateResult.Type.merged});
			svnList("Added:", builder, results, new SVN.UpdateResult.Type[]{SVN.UpdateResult.Type.add});
			svnList("Deleted:", builder, results, new SVN.UpdateResult.Type[]{SVN.UpdateResult.Type.delete});
			svnList("Conflicts:", builder, results, new SVN.UpdateResult.Type[]{SVN.UpdateResult.Type.conflict});
			builder.append("</html>");

			checkExternalModification(false);
			statusBar.showMessage("SVN update", builder.toString());
			statusBar.setUpdatesAvailableVisible(false);
		} else
		// svn commit
		if (action.equals("svn commit")) {
			saveAll();
			String message = (String) JOptionPane.showInputDialog(
							this,
							"Commit message:",
							"SVN commit",
							JOptionPane.QUESTION_MESSAGE,
							null,
							null,
							"");
			if (message != null) {
				Tuple2<Boolean, String> result = null;
				try {
					result = SVN.getInstance().commit(getMainEditor().getFile().getParentFile(), message);
				} catch (Exception exception) {
					exception.printStackTrace();
					statusBar.showMessage("SVN update failed", "SVN update failed: " + exception.getMessage());
					return;
				}
				statusBar.showMessage("SVN commit", "<html>SVN commit:<br><br>" + result._2 + "</html>");
			}
		} else if (action.equals("set master document")) {
			setMasterDocument(getActiveEditor().getResource());
		} else if (action.equals("select next tab")) {
			// select the right tab
			int index = tabbedPane.getSelectedIndex() + 1;
			if (index >= tabbedPane.getTabCount()) index = 0;
			tabbedPane.setSelectedIndex(index);
		} else if (action.equals("select previous tab")) {
			// select the left tab
			int index = tabbedPane.getSelectedIndex() - 1;
			if (index < 0) index = tabbedPane.getTabCount() - 1;
			tabbedPane.setSelectedIndex(index);
		} else if (action.equals("move tab left")) {
			moveTabToLeft();
		} else if (action.equals("move tab right")) {
			moveTabToRight();
		} else if (action.equals("font")) {
			SCEFontWindow fontDialog = new SCEFontWindow(GProperties.getEditorFont().getFamily(), GProperties.getEditorFont().getSize(), this);
			fontDialog.setVisible(true);
    } else if (action.equals("template editor")) {
      TemplateEditor templateEditor = new TemplateEditor(this);
      templateEditor.setVisible(true);
    } else if (action.equals("wizard")) {
      Wizard wizard = new Wizard(this);
      wizard.setVisible(true);
		} else if (action.equals("font window") || action.equals("font window cancel")) {
			SCEFontWindow fontDialog = (SCEFontWindow) e.getSource();
			changeFont(fontDialog.getFontName(), fontDialog.getFontSize());
		} else if (action.equals("global settings")) {
			open(new Doc.FileDoc(GProperties.CONFIG_FILE));
		} else if (action.equals("update")) {
			checkForUpdates(false);
		} else if (action.equals("show change log")) {
			open(new Doc.FileDoc(CHANGELOG));
		} else if (action.equals("about")) {
			AboutDialog aboutDialog = new AboutDialog(version);
			aboutDialog.showIt();
    } else if (action.equals("acknowledgements")) {
      AcknowledgementsDialog acknowledgementsDialog = new AcknowledgementsDialog(version);
      acknowledgementsDialog.setVisible(true);
		} else if (action.equals("stack trace")) {
			new ThreadInfoWindow();
		}
  }

	public void performForwardSearch() {
		try {
		  SourceCodeEditor editor = getActiveEditor();
		  int line = editor.getTextPane().getCaret().getRow()+1;
		  String texfile = editor.getFile().getAbsolutePath();
			String mainFile = getMainEditor().getFile().getAbsolutePath();
			int extensionIndex = mainFile.lastIndexOf(".tex");
			if (extensionIndex == -1) return; // no tex file
						String file = mainFile.substring(0, extensionIndex);

			String forwardSearchCommand = GProperties.getString("forward search.viewer");
			if (forwardSearchCommand == null || forwardSearchCommand.trim().equals("")) {
				JOptionPane.showMessageDialog(this, "Please go to Settings->Forward Search and select the document viewer of your choice\nor configure the command to start the viewer manually in the global settings (forward search.viewer).", "Document viewer needs to be set", JOptionPane.INFORMATION_MESSAGE);
				return;
			}

			ArrayList<String> list = StringUtils.tokenize(forwardSearchCommand);
		  String[] array = new String[list.size()];
		  list.toArray(array);

		  for(int index = 0; index < array.length; index++) {
		    String token = array[index];
		    token = token.replaceAll("%line", line+"");
		    token = token.replaceAll("%file", file);
		    token = token.replaceAll("%texfile", texfile);
		    token = token.replaceAll("&nbsp;", " ");
		    array[index] = token;
		    System.out.println(token);
		  }
		  ProcessUtil.exec(array, getMainEditor().getFile().getParentFile());
		} catch(Exception ex) {
		  logger.log(Level.SEVERE, "Forward search failed", ex);
			JOptionPane.showMessageDialog(this, "Forward search failed", ex.getMessage(), JOptionPane.ERROR_MESSAGE);
		}
	}

	private void showTool(int tab) {
		if (!toolsTab.isVisible()) toolsTab.setVisible(true);

    if (toolsTab.getSelectedIndex() != tab) {
      toolsTab.setSelectedIndex(tab);
    }

    if (toolsTab.getVisibleRect().getHeight() < 5) {
      textToolsSplit.setResizeWeight(1 - GProperties.getDouble("main_window.tools_panel.height"));
      textToolsSplit.resetToPreferredSizes();
      toolsTab.getSelectedComponent().requestFocus();
    }
	}

  private void toggleTool(int tab) {
	  if (toolsTab.isVisible() && toolsTab.getSelectedIndex() == tab) {
		  // if tool is visible -> hide it
			toolsTab.setVisible(false);
			getActiveEditor().requestFocus();
		} else {
		  // else -> show it
		  showTool(tab);
    }
  }

  private void svnList(String message, StringBuilder builder, ArrayList<SVN.UpdateResult> results, SVN.UpdateResult.Type[] types) {
	  int count = 0;
    boolean first = true;
    for (SVN.UpdateResult result : results) {
	    if (count >= 30) {
		    builder.append("<li>...</li>");
		    break;
	    }
      for (SVN.UpdateResult.Type type : types) {
        if (result.getType() == type) {
          if (first) {
            builder.append(message);
            builder.append("<ul>");
            first = false;
          }
          builder.append("<li>" + result.getFile().getName() + "</li>");
        }
      }
	    count++;
    }
    if (!first) builder.append("</ul>");
  }

  private synchronized void checkExternalModification(boolean showPopup) {
    ArrayList<String> reloaded = new ArrayList<String>();
    for (int tab = 0; tab < tabbedPane.getTabCount(); tab++) {
      SourceCodeEditor<Doc> editor = getEditor(tab);

      File file;
      if (editor.getResource() instanceof Doc.FileDoc) {
        Doc.FileDoc fileDoc = (Doc.FileDoc) editor.getResource();
        file = fileDoc.getFile();
      } else {
        continue;
      }

      // check that the file exists
      if (!file.exists()) continue;

      Long oldModified = lastModified.get(file);
      if(oldModified == null) continue;

      Long newModified = file.lastModified();
      // has the file been changed?
      if (!oldModified.equals(newModified)) {
        boolean reload = false;
        if (editor.getTextPane().getDocument().isModified()) {
          int choice = JOptionPane.showOptionDialog(
                  this,
                  "The document `" + file.toString() + "'\n" +
                          "has been modified externally as well as in the editor.\n" +
                          "Overwriting will discard the external modifications in file.\n" +
                          "Reloading will discard the modifications in the editor.\n",
                  "External Modification",
                  JOptionPane.WARNING_MESSAGE,
                  JOptionPane.YES_NO_OPTION,
                  null,
                  new Object[]{"Overwrite", "Reload", "Don't reload"},
                  2
          );
          if (choice == 0) save(editor);
          if (choice == 1) reload = true;
          if (choice == 2) lastModified.put(file,  newModified);
        } else {
          reload = true;
        }
        if (reload) {
          reloaded.add(file.getName());
          try {
            editor.reload();
            lastModified.put(file, newModified);
          } catch (IOException e1) {
            e1.printStackTrace();
          }
        }
      }
    }
    if (reloaded.size() > 0 && showPopup) {
      StringBuilder builder = new StringBuilder();
      builder.append("<html>");
      builder.append("The following documents have been externally modified and reloaded:<br>");
      builder.append("<ul>");
      for (String name : reloaded) builder.append("<li>" + name + "</li>");
      builder.append("</ul>");
      builder.append("</html>");

      statusBar.showMessage("External modifications", builder.toString());
    }
  }


  private void checkForUpdates(boolean startup) {
    if (startup && !GProperties.getBoolean("check_for_updates")) return;

    // check for new version
    if (updater.isNewVersionAvailable()) {
      // ask user if (s)he wants to update
      String changes = "";
      if (anyModifications()) {
        changes = "Your changes will be saved beforehand, since JLatexEditor has to be restarted. ";
      }
      if (JOptionPane.showConfirmDialog(this, "A new version of the JLatexEditor is available. " + changes + "Do you want to update?",
              "JLatexEditor - Updater", JOptionPane.YES_NO_CANCEL_OPTION, JOptionPane.QUESTION_MESSAGE) == JOptionPane.YES_OPTION) {

        // save all files
        if (!saveAll()) return;

        // perform update
        try {
          if (updater.performUpdate(true)) {
            // restart the editor
            System.exit(255);
          }
        } catch (IOException e) {
          e.printStackTrace();
        }
      }
    } else {
      if (!startup) {
        JOptionPane.showMessageDialog(this, "JLatexEditor is up-to-date.", "JLatexEditor - Updater", JOptionPane.INFORMATION_MESSAGE);
      }
    }
  }

	private void setMasterDocument(Doc doc) {
		int mainEditorTab = getTab(doc);
		mainEditor = getEditor(mainEditorTab);
		for (int tab = 0; tab < tabbedPane.getTabCount(); tab++) {
		  tabbedPane.getTabComponentAt(tab).setForeground(Color.BLACK);
		}
		tabbedPane.getTabComponentAt(mainEditorTab).setForeground(Color.RED);

		SCEManager.getBackgroundParser().parse();
		svnView.checkForUpdates();
	}

	private void moveTabToLeft() {
		Component focusOwner = this.getFocusOwner();
		int tab = tabbedPane.getSelectedIndex();
		if (tab <= 0) return;
		SourceCodeEditor<Doc> editor = getEditor(tab);
		Component tabComponent = tabbedPane.getTabComponentAt(tab);
		tabbedPane.removeChangeListener(this);
		tabbedPane.remove(tab);
		tabbedPane.insertTab(editor.getResource().getName(), null, editor, null, tab-1);
		tabbedPane.setTabComponentAt(tab-1, tabComponent);
		tabbedPane.setSelectedIndex(tab-1);
		tabbedPane.addChangeListener(this);
		focusOwner.requestFocus();
	}

	private void moveTabToRight() {
		Component focusOwner = this.getFocusOwner();
		int tab = tabbedPane.getSelectedIndex();
		if (tab >= tabbedPane.getTabCount()-1) return;
		SourceCodeEditor<Doc> editor = getEditor(tab);
		Component tabComponent = tabbedPane.getTabComponentAt(tab);
		tabbedPane.removeChangeListener(this);
		tabbedPane.remove(tab);
		tabbedPane.insertTab(editor.getResource().getName(), null, editor, null, tab+1);
		tabbedPane.setTabComponentAt(tab+1, tabComponent);
		tabbedPane.setSelectedIndex(tab+1);
		tabbedPane.addChangeListener(this);
		focusOwner.requestFocus();
	}

  public void windowOpened(WindowEvent e) {
    SwingUtilities.invokeLater(new Runnable() {
      public void run() {
        // recent files
        loadRecent();

        // reopen last files
        reopenLast();

	      String workingDir = System.getProperty("jlatexeditor.working_dir");

	      openDialog.setDialogTitle("Open");
	      if (workingDir != null) {
		      openDialog.setCurrentDirectory(new File(workingDir));
	      }

        // open files given in command line
        for (String fileName : filesToOpen) {
	        // make path absolute if working dir is given
	        if (workingDir != null && !(new File(fileName)).isAbsolute()) {
		        fileName = workingDir + "/" + fileName;
	        }
	        // update directory of open dialog
	        File file = new File(fileName);
	        if (file.isAbsolute()) {
		        openDialog.setCurrentDirectory(file.getParentFile());
	        }
	        // open file
	        FileLineNr fileLineNr = new FileLineNr(fileName);
	        open(new Doc.FileDoc(fileLineNr.file), fileLineNr.lineNr - 1);
        }
	      getActiveEditor().getFocusedPane().requestFocus();

        setExtendedState(getExtendedState() | GProperties.getInt("main_window.maximized_state"));
	      // TODO
	      //textToolsSplit.setDividerLocation((int) (GProperties.getDouble("main_window.tools_location") * getHeight()));
      }
    });
  }

  public void valueChanged(TreeSelectionEvent e) {
	  if (!(structureTree.getLastSelectedPathComponent() instanceof BackgroundParser.StructureEntry)) return;

    BackgroundParser.StructureEntry structureEntry = (BackgroundParser.StructureEntry) structureTree.getLastSelectedPathComponent();
    if(structureEntry == null) return;
    
    File file = new File(structureEntry.getFile());
    if (file.exists() && file.isFile()) {
      SourceCodeEditor<Doc> editor = open(new Doc.FileDoc(file));
      editor.getTextPane().getCaret().moveTo(structureEntry.getLineNr(), 0, false);
    }
  }

  public void close(int tabIndex) {
    closeTab(tabIndex);
  }

  private class ShutdownHook extends Thread {
	  private JLatexEditorJFrame mainWindow;

	  private ShutdownHook(JLatexEditorJFrame mainWindow) {
		  super("ShutdownHook");
		  this.mainWindow = mainWindow;
	  }

	  public void run() {
      try {
	      // save windows position and state
	      GProperties.setMainWindowBounds(mainWindow.getBounds(), mainWindow.getExtendedState() & JFrame.MAXIMIZED_BOTH);
	      // TODO
	      //GProperties.set("main_window.tools_location", "" + ((double ) textToolsSplit.getDividerLocation() / getHeight()));
	      GProperties.save();

	      // save last session
        PrintWriter writerLast = new PrintWriter(new FileWriter(FILE_LAST_SESSION));
        for (int tabNr = 0; tabNr < tabbedPane.getTabCount(); tabNr++) {
          SourceCodeEditor<Doc> editor = getEditor(tabNr);
          if (!(editor.getResource() instanceof Doc.FileDoc)) continue;
	        boolean masterDoc = editor.equals(mainEditor);
          writerLast.println(editor.getFile().getCanonicalPath() + ":" + editor.getTextPane().getCaret().getRow() + (masterDoc ? ":*" : ""));
        }
        writerLast.close();

	      // save recent files
        PrintWriter writerRecent = new PrintWriter(new FileWriter(FILE_RECENT));
        for (String name : recentFiles) writerRecent.println(name);
        writerRecent.close();
      } catch (IOException ignored) {
      }
    }
  }

	public void quit() {
		if (GProperties.getBoolean("ask_for_saving_files_before_closing") && anyModifications()) {
			int answer = JOptionPane.showConfirmDialog(this, "Some files have been modified.  Do you want to save them?", "Save modified files?", JOptionPane.YES_NO_CANCEL_OPTION);
			switch (answer) {
				case JOptionPane.YES_OPTION:
					if (saveAll()) {
					  System.exit(0);
					}
				case JOptionPane.NO_OPTION:
					System.exit(0);
			}
		} else {
			System.exit(0);
		}
	}

  public void windowClosing(WindowEvent e) {
	  quit();
  }

  public void windowClosed(WindowEvent e) {
  }

  public void windowIconified(WindowEvent e) {
  }

  public void windowDeiconified(WindowEvent e) {
  }

  public void windowActivated(WindowEvent e) {
  }

  public void windowDeactivated(WindowEvent e) {
  }

  public void stateChanged(ChangeEvent e) {
    if (e.getSource() == tabbedPane) {
      // document tab has been changed
      editorChanged();
    }
  }

  private void editorChanged() {
    // reattach error highlighter
    errorHighlighting.detach();
    SourceCodeEditor<Doc> editor = getActiveEditor();
    errorHighlighting.attach(editor, errorView);
    errorHighlighting.update();

    // update window title
    AbstractResource resource = editor.getResource();
    String fileName = resource.toString();
    if (resource instanceof Doc.FileDoc) {
      Doc.FileDoc fileDoc = (Doc.FileDoc) resource;
      File file = fileDoc.getFile();
	    lastDocDir = file.getParentFile();
      fileName = file.getName();
      for (int i = 0; i < GProperties.getInt("main_window.title.number_of_parent_dirs_shown"); i++) {
        file = file.getParentFile();
        if (file != null) {
          fileName = file.getName() + "/" + fileName;
        } else {
          break;
        }
      }
      if (file != null) {
        if (file.getParentFile() == null ||
                file.getParentFile() != null && file.getParentFile().getParentFile() == null) {
          fileName = fileDoc.getFile().getAbsolutePath();
        } else {
          fileName = ".../" + fileName;
        }
      }
    }
    setTitle(fileName + "  -  " + windowTitleSuffix);
  }

  public void mouseDragged(MouseEvent e) {
  }

  public void mouseMoved(MouseEvent e) {
    JMenuItem item = (JMenuItem) e.getSource();
    String action = item.getActionCommand();
    String fontName = action.substring(action.indexOf(": ") + 2);
    GProperties.setEditorFont(new Font(fontName, Font.PLAIN, GProperties.getEditorFont().getSize()));

    SourceCodeEditor<Doc> editor = getActiveEditor();
    SCEPane pane = editor.getTextPane();
    pane.setFont(GProperties.getEditorFont());
    LatexStyles.addStyles(pane.getDocument());
    editor.repaint();
  }

  public void changeFont(String fontName, int fontSize) {
    GProperties.setEditorFont(new Font(fontName, Font.PLAIN, fontSize));

    SourceCodeEditor<Doc> editor = getActiveEditor();
    SCEPane pane = editor.getTextPane();
    pane.setFont(GProperties.getEditorFont());
    LatexStyles.load();
    LatexStyles.addStyles(pane.getDocument());
    editor.repaint();
  }

  public static class ShortcutChangeListener implements PropertyChangeListener {
    private JMenuItem menuItem;

    public ShortcutChangeListener(JMenuItem menuItem) {
      this.menuItem = menuItem;
    }

    public void propertyChange(PropertyChangeEvent evt) {
      String shortcut = (String) evt.getNewValue();
      if (shortcut == null) {
        shortcut = "";
      }
      menuItem.setAccelerator(KeyStroke.getKeyStroke(shortcut));
    }
  }

  private class TabLabel extends JPanel implements MouseListener, SCEModificationStateListener {
    private Doc doc;
    private JLabel label;

    private TabLabel(Doc doc, SourceCodeEditor<Doc> editor) {
      this.doc = doc;
      setOpaque(false);

      label = new JLabel(doc.getName());

      BorderLayout layout = new BorderLayout(4, 1);
      setLayout(layout);
      setBackground(new Color(255, 255, 255, 255));
      add(label, BorderLayout.CENTER);

      addMouseListener(this);
      editor.getTextPane().getDocument().addSCEModificationStateListener(this);
    }

    public AbstractResource getDoc() {
      return doc;
    }

    public void setDoc(Doc doc) {
      this.doc = doc;
      label.setText(doc.getName());
    }

    public boolean contains(int x, int y) {
      return x >= -4 && x <= getWidth() + 4 && y >= -2 && y <= getHeight() + 2;
    }

    public void mouseClicked(MouseEvent e) {
      tabbedPane.setSelectedIndex(getTab(doc));
      if (e.getClickCount() >= 2) {
	      setMasterDocument(doc);
      }
    }

    public void setForeground(Color fg) {
      super.setForeground(fg);
      if (label != null) label.setForeground(fg);
    }

    public void mousePressed(MouseEvent e) {
    }

    public void mouseReleased(MouseEvent e) {
    }

    public void mouseEntered(MouseEvent e) {
    }

    public void mouseExited(MouseEvent e) {
    }

    // SCEModificationStateListener
    public void modificationStateChanged(boolean modified) {
      label.setText(modified ? "*" + doc.getName() : doc.getName());
    }
  }
}


/**
 * @author Jörg Endrullis
 * @author Stefan Endrullis
 */

package jlatexeditor;

import de.endrullis.utils.ProgramUpdater;
import jlatexeditor.codehelper.*;
import jlatexeditor.errorhighlighting.LatexCompiler;
import jlatexeditor.errorhighlighting.LatexErrorHighlighting;
import jlatexeditor.gproperties.GProperties;
import jlatexeditor.gproperties.GPropertiesCodeHelper;
import jlatexeditor.gproperties.GPropertiesStyles;
import jlatexeditor.gproperties.GPropertiesSyntaxHighlighting;
import jlatexeditor.gui.AboutDialog;
import jlatexeditor.gui.LocalHistory;
import jlatexeditor.gui.StatusBar;
import jlatexeditor.gui.SymbolsPanel;
import jlatexeditor.quickhelp.LatexQuickHelp;
import jlatexeditor.syntaxhighlighting.LatexStyles;
import jlatexeditor.syntaxhighlighting.LatexSyntaxHighlighting;
import jlatexeditor.tools.SVN;
import sce.codehelper.CombinedCodeHelper;
import sce.component.*;
import sce.syntaxhighlighting.SyntaxHighlighting;
import util.Pair;
import util.StreamUtils;
import util.filechooser.SCEFileChooser;

import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.plaf.basic.BasicSplitPaneUI;
import java.awt.*;
import java.awt.event.*;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.*;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;

public class JLatexEditorJFrame extends JFrame implements ActionListener, WindowListener, ChangeListener, MouseMotionListener, KeyListener {
  public static final File FILE_LAST_SESSION = new File(System.getProperty("user.home") + "/.jlatexeditor/last.session");
  public static final File FILE_RECENT = new File(System.getProperty("user.home") + "/.jlatexeditor/recent");
  private JMenu recentFilesMenu;
  private ArrayList<String> recentFiles = new ArrayList<String>();

  private static String UNTITLED = "Untitled";

	private static String version = "*Bleeding Edge*";
	private static boolean devVersion = true;
	private static String windowTitleSuffix;
	static {
		try {
			version = StreamUtils.readFile("version.txt");
			devVersion = false;
		} catch (IOException ignored) {}
		windowTitleSuffix = "JLatexEditor " + version;
	}

  private JMenuBar menuBar = null;
  private JTabbedPane tabbedPane = null;
  private JSplitPane textToolsSplit = null;
  private JTabbedPane toolsTab = null;
  private ErrorView errorView = null;
	private SymbolsPanel symbolsPanel;
	private JSplitPane symbolsTextSplit;

  private StatusBar statusBar = null;

  // command line arguments
  private String args[];

  // last directory of the opening dialog
  private JFileChooser openDialog = new SCEFileChooser();

  // compile thread
  private LatexCompiler latexCompiler = null;
  // main file to compile
  private SourceCodeEditor mainEditor = null;

  private LatexErrorHighlighting errorHighlighting = new LatexErrorHighlighting();

  // file changed time
  private Timer modificationTimer = new Timer(2000, this);
  private HashMap<File,Long> lastModified = new HashMap<File, Long>();

	private final ProgramUpdater updater = new ProgramUpdater("JLatexEditor update", "http://endrullis.de/JLatexEditor/update/");

  // background parser
  private BackgroundParser backgroundParser;
  private HashMap<URI,Doc> docMap = new HashMap<URI,Doc>();

	public static void main(String args[]){
    /*
    try {
      //System.setProperty("swing.aatext", "true");
      System.setProperty("Quaqua.tabLayoutPolicy","wrap");
      System.setProperty("apple.laf.useScreenMenuBar", "true");
      System.setProperty("com.apple.macos.useScreenMenuBar", "true");

      try {
        Methods.invokeStatic(JFrame.class, "setDefaultLookAndFeelDecorated", Boolean.TYPE, Boolean.TRUE);
        Methods.invokeStatic(JDialog.class, "setDefaultLookAndFeelDecorated", Boolean.TYPE, Boolean.TRUE);
      } catch (NoSuchMethodException e) { }

      String lafName = QuaquaManager.getLookAndFeelClassName();
      if(!lafName.equals("default")) {
        if(lafName.equals("system")) {
          lafName = UIManager.getSystemLookAndFeelClassName();
        } else if (lafName.equals("crossplatform")) {
          lafName = UIManager.getCrossPlatformLookAndFeelClassName();
        }

        LookAndFeel laf = (LookAndFeel) Class.forName(lafName).newInstance();
        UIManager.setLookAndFeel(laf);
      }
    } catch (Exception e) { }
    */

    UIManager.put("TabbedPaneUI", "util.gui.SCETabbedPaneUI");
    UIManager.put("List.timeFactor", 200L);
    /*
    UIManager.put("Menu.background", SCETabbedPaneUI.BLUE);
    UIManager.put("Menu.selectionBackground", SCETabbedPaneUI.BLUE);
    UIManager.put("MenuBar.gradient", Arrays.asList(1.0f, 0.0f, SCETabbedPaneUI.BLUE, SCETabbedPaneUI.BLUE.brighter(), SCETabbedPaneUI.BLUE.darker()));
    */
    
	  new AboutDialog(null).showAndAutoHideAfter(5000);

	  JLatexEditorJFrame latexEditor = new JLatexEditorJFrame(args);
    latexEditor.setSize(1024,800);
    latexEditor.setVisible(true);
  }

  public JLatexEditorJFrame(String args[]){
	  super(windowTitleSuffix);
    this.args = args;
    setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
    addWindowListener(this);
    Runtime.getRuntime().addShutdownHook(new ShutdownHook());

	  initFileChooser();

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
	  fileMenu.add(createMenuItem("Close", "close", 'C'));
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
	  editMenu.add(createMenuItem("Comment", "comment", 'o'));
	  editMenu.add(createMenuItem("Uncomment", "uncomment", 'u'));
	  editMenu.addSeparator();
	  editMenu.add(createMenuItem("Diff", "diff", 'D'));

	  JMenu viewMenu = new JMenu("View");
		viewMenu.setMnemonic('V');
	  menuBar.add(viewMenu);

		viewMenu.add(createMenuItem("Symbols", "symbols", 'y'));
		viewMenu.add(createMenuItem("Status Bar", "status bar", 'S'));
		viewMenu.add(createMenuItem("Compile", "compile", 'S'));
		viewMenu.add(createMenuItem("Local History", "local history", 'S'));

    JMenu buildMenu = new JMenu("Build");
	  buildMenu.setMnemonic('B');
    menuBar.add(buildMenu);

	  buildMenu.add(createMenuItem("pdf", "pdf", null));
	  buildMenu.add(createMenuItem("dvi", "dvi", null));
	  buildMenu.add(createMenuItem("dvi + ps", "dvi + ps", null));
	  buildMenu.add(createMenuItem("dvi + ps + pdf", "dvi + ps + pdf", null));

    JMenu vcMenu = new JMenu("Version Control");
	  vcMenu.setMnemonic('C');
    menuBar.add(vcMenu);

	  vcMenu.add(createMenuItem("SVN update", "svn update", 'u'));
	  vcMenu.add(createMenuItem("SVN commit", "svn commit", 'c'));

    JMenu settingsMenu = new JMenu("Settings");
    settingsMenu.setMnemonic('S');
    menuBar.add(settingsMenu);

	  settingsMenu.add(createMenuItem("Font", "font", 'F'));
	  settingsMenu.add(createMenuItem("Global Settings", "global settings", 'G'));

	  JMenu helpMenu = new JMenu("Help");
	  helpMenu.setMnemonic('H');
	  menuBar.add(helpMenu);

	  JMenuItem updateMenuItem = createMenuItem("Check for update", "update", 'u');
	  if (devVersion) updateMenuItem.setVisible(false);
	  helpMenu.add(updateMenuItem);
	  helpMenu.add(createMenuItem("About", "about", 'A'));
	  
    // error messages
    toolsTab = new JTabbedPane();
    errorView = new ErrorView(this);
    toolsTab.addTab("Compile", errorView);
    toolsTab.addTab("Local History", new LocalHistory(this));

    // tabs for the files
    tabbedPane = new JTabbedPane();
    try {
      addTab(new UntitledDoc());
    } catch (IOException ignored) {}

    // symbols panel
    symbolsPanel = new SymbolsPanel(this);
    symbolsPanel.setVisible(false);
    symbolsTextSplit = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, true, symbolsPanel, tabbedPane);
    symbolsTextSplit.setContinuousLayout(false);
    symbolsTextSplit.setDividerLocation(0);
    symbolsTextSplit.setOneTouchExpandable(true);
    ((BasicSplitPaneUI) symbolsTextSplit.getUI()).getDivider().addMouseListener(new MouseAdapter() {
      public void mousePressed(MouseEvent e) {
        symbolsPanel.setVisible(true);
      }
    });

	  textToolsSplit = new JSplitPane(JSplitPane.VERTICAL_SPLIT, true, symbolsTextSplit, toolsTab);
    textToolsSplit.setOneTouchExpandable(true);
    textToolsSplit.setResizeWeight(1 - GProperties.getDouble("tools_panel.height"));
	  ((BasicSplitPaneUI) textToolsSplit.getUI()).getDivider().addMouseListener(new MouseAdapter() {
	    public void mousePressed(MouseEvent e) {
		    toolsTab.setVisible(true);
	    }
	  });

    statusBar = new StatusBar(this);

    cp.add(textToolsSplit, BorderLayout.CENTER);
    cp.add(statusBar, BorderLayout.SOUTH);
    cp.validate();

    errorHighlighting.attach(getEditor(0), errorView);

    // file changed timer
    modificationTimer.setActionCommand("timer");
    modificationTimer.start();

	  // search for updates in the background
	  if (!devVersion) {
			new Thread(){
				public void run() {
					checkForUpdates(true);
				}
			}.start();
	  }

    // background parser
    backgroundParser = new BackgroundParser(this);
    backgroundParser.start();

	  PropertyChangeListener fontChangeListener = new PropertyChangeListener() {
		  public void propertyChange(PropertyChangeEvent evt) {
			  changeFont(GProperties.getString("editor.font.name"), GProperties.getInt("editor.font.size"));
		  }
	  };
	  GProperties.addPropertyChangeListener("editor.font.name", fontChangeListener);
	  GProperties.addPropertyChangeListener("editor.font.size", fontChangeListener);
	  GProperties.addPropertyChangeListener("editor.font.antialiasing", fontChangeListener);
  }

  /**
   * Reopen the files that were open the last time.
   */
  private void reopenLast() {
    try {
      BufferedReader reader = new BufferedReader(new FileReader(FILE_LAST_SESSION));
      String line;
      while((line = reader.readLine()) != null) {
        int colon = line.indexOf(':');
        if(colon == -1) colon = line.length();

        File file = new File(line.substring(0, colon));
        int lineNr = colon >= line.length() ? 0 : Integer.parseInt(line.substring(colon+1));

        if(file.exists() && file.isFile()) {
          SourceCodeEditor editor = open(new FileDoc(file));
          editor.getTextPane().getCaret().moveTo(lineNr, 0);
        }
      }
      reader.close();
    } catch (IOException ignored) {}
  }

  private void loadRecent() {
    recentFiles.clear();
    try {
      BufferedReader reader = new BufferedReader(new FileReader(FILE_RECENT));
      String line;
      while((line = reader.readLine()) != null) recentFiles.add(line);
      reader.close();
    } catch (IOException ignored) {}
  }

  private void addRecent(File file) {
    try {
      String fileName = file.getCanonicalPath();
      recentFiles.remove(fileName);
      recentFiles.add(0, fileName);
      if(recentFiles.size() > 20) recentFiles.remove(20);
    } catch (IOException ignored) { }
    updateRecentMenu();
  }

  private void updateRecentMenu() {
    recentFilesMenu.removeAll();
    for(String name : recentFiles) {
      JMenuItem item = new JMenuItem(name);
      item.setActionCommand("open recent:" + name);
      item.addActionListener(this);
      recentFilesMenu.add(item);
    }
    recentFilesMenu.addSeparator();
    recentFilesMenu.add(createMenuItem("Clear List", "clear recent", 'C'));
  }

	private JMenuItem createMenuItem(String label, String command, Character mnemonic) {
		JMenuItem menuItem = new JMenuItem(label);
		menuItem.setActionCommand(command);
		if (mnemonic != null) {
			menuItem.setMnemonic(mnemonic);
		}
		String shorcutString = GProperties.getString("shortcut." + command);
		if (shorcutString != null && !shorcutString.equals("")) {
			menuItem.setAccelerator(KeyStroke.getKeyStroke(shorcutString));
		}
		menuItem.addActionListener(this);

		// add shortcut change listener
		GProperties.addPropertyChangeListener("shortcut." + command, new ShortcutChangeListener(menuItem));

		return menuItem;
	}

	private void initFileChooser() {
		openDialog.addChoosableFileFilter(new FileNameExtensionFilter(
			"LaTeX files (*.tex, *.def, *.bib)", "tex", "def", "bib"));
	}

	private SourceCodeEditor<Doc> createLatexSourceCodeEditor() {
    SourceCodeEditor<Doc> editor = new SourceCodeEditor<Doc>(null);

    SCEPane scePane = editor.getTextPane();
    SCEDocument document = scePane.getDocument();

    // add some styles to the document
    LatexStyles.addStyles(document);

    // syntax highlighting
    SyntaxHighlighting syntaxHighlighting = new LatexSyntaxHighlighting(scePane);
    syntaxHighlighting.start();

	  // code completion and quick help
		CombinedCodeHelper codeHelper = new CombinedCodeHelper();
		codeHelper.addPatternHelper(new BibCodeHelper(backgroundParser));
    codeHelper.addPatternHelper(new LabelCodeHelper(backgroundParser));
		codeHelper.addPatternHelper(new IncludeCodeHelper());
		codeHelper.addPatternHelper(new LatexCommandCodeHelper("(\\\\[a-zA-Z]*)", "data/codehelper/commands.xml"));
		codeHelper.addPatternHelper(new WordCompletion(backgroundParser));
	  scePane.setCodeHelper(codeHelper);
	  scePane.setTabCompletion(new LatexCommandCodeHelper("([a-zA-Z]*)", "data/codehelper/tabCompletion.xml"));
	  scePane.setQuickHelp(new LatexQuickHelp("data/quickhelp/"));

    try {
      scePane.addCodeAssistantListener(new SpellCheckSuggester());
    } catch (Exception ignored) { }

	  new JumpTo(editor, this);

	  scePane.addKeyListener(this);

	  return editor;
  }

	private SourceCodeEditor<Doc> createGPropertiesSourceCodeEditor() {
    SourceCodeEditor<Doc> editor = new SourceCodeEditor<Doc>(null);

    SCEPane scePane = editor.getTextPane();
    SCEDocument document = scePane.getDocument();

		// TODO: user other styles
    // add some styles to the document
    GPropertiesStyles.addStyles(document);

    // syntax highlighting
    GPropertiesSyntaxHighlighting syntaxHighlighting = new GPropertiesSyntaxHighlighting(scePane);
    syntaxHighlighting.start();

	  // code completion and quick help
		CombinedCodeHelper codeHelper = new CombinedCodeHelper();
		codeHelper.addPatternHelper(new GPropertiesCodeHelper());
	  scePane.setCodeHelper(codeHelper);

	  scePane.addKeyListener(this);

	  return editor;
  }

  public BackgroundParser getBackgroundParser() {
    return backgroundParser;
  }

  public int getTab(Doc doc) {
		for(int tab = 0; tab < tabbedPane.getTabCount(); tab++) {
			SourceCodeEditor editor = (SourceCodeEditor) tabbedPane.getComponentAt(tab);
			if(doc.equals(editor.getResource())) return tab;
		}
    return -1;
  }

  public SourceCodeEditor getEditor(int tab) {
    return (SourceCodeEditor) tabbedPane.getComponentAt(tab);
  }

  public SourceCodeEditor getActiveEditor() {
    return getEditor(tabbedPane.getSelectedIndex());
  }

  public SourceCodeEditor getMainEditor() {
    return mainEditor != null ? mainEditor : getActiveEditor();
  }

	private SourceCodeEditor addTab(Doc doc) throws IOException {
		SourceCodeEditor<Doc> editor;
		if (doc.getName().endsWith("global.properties")) {
			editor = createGPropertiesSourceCodeEditor();
		} else {
			editor = createLatexSourceCodeEditor();
		}
		editor.setResource(doc);
		tabbedPane.removeChangeListener(this);
		tabbedPane.addTab(doc.getName(), editor);
		tabbedPane.addChangeListener(this);
		tabbedPane.setTabComponentAt(tabbedPane.getTabCount()-1, new TabLabel(doc, editor));
		tabbedPane.setSelectedIndex(tabbedPane.getTabCount()-1);
	  editor.open(doc);

		return editor;
	}

  public SourceCodeEditor open(Doc doc) {
    try{
	    // is existing object if it already exists, otherwise add it to docMap
	    if (docMap.containsKey(doc.getName())) {
		    doc = docMap.get(doc.getName());
	    } else {
		    docMap.put(doc.getUri(), doc);
	    }

      // already open?
      int tab = getTab(doc);
      if(tab != -1) { tabbedPane.setSelectedIndex(tab); return getEditor(tab); }

      // replacing the untitled tab?
	    boolean closeFirstTab = false;
      if(tabbedPane.getTabCount() == 1) {
	      SourceCodeEditor firstEditor = getEditor(0);
	      if (firstEditor.getResource() instanceof UntitledDoc && !firstEditor.getTextPane().getDocument().isModified()) {
		      closeFirstTab = true;
	      }
      }

	    SourceCodeEditor editor = addTab(doc);
	    if (closeFirstTab) closeTab(0);

	    if (doc instanceof FileDoc) {
	      FileDoc fileDoc = (FileDoc) doc;
		    lastModified.put(fileDoc.file, fileDoc.file.lastModified());
        
        addRecent(fileDoc.getFile());
	    }

	    editorChanged();
      return editor;
    } catch(IOException exc){
      System.out.println("Error opening file");
      exc.printStackTrace();
    }
    return null;
  }

	/**
	 * Returns true if any modifications have been done at an open file.
	 *
	 * @return true if any modifications have been done at an open file
	 */
	public boolean anyModifications () {
		for(int tab = 0; tab < tabbedPane.getTabCount(); tab++) {
		  SourceCodeEditor editor = (SourceCodeEditor) tabbedPane.getComponentAt(tab);
			if (editor.getTextPane().getDocument().isModified()) {
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
    for(int tab = 0; tab < tabbedPane.getTabCount(); tab++) {
      SourceCodeEditor editor = (SourceCodeEditor) tabbedPane.getComponentAt(tab);
      AbstractResource resource = editor.getResource();
      boolean save = (!(resource instanceof UntitledDoc)) || tab == tabbedPane.getSelectedIndex();
      if(save) { if (!save(editor)) all = false; }
    }

    backgroundParser.parse();
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
		if (doc instanceof UntitledDoc) {
      openDialog.setDialogTitle("Save " + doc.getName());
      openDialog.setDialogType(JFileChooser.SAVE_DIALOG);
      if(openDialog.showDialog(this, "Save") != JFileChooser.APPROVE_OPTION) return false;
      file = openDialog.getSelectedFile();
      if(file == null) return false;

      if(file.exists()) {
        int choice = JOptionPane.showOptionDialog(
                this,
                "The file exists! Do you want to overwrite the file?",
                "File Exists",
                JOptionPane.WARNING_MESSAGE,
                JOptionPane.YES_NO_OPTION,
                null,
                new Object[] {"Overwrite", "Cancel"},
                2
        );
        if(choice == 1) return false;
      }

      TabLabel tabLabel = (TabLabel) tabbedPane.getTabComponentAt(getTab(doc));
			docMap.remove(doc.getUri());
      doc = new FileDoc(file);
			docMap.put(doc.getUri(), doc);
      tabLabel.setDoc(doc);
      editor.setResource(doc);
		} else
		if (doc instanceof FileDoc) {
		  FileDoc fileDoc = (FileDoc) doc;
		  file = fileDoc.getFile();
			gPropertiesSaved = file.equals(GProperties.CONFIG_FILE);
		}

    String text = editor.getTextPane().getText();
    try{
      boolean history = true;
      File history_dir = LocalHistory.getHistoryDir(file);
      if(!history_dir.exists()) history = history_dir.mkdirs();

      File file_backup = LocalHistory.getBackupFile(file);
      File file_revisions = LocalHistory.getRevisionsFile(file);

      PrintWriter writer = new PrintWriter(new FileOutputStream(file));
      writer.write(text);
      writer.close();

      if(history) {
        PrintWriter diff_writer = new PrintWriter(new FileOutputStream(file_revisions, true));

        try {
          if(file_backup.exists()) {
            Process process = Runtime.getRuntime().exec(new String[]{
              "diff",
              file.getCanonicalPath(),
              file_backup.getCanonicalPath()
            });
            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));

            String line;
            while((line = reader.readLine()) != null) diff_writer.println(line);

            reader.close();
            process.destroy();
          }
        } catch(Exception diffException) {
          System.err.println("Local history, error starting diff: " + diffException.getMessage());
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
    } catch(IOException ex){
      ex.printStackTrace();
    }

		return true;
  }

  public void compile(int type) {
    SourceCodeEditor editor = mainEditor;
    if(editor == null) {
      editor = (SourceCodeEditor) tabbedPane.getSelectedComponent();
    }

    if(latexCompiler != null) latexCompiler.halt();
    latexCompiler = LatexCompiler.createInstance(type, editor, errorView);

    errorHighlighting.clear();
    latexCompiler.addLatexCompileListener(errorHighlighting);

    latexCompiler.start();
  }

  private void closeTab(int tab) {
    SourceCodeEditor editor = getEditor(tab);
	  if (tabbedPane.getTabCount() > 1) {
		  tabbedPane.removeTabAt(tab);
	  } else {
		  try {
			  addTab(new UntitledDoc());
		  } catch (IOException ignored) {}
		  tabbedPane.removeTabAt(tab);
	  }
    editor.getTextPane().setText("");
  }

  // ActionListener methods
  public void actionPerformed(ActionEvent e){
    String action = e.getActionCommand();
    
    // new file
	  if(action.equals("new")){
		  try {
			  addTab(new UntitledDoc());
		  } catch (IOException ignored) {}
	  } else

		// open a file
    if(action.equals("open")){
      openDialog.setDialogTitle("Open");
      openDialog.setDialogType(JFileChooser.OPEN_DIALOG);
      if(openDialog.showDialog(this, "Open") != JFileChooser.APPROVE_OPTION) return;
      if(openDialog.getSelectedFile() == null) return;

      open(new FileDoc(openDialog.getSelectedFile()));
    } else
    // recent files list
    if(action.startsWith("open recent:")){
      open(new FileDoc(new File(action.substring("open recent:".length()))));
    } else
    if(action.equals("clear recent")){
      recentFiles.clear();
      updateRecentMenu();
    } else
    // save a file
    if(action.equals("save")){
      saveAll();
    } else
    // close
    if(action.equals("close")){
      closeTab(tabbedPane.getSelectedIndex());
    } else
    // exit
    if(action.equals("exit")){
      saveAll();
      System.exit(0);
    } else

    // undo
    if(action.equals("undo")){
      getEditor(tabbedPane.getSelectedIndex()).getTextPane().getUndoManager().undo(false);
    } else
    // undo
    if(action.equals("redo")){
      getEditor(tabbedPane.getSelectedIndex()).getTextPane().getUndoManager().redo(false);
    } else

    // find
    if(action.equals("find")){
      getEditor(tabbedPane.getSelectedIndex()).search();
    } else
    // replace
    if(action.equals("replace")){
      getEditor(tabbedPane.getSelectedIndex()).replace();
    } else
    // find next
    if(action.equals("find next")){
      getEditor(tabbedPane.getSelectedIndex()).getSearch().next();
    } else
    // find previous
    if(action.equals("find previous")){
      getEditor(tabbedPane.getSelectedIndex()).getSearch().previous();
    } else

    // cut
    if(action.equals("cut")){
      getEditor(tabbedPane.getSelectedIndex()).cut();
    } else

    // copy
    if(action.equals("copy")){
      getEditor(tabbedPane.getSelectedIndex()).copy();
    } else

    // paste
    if(action.equals("paste")){
      getEditor(tabbedPane.getSelectedIndex()).paste();
    } else

		// lineComment
		if(action.equals("comment")){
			getEditor(tabbedPane.getSelectedIndex()).lineComment("% ");
		} else
		// lineUncomment
		if(action.equals("uncomment")){
			getEditor(tabbedPane.getSelectedIndex()).lineUncomment("% ");
		} else

		// show/hide symbols
		if(action.equals("symbols")){
			symbolsPanel.setVisible(!symbolsPanel.isVisible());
			symbolsTextSplit.setDividerLocation(symbolsPanel.isVisible() ? GProperties.getDouble("symbols_panel.width") : 0);
		} else
		// show/hide status bar
		if(action.equals("status bar")){
			statusBar.setVisible(!statusBar.isVisible());
		} else
		// show/hide compile
		if(action.equals("compile")){
			toggleTool(0);
		} else
		// show/hide status bar
		if(action.equals("local history")){
			toggleTool(1);
		} else

    // diff
    if(action.equals("diff")){
      openDialog.showDialog(this, "Diff View");
      if(openDialog.getSelectedFile() == null) return;

      try {
        String text = SourceCodeEditor.readFile(openDialog.getSelectedFile().getCanonicalPath());
        getEditor(tabbedPane.getSelectedIndex()).diffView(openDialog.getSelectedFile().getCanonicalPath(), text);
      } catch (IOException e1) {
        e1.printStackTrace();
      }
    } else

    // compile
    if(action.equals("pdf")) { saveAll(); compile(LatexCompiler.TYPE_PDF); } else
    if(action.equals("dvi")) { saveAll(); compile(LatexCompiler.TYPE_DVI); } else
    if(action.equals("dvi + ps")) { saveAll(); compile(LatexCompiler.TYPE_DVI_PS); } else
    if(action.equals("dvi + ps + pdf")) { saveAll(); compile(LatexCompiler.TYPE_DVI_PS_PDF); } else

    // svn update
    if(action.equals("svn update")){
      saveAll();
      ArrayList<SVN.UpdateResult> results;
      try {
        results = SVN.getInstance().update(getMainEditor().getFile().getParentFile());
      } catch (Exception exception) {
        statusBar.showMessage("SVN update failed", "SVN update failed: " + exception.getMessage());
        return;
      }
      StringBuilder builder = new StringBuilder();
      builder.append("<html>");
      builder.append("SVN update: " + (results.size() == 0 ? "All Quiet on the Western Front" : "<br>"));
      builder.append("<br>");
      svnList("Updated/Merged:", builder, results, new int[] { SVN.UpdateResult.TYPE_UPDATE, SVN.UpdateResult.TYPE_MERGED });
      svnList("Added:", builder, results, new int[] { SVN.UpdateResult.TYPE_ADD });
      svnList("Deleted:", builder, results, new int[] { SVN.UpdateResult.TYPE_DELETE });
      svnList("Conflicts:", builder, results, new int[] { SVN.UpdateResult.TYPE_CONFLICT });
      builder.append("</html>");

      checkExternalModification(false);
      statusBar.showMessage("SVN update", builder.toString());
      statusBar.setUpdatesAvailableVisible(false);
    } else
    // svn commit
    if(action.equals("svn commit")){
      saveAll();
      String message = (String)JOptionPane.showInputDialog(
                          this,
                          "Commit message:",
                          "SVN commit",
                          JOptionPane.QUESTION_MESSAGE,
                          null,
                          null,
                          "");
      if(message != null) {
        Pair<Boolean,String> result = SVN.getInstance().commit(getMainEditor().getFile().getParentFile(), message);
        statusBar.showMessage("SVN commit", "<html>SVN commit:<br><br>" + result.second + "</html>");
      }
    } else

	  if(action.equals("font")){
		  SCEFontWindow fontDialog = new SCEFontWindow(GProperties.getEditorFont().getFamily(), GProperties.getEditorFont().getSize(), this);
		  fontDialog.setVisible(true);
	  } else
    if(action.equals("font window") || action.equals("font window cancel")){
      SCEFontWindow fontDialog = (SCEFontWindow) e.getSource();
      changeFont(fontDialog.getFontName(), fontDialog.getFontSize());
    } else
    if(action.equals("global settings")){
	    open(new FileDoc(GProperties.CONFIG_FILE));
    } else
    if(action.equals("update")){
		  checkForUpdates(false);
	  } else

	  if(action.equals("about")){
		  AboutDialog aboutDialog = new AboutDialog(version);
		  aboutDialog.showIt();
	  } else

    // timer
    if(action.equals("timer")){
      checkExternalModification(true);
    }
  }

	private void toggleTool(int tab) {
		if (toolsTab.isVisible()) {
			if (toolsTab.getSelectedIndex() == tab) {
				toolsTab.setVisible(false);
				getActiveEditor().requestFocus();
			} else {
				toolsTab.setSelectedIndex(tab);
			}
		} else {
			toolsTab.setSelectedIndex(tab);
			toolsTab.setVisible(true);
			textToolsSplit.setResizeWeight(1 - GProperties.getDouble("tools_panel.height"));
			textToolsSplit.resetToPreferredSizes();
			toolsTab.getSelectedComponent().requestFocus();
		}
	}

	private void svnList(String message, StringBuilder builder, ArrayList<SVN.UpdateResult> results, int[] types) {
    boolean first = true;
    for(SVN.UpdateResult result : results) {
      for(int type : types) {
        if(result.getType() == type) {
          if(first) {
            builder.append(message);
            builder.append("<ul>");
            first = false;
          }
          builder.append("<li>" + result.getFile().getName() + "</li>");
        }
      }
    }
    if(!first) builder.append("</ul>");
  }

  private synchronized void checkExternalModification(boolean showPopup) {
    ArrayList<String> reloaded = new ArrayList<String>();
    for(int tab = 0; tab < tabbedPane.getTabCount(); tab++) {
      SourceCodeEditor editor = getEditor(tab);

      File file;
      if (editor.getResource() instanceof FileDoc) {
        FileDoc fileDoc = (FileDoc) editor.getResource();
        file = fileDoc.getFile();
      } else {
        continue;
      }

      Long oldModified = lastModified.get(file);
      Long newModified = file.lastModified();
      // has the file been changed?
      if(!oldModified.equals(newModified)) {
        boolean reload = false;
        if(editor.getTextPane().getDocument().isModified()) {
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
                  new Object[] {"Overwrite", "Reload", "Don't reload"},
                  2
          );
          if(choice == 0) save(editor);
          if(choice == 1) reload = true;
        } else {
          reload = true;
        }
        if(reload) {
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
    if(reloaded.size() > 0 && showPopup) {
      StringBuilder builder = new StringBuilder();
      builder.append("<html>");
      builder.append("The following documents have been externally modified and reloaded:<br>");
      builder.append("<ul>");
      for(String name : reloaded) builder.append("<li>" + name + "</li>");
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
				if (updater.performUpdate(true)) {
					// restart the editor
					System.exit(255);
				}
			}
		} else {
			if (!startup) {
				JOptionPane.showMessageDialog(this, "JLatexEditor is up-to-date.", "JLatexEditor - Updater", JOptionPane.INFORMATION_MESSAGE);
			}
		}
	}

	public void windowOpened(WindowEvent e) {
    SwingUtilities.invokeLater(new Runnable() {
      public void run() {
        // recent files
        loadRecent();

        // reopen last files
        reopenLast();

        // open files given in command line
        for(String arg : args) { open(new FileDoc(new File(arg))); }
        openDialog.setDialogTitle("Open");
        if(args.length > 0) {
          openDialog.setCurrentDirectory(new File(new File(args[0]).getParent()));
        }

        setExtendedState(getExtendedState() | MAXIMIZED_BOTH);
      }
    });
  }

  private class ShutdownHook extends Thread {
    public void run() {
      try {
        PrintWriter writerLast = new PrintWriter(new FileWriter(FILE_LAST_SESSION));
        for(int tabNr = 0; tabNr < tabbedPane.getTabCount(); tabNr++) {
          SourceCodeEditor editor = getEditor(tabNr);
          if(!(editor.getResource() instanceof FileDoc)) continue;
          writerLast.println(editor.getFile().getCanonicalPath() + ":" + editor.getTextPane().getCaret().getRow());
        }
        writerLast.close();

        PrintWriter writerRecent = new PrintWriter(new FileWriter(FILE_RECENT));
        for(String name : recentFiles) writerRecent.println(name);
        writerRecent.close();
      } catch (IOException ignored) {}
    }
  }
  
  public void windowClosing(WindowEvent e) {
    System.exit(0);
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
    if(e.getSource() == tabbedPane) {
	    // document tab has been changed
	    editorChanged();
    }
  }

	private void editorChanged() {
		// reattach error highlighter
		errorHighlighting.detach();
		SourceCodeEditor editor = getEditor(tabbedPane.getSelectedIndex());
		errorHighlighting.attach(editor, errorView);
		errorHighlighting.update();

		// update window title
		AbstractResource resource = editor.getResource();
		String fileName = resource.toString();
		if (resource instanceof FileDoc) {
			FileDoc fileDoc = (FileDoc) resource;
			File file = fileDoc.getFile();
			String fileWithPath = file.getPath();
			fileName = file.getName();
			for (int i=0; i<GProperties.getInt("window.title.number_of_parent_dirs_shown"); i++) {
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

    SourceCodeEditor editor = getEditor(tabbedPane.getSelectedIndex());
    SCEPane pane = editor.getTextPane();
    pane.setFont(GProperties.getEditorFont());
    LatexStyles.addStyles(pane.getDocument());
    editor.repaint();
  }

	// KeyListener
	public void keyTyped(KeyEvent e) {
	}

	public void keyPressed(KeyEvent e) {
		if (e.getModifiers() == KeyEvent.ALT_MASK) {
			// alt+left
			if (e.getKeyCode() == KeyEvent.VK_LEFT) {
				// select the left tab
				int index = tabbedPane.getSelectedIndex() - 1;
				if (index < 0) index = tabbedPane.getTabCount() - 1;
				tabbedPane.setSelectedIndex(index);

				e.consume();
			}
			// alt+right
			if (e.getKeyCode() == KeyEvent.VK_RIGHT) {
				// select the right tab
				int index = tabbedPane.getSelectedIndex() + 1;
				if (index >= tabbedPane.getTabCount()) index = 0;
				tabbedPane.setSelectedIndex(index);

				e.consume();
			}
		}
	}

	public void keyReleased(KeyEvent e) {
	}

  public void changeFont(String fontName, int fontSize) {
    GProperties.setEditorFont(new Font(fontName, Font.PLAIN, fontSize));

    SourceCodeEditor editor = getActiveEditor();
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
			System.out.println(shortcut);
			if (shortcut == null) {
				shortcut = "";
			}
			menuItem.setAccelerator(KeyStroke.getKeyStroke(shortcut));
		}
	}
	
  private class TabLabel extends JPanel implements MouseListener, SCEModificationStateListener {
    private Doc doc;
    private JLabel label;
    private JLabel closeIcon;

    private TabLabel(Doc doc, SourceCodeEditor editor) {
      this.doc = doc;
      setOpaque(false);

      label = new JLabel(doc.getName());
      closeIcon = new JLabel(new ImageIcon("icons/tab_close_over.png"));
      closeIcon.setVerticalAlignment(SwingConstants.CENTER);

      BorderLayout layout = new BorderLayout(4, 1);
      setLayout(layout);
      setBackground(new Color(255,255,255, 255));
      add(label, BorderLayout.CENTER);
      add(closeIcon, BorderLayout.EAST);

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
      if(e.getClickCount() >= 2) {
        mainEditor = getEditor(getTab(doc));
        for(int tab = 0; tab < tabbedPane.getTabCount(); tab++) {
          tabbedPane.getTabComponentAt(tab).setForeground(Color.BLACK);
        }
        label.setForeground(Color.RED);

        backgroundParser.parse();
        statusBar.checkForUpdates();
      }
      if(closeIcon.contains(e.getX() - closeIcon.getX(), e.getY() - closeIcon.getY())) {
        closeTab(getTab(doc));
      }
    }

    public void setForeground(Color fg) {
      super.setForeground(fg);
      if(label != null) label.setForeground(fg);
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
		  label.setText(modified ? "*"+ doc.getName() : doc.getName());
	  }
  }

	/**
	 * Abstract document.
	 */
	public static interface Doc extends AbstractResource {}

	/**
	 * Document read from file.
	 */
	public static class FileDoc implements Doc {
		private File file;
		private String id;

		public FileDoc(File file) {
			this.file = file;
			try {
				this.id = file.getCanonicalPath();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}

		@Override
		public int hashCode() {
			return id.hashCode();
		}

		@Override
		public boolean equals(Object obj) {
			if (obj instanceof FileDoc) {
			  FileDoc that = (FileDoc) obj;
			  return this.id.equals(that.id);
			}
			return false;
		}

		public File getFile() {
			return file;
		}

		public String getContent() throws IOException {
			return StreamUtils.readFile(file.getAbsolutePath());
		}

		public String getName() { return file.getName(); }

		public URI getUri() {
			return file.toURI();
		}

		public String toString() { return file.toString(); }
	}

	/**
	 * Unsaved document.
	 */
	public static class UntitledDoc implements Doc {
		private static int untitledNr = 1;
		private String name;

		public UntitledDoc() {
			name = UNTITLED + " " + untitledNr++;
		}

		@Override
		public int hashCode() {
			return name.hashCode();
		}

		@Override
		public boolean equals(Object obj) {
			if (obj instanceof UntitledDoc) {
				UntitledDoc that = (UntitledDoc) obj;
				return this.name.equals(that.name);
			}
			return false;
		}

		public String getContent() { return ""; }
		public String getName() { return name; }

		public URI getUri() {
			try {
				return new URI("unsaved:" + name);
			} catch (URISyntaxException e) {
				e.printStackTrace();
				return null;
			}
		}

		public String toString() { return name; }
	}
}

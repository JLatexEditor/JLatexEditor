
/**
 * @author Jörg Endrullis
 * @author Stefan Endrullis
 */

package jlatexeditor;

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
import jlatexeditor.quickhelp.LatexQuickHelp;
import jlatexeditor.syntaxhighlighting.LatexStyles;
import jlatexeditor.syntaxhighlighting.LatexSyntaxHighlighting;
import sce.codehelper.CombinedCodeHelper;
import sce.component.*;
import sce.syntaxhighlighting.SyntaxHighlighting;
import util.StreamUtils;
import util.filechooser.SCEFileChooser;
import util.updater.ProgramUpdater;

import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.util.Calendar;
import java.util.HashMap;

public class JLatexEditorJFrame extends JFrame implements ActionListener, WindowListener, ChangeListener, MouseMotionListener, KeyListener {
  private static String UNTITLED = "Untitled";

	private static String version = "*Bleeding Edge*";
	private static boolean devVersion = true;
	static {
		try {
			version = StreamUtils.readFile("version.txt");
			devVersion = false;
		} catch (IOException ignored) {}
	}

  private JMenuBar menuBar = null;
  private JTabbedPane tabbedPane = null;
  private JSplitPane textToolsSplit = null;
  private JTabbedPane toolsTab = null;
  private ErrorView errorView = null;

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
  private Timer timer = new Timer(2000, this);
  private HashMap<File,Long> lastModified = new HashMap<File, Long>();

	private final ProgramUpdater updater = new ProgramUpdater("JLatexEditor update", "http://endrullis.de/JLatexEditor/update/");

  // background parser
  private BackgroundParser backgroundParser;

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
	  super("JLatexEditor " + version);
    this.args = args;
    addWindowListener(this);

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
    setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

    // create menu
    menuBar = new JMenuBar();
    setJMenuBar(menuBar);

    JMenu fileMenu = new JMenu("File");
	  fileMenu.setMnemonic('F');
    menuBar.add(fileMenu);

	  JMenuItem newMenuItem = new JMenuItem("New");
	  newMenuItem.setActionCommand("new");
		newMenuItem.setMnemonic('N');
	  newMenuItem.setAccelerator(KeyStroke.getKeyStroke("control N"));
	  newMenuItem.addActionListener(this);
	  fileMenu.add(newMenuItem);

    JMenuItem openMenuItem = new JMenuItem("Open");
    openMenuItem.setActionCommand("open");
	  openMenuItem.setMnemonic('O');
    openMenuItem.setAccelerator(KeyStroke.getKeyStroke("control O"));
    openMenuItem.addActionListener(this);
    fileMenu.add(openMenuItem);

    JMenuItem saveMenuItem = new JMenuItem("Save");
    saveMenuItem.setActionCommand("save");
	  saveMenuItem.setMnemonic('S');
    saveMenuItem.setAccelerator(KeyStroke.getKeyStroke("control S"));
    saveMenuItem.addActionListener(this);
    fileMenu.add(saveMenuItem);

    JMenuItem closeMenuItem = new JMenuItem("Close");
    closeMenuItem.setActionCommand("close");
	  closeMenuItem.setMnemonic('C');
    closeMenuItem.setAccelerator(KeyStroke.getKeyStroke("control W"));
    closeMenuItem.addActionListener(this);
    fileMenu.add(closeMenuItem);

    JMenuItem exitMenuItem = new JMenuItem("Exit");
    exitMenuItem.setActionCommand("exit");
	  exitMenuItem.setMnemonic('E');
    exitMenuItem.addActionListener(this);
    fileMenu.add(exitMenuItem);

    JMenu editMenu = new JMenu("Edit");
	  editMenu.setMnemonic('E');
    menuBar.add(editMenu);

    JMenuItem undoMenuItem = new JMenuItem("Undo");
    undoMenuItem.setActionCommand("undo");
	  undoMenuItem.setMnemonic('U');
    undoMenuItem.setAccelerator(KeyStroke.getKeyStroke("control Z"));
    undoMenuItem.addActionListener(this);
    editMenu.add(undoMenuItem);

    JMenuItem redoMenuItem = new JMenuItem("Redo");
    redoMenuItem.setActionCommand("redo");
	  redoMenuItem.setMnemonic('R');
    redoMenuItem.setAccelerator(KeyStroke.getKeyStroke("control shift Z"));
    redoMenuItem.addActionListener(this);
    editMenu.add(redoMenuItem);

    editMenu.addSeparator();

    JMenuItem findMenuItem = new JMenuItem("Find");
    findMenuItem.setActionCommand("find");
	  findMenuItem.setMnemonic('F');
    findMenuItem.setAccelerator(KeyStroke.getKeyStroke("control F"));
    findMenuItem.addActionListener(this);
    editMenu.add(findMenuItem);

    JMenuItem replaceMenuItem = new JMenuItem("Replace");
    replaceMenuItem.setActionCommand("replace");
	  replaceMenuItem.setMnemonic('R');
    replaceMenuItem.setAccelerator(KeyStroke.getKeyStroke("control R"));
    replaceMenuItem.addActionListener(this);
    editMenu.add(replaceMenuItem);

    JMenuItem findNextMenuItem = new JMenuItem("Find Next");
    findNextMenuItem.setActionCommand("find next");
	  findNextMenuItem.setMnemonic('N');
    findNextMenuItem.setAccelerator(KeyStroke.getKeyStroke("F3"));
    findNextMenuItem.addActionListener(this);
    editMenu.add(findNextMenuItem);

    JMenuItem findPreviousMenuItem = new JMenuItem("Find Previous");
    findPreviousMenuItem.setActionCommand("find previous");
	  findPreviousMenuItem.setMnemonic('P');
    findPreviousMenuItem.setAccelerator(KeyStroke.getKeyStroke("shift F3"));
    findPreviousMenuItem.addActionListener(this);
    editMenu.add(findPreviousMenuItem);

    JMenuItem cutMenuItem = new JMenuItem("Cut");
    cutMenuItem.setActionCommand("cut");
    cutMenuItem.setAccelerator(KeyStroke.getKeyStroke("control X"));
    cutMenuItem.addActionListener(this);
    editMenu.add(cutMenuItem);

    JMenuItem copyMenuItem = new JMenuItem("Copy");
    copyMenuItem.setActionCommand("copy");
    copyMenuItem.setAccelerator(KeyStroke.getKeyStroke("control C"));
    copyMenuItem.addActionListener(this);
    editMenu.add(copyMenuItem);

    JMenuItem pasteMenuItem = new JMenuItem("Paste");
    pasteMenuItem.setActionCommand("paste");
    pasteMenuItem.setAccelerator(KeyStroke.getKeyStroke("control V"));
    pasteMenuItem.addActionListener(this);
    editMenu.add(pasteMenuItem);

	  editMenu.addSeparator();

	  JMenuItem commentMenuItem = new JMenuItem("Comment");
	  commentMenuItem.setActionCommand("comment");
	  commentMenuItem.setMnemonic('o');
	  commentMenuItem.setAccelerator(KeyStroke.getKeyStroke("control D"));
	  commentMenuItem.addActionListener(this);
	  editMenu.add(commentMenuItem);

	  JMenuItem uncommentMenuItem = new JMenuItem("Uncomment");
	  uncommentMenuItem.setActionCommand("uncomment");
	  uncommentMenuItem.setMnemonic('u');
	  uncommentMenuItem.setAccelerator(KeyStroke.getKeyStroke("control shift D"));
	  uncommentMenuItem.addActionListener(this);
	  editMenu.add(uncommentMenuItem);

	  editMenu.addSeparator();

    JMenuItem diffMenuItem = new JMenuItem("Diff");
    diffMenuItem.setActionCommand("diff");
	  diffMenuItem.setMnemonic('D');
    diffMenuItem.setAccelerator(KeyStroke.getKeyStroke("alt D"));
    diffMenuItem.addActionListener(this);
    editMenu.add(diffMenuItem);

    JMenu buildMenu = new JMenu("Build");
	  buildMenu.setMnemonic('B');
    menuBar.add(buildMenu);

    JMenuItem pdfMenuItem = new JMenuItem("pdf");
    pdfMenuItem.setActionCommand("pdf");
    pdfMenuItem.setAccelerator(KeyStroke.getKeyStroke("alt 1"));
    pdfMenuItem.addActionListener(this);
    buildMenu.add(pdfMenuItem);

    JMenuItem dviMenuItem = new JMenuItem("dvi");
    dviMenuItem.setActionCommand("dvi");
    dviMenuItem.setAccelerator(KeyStroke.getKeyStroke("alt 2"));
    dviMenuItem.addActionListener(this);
    buildMenu.add(dviMenuItem);

    JMenuItem dvipsMenuItem = new JMenuItem("dvi + ps");
    dvipsMenuItem.setActionCommand("dvi + ps");
    dvipsMenuItem.setAccelerator(KeyStroke.getKeyStroke("alt 3"));
    dvipsMenuItem.addActionListener(this);
    buildMenu.add(dvipsMenuItem);

    JMenuItem dvipspdfMenuItem = new JMenuItem("dvi + ps + pdf");
    dvipspdfMenuItem.setActionCommand("dvi + ps + pdf");
    dvipspdfMenuItem.setAccelerator(KeyStroke.getKeyStroke("alt 4"));
    dvipspdfMenuItem.addActionListener(this);
    buildMenu.add(dvipspdfMenuItem);

    JMenu vcMenu = new JMenu("Version Control");
	  vcMenu.setMnemonic('V');
    menuBar.add(vcMenu);

    JMenuItem svnMenuItem = new JMenuItem("SVN up");
    svnMenuItem.setActionCommand("svn up");
	  svnMenuItem.setMnemonic('u');
    svnMenuItem.setAccelerator(KeyStroke.getKeyStroke("alt U"));
    svnMenuItem.addActionListener(this);
    vcMenu.add(svnMenuItem);

    JMenuItem svnCommitItem = new JMenuItem("SVN commit");
    svnCommitItem.setActionCommand("svn commit");
	  svnCommitItem.setMnemonic('c');
    svnCommitItem.setAccelerator(KeyStroke.getKeyStroke("alt C"));
    svnCommitItem.addActionListener(this);
    vcMenu.add(svnCommitItem);

    JMenu settingsMenu = new JMenu("Settings");
    settingsMenu.setMnemonic('S');
    menuBar.add(settingsMenu);

    JMenuItem fontMenuItem = new JMenuItem("Font");
	  fontMenuItem.setActionCommand("font");
    fontMenuItem.setMnemonic('F');
    fontMenuItem.addActionListener(this);
    settingsMenu.add(fontMenuItem);  

	  JMenuItem globalSettings = new JMenuItem("Global Settings");
		globalSettings.setActionCommand("global settings");
	  globalSettings.setMnemonic('G');
	  globalSettings.setAccelerator(KeyStroke.getKeyStroke("control alt S"));
	  globalSettings.addActionListener(this);
	  settingsMenu.add(globalSettings);

	  JMenu helpMenu = new JMenu("Help");
	  helpMenu.setMnemonic('H');
	  menuBar.add(helpMenu);

	  JMenuItem updateMenuItem = new JMenuItem("Check for update");
	  updateMenuItem.setActionCommand("update");
	  updateMenuItem.setMnemonic('u');
	  //updateMenuItem.setAccelerator(KeyStroke.getKeyStroke("alt u"));
	  updateMenuItem.addActionListener(this);
	  if (devVersion) updateMenuItem.setVisible(false);
	  helpMenu.add(updateMenuItem);

	  JMenuItem aboutCommitItem = new JMenuItem("About");
	  aboutCommitItem.setActionCommand("about");
	  aboutCommitItem.setMnemonic('A');
	  //aboutCommitItem.setAccelerator(KeyStroke.getKeyStroke(""));
	  aboutCommitItem.addActionListener(this);
	  helpMenu.add(aboutCommitItem);
	  
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

	  textToolsSplit = new JSplitPane(JSplitPane.VERTICAL_SPLIT, true, tabbedPane, toolsTab);
    textToolsSplit.setOneTouchExpandable(true);
    textToolsSplit.setResizeWeight(.85);

    statusBar = new StatusBar();

    cp.add(textToolsSplit, BorderLayout.CENTER);
    cp.add(statusBar, BorderLayout.SOUTH);
    cp.validate();

    errorHighlighting.attach(getEditor(0), errorView);

    // file changed timer
    timer.setActionCommand("timer");
    timer.start();

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
  }

	private void initFileChooser() {
		openDialog.addChoosableFileFilter(new FileNameExtensionFilter(
			"LaTeX files (*.tex, *.def, *.bib)", "tex", "def", "bib"));
	}

	private SourceCodeEditor createLatexSourceCodeEditor() {
    SourceCodeEditor editor = new SourceCodeEditor<AbstractResource>(null);

    SCEPane scePane = editor.getTextPane();
    SCEDocument document = scePane.getDocument();

    // add some styles to the document
    LatexStyles.addStyles(document);

    // syntax highlighting
    SyntaxHighlighting syntaxHighlighting = new LatexSyntaxHighlighting(scePane);
    syntaxHighlighting.start();

	  // code completion and quick help
		CombinedCodeHelper codeHelper = new CombinedCodeHelper();
		codeHelper.addPatternHelper(new BibCodeHelper(getBackgroundParser()));
		codeHelper.addPatternHelper(new IncludeCodeHelper());
		codeHelper.addPatternHelper(new LatexCommandCodeHelper("data/codehelper/commands.xml"));
	  scePane.setCodeHelper(codeHelper);
	  scePane.setTabCompletion(new LatexCommandCodeHelper("data/codehelper/tabCompletion.xml"));
	  scePane.setQuickHelp(new LatexQuickHelp("data/quickhelp/"));

    try {
      scePane.addCodeAssistantListener(new SpellCheckSuggester());
    } catch (Exception e) { }

	  new JumpTo(editor, this);

	  scePane.addKeyListener(this);

	  return editor;
  }

	private SourceCodeEditor createGPropertiesSourceCodeEditor() {
    SourceCodeEditor editor = new SourceCodeEditor<AbstractResource>(null);

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

  public int getTab(AbstractResource resource) {
		for(int tab = 0; tab < tabbedPane.getTabCount(); tab++) {
			SourceCodeEditor editor = (SourceCodeEditor) tabbedPane.getComponentAt(tab);
			if(resource.equals(editor.getResource())) return tab;
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

	private SourceCodeEditor addTab(AbstractResource resource) throws IOException {
		SourceCodeEditor editor;
		if (resource.getName().endsWith("global.properties")) {
			editor = createGPropertiesSourceCodeEditor();
		} else {
			editor = createLatexSourceCodeEditor();
		}
		tabbedPane.removeChangeListener(this);
		tabbedPane.addTab(resource.getName(), editor);
		tabbedPane.addChangeListener(this);
		tabbedPane.setTabComponentAt(tabbedPane.getTabCount()-1, new TabLabel(resource, editor));
		tabbedPane.setSelectedIndex(tabbedPane.getTabCount()-1);
	  editor.open(resource);

		return editor;
	}

  public SourceCodeEditor open(AbstractResource resource) {
    try{
      // already open?
      int tab = getTab(resource);
      if(tab != -1) { tabbedPane.setSelectedIndex(tab); return getEditor(tab); }

      // replacing the untitled tab?
	    boolean closeFirstTab = false;
      if(tabbedPane.getTabCount() == 1) {
	      SourceCodeEditor firstEditor = getEditor(0);
	      if (firstEditor.getResource() instanceof UntitledDoc && !firstEditor.getTextPane().getDocument().isModified()) {
		      closeFirstTab = true;
	      }
      }

	    SourceCodeEditor editor = addTab(resource);
	    if (closeFirstTab) closeTab(0);


	    if (resource instanceof FileDoc) {
	      FileDoc fileDoc = (FileDoc) resource;
		    lastModified.put(fileDoc.file, fileDoc.file.lastModified());
	    }

      errorHighlighting.detach();
      errorHighlighting.attach(editor, errorView);
      errorHighlighting.update();
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
  public boolean saveAll() {
    boolean all = true;
    for(int tab = 0; tab < tabbedPane.getTabCount(); tab++) {
      SourceCodeEditor editor = (SourceCodeEditor) tabbedPane.getComponentAt(tab);
      AbstractResource resource = editor.getResource();
      boolean save = (!(resource instanceof UntitledDoc)) || tab == tabbedPane.getSelectedIndex();
      if(save) { if (!save(editor)) all = false; }
    }
		return all;
  }

	/**
	 * Saves the document given by the editor.
	 *
	 * @param editor editor containing the document to save
	 * @return true if saving the document has NOT been canceled
	 */
  private boolean save(SourceCodeEditor editor) {
		if (!editor.getTextPane().getDocument().isModified()) return true;

		AbstractResource resource = editor.getResource();

		boolean gPropertiesSaved = false;

		File file = null;
		if (resource instanceof UntitledDoc) {
      openDialog.setDialogTitle("Save " + resource.getName());
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

      TabLabel tabLabel = (TabLabel) tabbedPane.getTabComponentAt(getTab(resource));
      resource = new FileDoc(file);
      tabLabel.setResource(resource);
      editor.setResource(resource);
		} else
		if (resource instanceof FileDoc) {
		  FileDoc fileDoc = (FileDoc) resource;
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

        diff_writer.println(LocalHistory.REVISION + Calendar.getInstance().getTime());
        diff_writer.close();

        PrintWriter history_writer = new PrintWriter(new FileOutputStream(file_backup));
        history_writer.write(text);
        history_writer.close();
      }

      lastModified.put(file, file.lastModified());
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
    latexCompiler = new LatexCompiler(type, editor, errorView);

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

/*
    // font
    if(action.startsWith("Font: ")) {
      String fontName = action.substring(action.indexOf(": ") + 2);
      GProperties.setEditorFont(new Font(fontName, Font.PLAIN, GProperties.getEditorFont().getSize()));
      getEditor(tabbedPane.getSelectedIndex()).repaint();
    }
    // text antialiasing
    if(action.startsWith("TextAntialias: ")) {
      String key = action.substring(action.indexOf(": ") + 2);
      GProperties.setTextAntiAliasing(GProperties.TEXT_ANTIALIAS_MAP.get(key));
      getEditor(tabbedPane.getSelectedIndex()).repaint();
    }
*/
	  if(action.equals("font")){
		  SCEFontWindow fontDialog = new SCEFontWindow(GProperties.getEditorFont().getFontName(), GProperties.getEditorFont().getSize(), this);
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

			//JOptionPane.showMessageDialog(this, "<html><h2>JLatexEditor</h2><p>TODO</p></html>", "JLatexEditor", JOptionPane.INFORMATION_MESSAGE);
	  } else

    // timer
    if(action.equals("timer")){
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
            try {
              editor.reload();
              lastModified.put(file, newModified);
            } catch (IOException e1) {
              e1.printStackTrace();
            }
          }
        }
      }
    }
  }



  private void checkForUpdates(boolean startup) {
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

  public void windowClosing(WindowEvent e) {
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
      errorHighlighting.detach();
      errorHighlighting.attach(getEditor(tabbedPane.getSelectedIndex()), errorView);
    }
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
		// alt left/right
		if (e.getModifiers() == KeyEvent.ALT_MASK) {
			if (e.getKeyCode() == KeyEvent.VK_LEFT) {
				// select the left tab
				int index = tabbedPane.getSelectedIndex() - 1;
				if (index < 0) index = tabbedPane.getTabCount() - 1;
				tabbedPane.setSelectedIndex(index);

				e.consume();
			}
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

    SourceCodeEditor editor = getEditor(tabbedPane.getSelectedIndex());
    SCEPane pane = editor.getTextPane();
    pane.setFont(GProperties.getEditorFont());
    LatexStyles.load();
    LatexStyles.addStyles(pane.getDocument());
    editor.repaint();
  }

  private class TabLabel extends JPanel implements MouseListener, SCEModificationStateListener {
    private AbstractResource resource;
    private JLabel label;
    private JLabel closeIcon;

    private TabLabel(AbstractResource resource, SourceCodeEditor editor) {
      this.resource = resource;
      setOpaque(false);

      label = new JLabel(resource.getName());
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

    public AbstractResource getResource() {
      return resource;
    }

    public void setResource(AbstractResource resource) {
      this.resource = resource;
      label.setText(resource.getName());
    }

    public boolean contains(int x, int y) {
      return x >= -4 && x <= getWidth() + 4 && y >= -2 && y <= getHeight() + 2;
    }

    public void mouseClicked(MouseEvent e) {
      tabbedPane.setSelectedIndex(getTab(resource));
      if(e.getClickCount() >= 2) {
        mainEditor = getEditor(getTab(resource));
        for(int tab = 0; tab < tabbedPane.getTabCount(); tab++) {
          tabbedPane.getTabComponentAt(tab).setForeground(Color.BLACK);
        }
        label.setForeground(Color.RED);
      }
      if(closeIcon.contains(e.getX() - closeIcon.getX(), e.getY() - closeIcon.getY())) {
        closeTab(getTab(resource));
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
		  label.setText(modified ? "*"+resource.getName() : resource.getName());
	  }
  }

	/**
	 * Document read from file.
	 */
	public static class FileDoc implements AbstractResource {
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
		public String toString() { return file.toString(); }
	}

	/**
	 * Unsaved document.
	 */
	public static class UntitledDoc implements AbstractResource {
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
		public String toString() { return name; }
	}
}

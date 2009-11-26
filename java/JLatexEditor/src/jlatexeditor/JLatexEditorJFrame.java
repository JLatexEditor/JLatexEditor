
/**
 * @author Jörg Endrullis
 * @author Stefan Endrullis
 */

package jlatexeditor;

import jlatexeditor.errorhighlighting.LatexCompiler;
import sce.component.SCEDocument;
import sce.component.SCEPane;
import sce.component.SourceCodeEditor;
import jlatexeditor.errorhighlighting.LatexErrorHighlighting;
import jlatexeditor.quickhelp.LatexQuickHelp;
import jlatexeditor.codehelper.LatexCodeHelper;
import jlatexeditor.syntaxhighlighting.LatexStyles;
import jlatexeditor.syntaxhighlighting.LatexSyntaxHighlighting;
import sce.syntaxhighlighting.SyntaxHighlighting;

import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.util.HashMap;

public class JLatexEditorJFrame extends JFrame implements ActionListener, WindowListener, ChangeListener {
  private static String UNTITLED = "Untitled";

  private JMenuBar menuBar = null;
  private JTabbedPane tabbedPane = null;
  private JSplitPane textErrorSplit = null;
  private ErrorView errorView = null;

  // command line arguments
  private String args[];

  // last directory of the opening dialog
  private JFileChooser openDialog = new JFileChooser();

  // compile thread
  private LatexCompiler latexCompiler = null;
  // main file to compile
  private SourceCodeEditor mainEditor = null;

  private LatexErrorHighlighting errorHighlighting = new LatexErrorHighlighting();

  // file changed time
  private Timer timer = new Timer(2000, this);
  private HashMap<File,Long> lastModified = new HashMap<File, Long>();

  public static void main(String args[]){
    JLatexEditorJFrame latexEditor = new JLatexEditorJFrame("jlatexeditor.JLatexEditor", args);
    latexEditor.setVisible(true);
  }

  public JLatexEditorJFrame(String name, String args[]){
    super(name);
    this.args = args;
    addWindowListener(this);
    
    // set Layout
    getContentPane().setLayout(new BorderLayout());
    setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

    // create menu
    menuBar = new JMenuBar();
    setJMenuBar(menuBar);

    JMenu fileMenu = new JMenu("File");
    menuBar.add(fileMenu);

    JMenuItem openMenuItem = new JMenuItem("Open");
    openMenuItem.setActionCommand("open");
    openMenuItem.setAccelerator(KeyStroke.getKeyStroke("control O"));
    openMenuItem.addActionListener(this);
    fileMenu.add(openMenuItem);

    JMenuItem saveMenuItem = new JMenuItem("Save");
    saveMenuItem.setActionCommand("save");
    saveMenuItem.setAccelerator(KeyStroke.getKeyStroke("control S"));
    saveMenuItem.addActionListener(this);
    fileMenu.add(saveMenuItem);

    JMenuItem closeMenuItem = new JMenuItem("Close");
    closeMenuItem.setActionCommand("close");
    closeMenuItem.setAccelerator(KeyStroke.getKeyStroke("control W"));
    closeMenuItem.addActionListener(this);
    fileMenu.add(closeMenuItem);

    JMenuItem exitMenuItem = new JMenuItem("Exit");
    exitMenuItem.setActionCommand("exit");
    exitMenuItem.addActionListener(this);
    fileMenu.add(exitMenuItem);

    JMenu editMenu = new JMenu("Edit");
    menuBar.add(editMenu);

    JMenuItem findMenuItem = new JMenuItem("Find");
    findMenuItem.setActionCommand("find");
    findMenuItem.setAccelerator(KeyStroke.getKeyStroke("control F"));
    findMenuItem.addActionListener(this);
    fileMenu.add(findMenuItem);

    JMenu buildMenu = new JMenu("Build");
    menuBar.add(buildMenu);

    JMenuItem compileMenuItem = new JMenuItem("Compile");
    compileMenuItem.setActionCommand("compile");
    compileMenuItem.setAccelerator(KeyStroke.getKeyStroke("alt 1"));
    compileMenuItem.addActionListener(this);
    buildMenu.add(compileMenuItem);

    // error messages
    errorView = new ErrorView(this);

    // tabs for the files
    tabbedPane = new JTabbedPane();
    tabbedPane.addTab(UNTITLED, createSourceCodeEditor());
    tabbedPane.addChangeListener(this);

    textErrorSplit = new JSplitPane(JSplitPane.VERTICAL_SPLIT, true, tabbedPane, errorView);
    textErrorSplit.setOneTouchExpandable(true);
    textErrorSplit.setResizeWeight(.85);

    getContentPane().add(textErrorSplit, BorderLayout.CENTER);
    getContentPane().validate();

    errorHighlighting.attach(getEditor(0), errorView);

    // file changed timer
    timer.setActionCommand("timer");
    timer.start();
  }

  private SourceCodeEditor createSourceCodeEditor() {
    SourceCodeEditor editor = new SourceCodeEditor(null);

    SCEPane scePane = editor.getTextPane();
    SCEDocument document = scePane.getDocument();

    // add some styles to the document
    LatexStyles.addStyles(document);

    // syntax highlighting
    SyntaxHighlighting syntaxHighlighting = new LatexSyntaxHighlighting(scePane);
    syntaxHighlighting.start();

	  // code completion and quick help
	  scePane.setCodeHelper(new LatexCodeHelper("data/codehelper/commands.xml"));
	  scePane.setQuickHelp(new LatexQuickHelp("data/quickhelp/"));

    return editor;
  }

  public int getTab(File file) {
    try {
      String fileCanonical = file.getCanonicalPath();
      for(int tab = 0; tab < tabbedPane.getTabCount(); tab++) {
        SourceCodeEditor editor = (SourceCodeEditor) tabbedPane.getComponentAt(tab);
        String editorCanonical = editor.getFile() != null ? editor.getFile().getCanonicalPath() : "";
        if(fileCanonical.equals(editorCanonical)) return tab;
      }
    } catch (IOException e) {
      e.printStackTrace();
    }
    return -1;
  }

  public SourceCodeEditor getEditor(int tab) {
    return (SourceCodeEditor) tabbedPane.getComponentAt(tab);
  }

  public SourceCodeEditor open(File file) {
    try{
      // already open?
      int tab = getTab(file);
      if(tab != -1) { tabbedPane.setSelectedIndex(tab); return getEditor(tab); }

      boolean addTab = true;

      // replacing the untitled tab?
      SourceCodeEditor editor = null;
      if(tabbedPane.getTabCount() == 1
              && tabbedPane.getTitleAt(0).equals(UNTITLED)
              && ((SourceCodeEditor) tabbedPane.getComponentAt(0)).getText().trim().equals("")) {
        tabbedPane.setTitleAt(0, file.getName());
        tabbedPane.setTabComponentAt(0, new TabLabel(file));
        editor = ((SourceCodeEditor) tabbedPane.getComponentAt(0));
      } else {
        editor = createSourceCodeEditor();
        tabbedPane.removeChangeListener(this);
        tabbedPane.addTab(file.getName(), editor);
        tabbedPane.addChangeListener(this);
        tabbedPane.setTabComponentAt(tabbedPane.getTabCount()-1,new TabLabel(file));
        tabbedPane.setSelectedIndex(tabbedPane.getTabCount()-1);
      }

      editor.open(file);
      lastModified.put(file, file.lastModified());

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

  public void saveAll() {
    for(int tab = 0; tab < tabbedPane.getTabCount(); tab++) {
      SourceCodeEditor editor = (SourceCodeEditor) tabbedPane.getComponentAt(tab);

      File file = editor.getFile();
      File backup = new File(file.getAbsolutePath() + "~");
      if(backup.exists()) backup.delete();
      file.renameTo(backup);

      String text = editor.getTextPane().getText();
      try{
        PrintWriter writer = new PrintWriter(new FileOutputStream(file));
        writer.write(text);
        writer.close();
      } catch(IOException ex){
        ex.printStackTrace();
      }
    }
  }

  public void compile() {
    SourceCodeEditor editor = mainEditor;
    if(editor == null) {
      editor = (SourceCodeEditor) tabbedPane.getSelectedComponent();
    }

    if(latexCompiler != null) latexCompiler.halt();
    latexCompiler = new LatexCompiler(editor, errorView);

    errorHighlighting.clear();
    latexCompiler.addLatexCompileListener(errorHighlighting);

    latexCompiler.start();
  }

  private void closeTab(int tab) {
    SourceCodeEditor editor = getEditor(tab);
    tabbedPane.removeTabAt(tab);
    editor.getTextPane().setText("");
  }

  // ActionListener methods
  public void actionPerformed(ActionEvent e){
    // open a file
    if(e.getActionCommand().equals("open")){
      //openDialog.pack();
      openDialog.showDialog(this, "Open");
      if(openDialog.getSelectedFile() == null) return;

      open(openDialog.getSelectedFile());
    }

    // save a file
    if(e.getActionCommand().equals("save")){
      saveAll();
    }

    // close
    if(e.getActionCommand().equals("close")){
      closeTab(tabbedPane.getSelectedIndex());
    }

    // exit
    if(e.getActionCommand().equals("exit")){
      saveAll();
      System.exit(-1);
    }

    // find
    if(e.getActionCommand().equals("find")){
      getEditor(tabbedPane.getSelectedIndex()).search();
    }

    // compile
    if(e.getActionCommand().equals("compile")){
      saveAll();
      compile();
    }

    // timer
    if(e.getActionCommand().equals("timer")){
      for(int tab = 0; tab < tabbedPane.getTabCount(); tab++) {
        SourceCodeEditor editor = getEditor(tab);
        File file = editor.getFile();
        if(file == null) continue;

        Long oldModified = lastModified.get(file);
        Long newModified = file.lastModified();
        // has the file been changed?
        if(!oldModified.equals(newModified)) {
          if(editor.getTextPane().getDocument().isModified()) {
            // TODO: show warning or diff
          } else {
            // reload
            try {
              editor.reload();
            } catch (IOException e1) {
              e1.printStackTrace();
            }
          }
        }
      }
    }
  }

  public void windowOpened(WindowEvent e) {
    SwingUtilities.invokeLater(new Runnable() {
      public void run() {
        // open files given in command line
        for(String arg : args) { open(new File(arg)); }
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

  private class TabLabel extends JPanel implements MouseListener {
    private File file;
    private JLabel label;
    private JLabel closeIcon;

    private TabLabel(File file) {
      this.file = file;
      setOpaque(false);

      label = new JLabel(file.getName());
      closeIcon = new JLabel(new ImageIcon("icons/tab_close_over.png"));
      closeIcon.setVerticalAlignment(SwingConstants.CENTER);

      BorderLayout layout = new BorderLayout(4, 1);
      setLayout(layout);
      setBackground(new Color(255,255,255, 255));
      add(label, BorderLayout.CENTER);
      add(closeIcon, BorderLayout.EAST);

      addMouseListener(this);
    }

    public boolean contains(int x, int y) {
      return x >= -4 && x <= getWidth() + 4 && y >= -2 && y <= getHeight() + 2;
    }

    public void mouseClicked(MouseEvent e) {
      tabbedPane.setSelectedIndex(getTab(file));
      if(e.getClickCount() >= 2) {
        mainEditor = getEditor(getTab(file));
        for(int tab = 0; tab < tabbedPane.getTabCount(); tab++) {
          tabbedPane.getTabComponentAt(tab).setForeground(Color.BLACK);
        }
        label.setForeground(Color.RED);
      }
      if(closeIcon.contains(e.getX() - closeIcon.getX(), e.getY() - closeIcon.getY())) {
        closeTab(getTab(file));
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
  }
}

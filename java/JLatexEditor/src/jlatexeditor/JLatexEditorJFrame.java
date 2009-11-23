
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
import util.StreamUtils;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.io.*;

public class JLatexEditorJFrame extends JFrame implements ActionListener, WindowListener {
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

    JMenu fileMenu = new JMenu("Datei");
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

    JMenuItem compileMenuItem = new JMenuItem("Compile");
    saveMenuItem.setActionCommand("compile");
    saveMenuItem.setAccelerator(KeyStroke.getKeyStroke("alt 1"));
    saveMenuItem.addActionListener(this);
    fileMenu.add(compileMenuItem);

    // error messages
    errorView = new ErrorView();

    // tabs for the files
    tabbedPane = new JTabbedPane();
    tabbedPane.addTab(UNTITLED, createSourceCodeEditor());

    textErrorSplit = new JSplitPane(JSplitPane.VERTICAL_SPLIT, true, tabbedPane, errorView);
    textErrorSplit.setOneTouchExpandable(true);
    textErrorSplit.setResizeWeight(.90);

    getContentPane().add(textErrorSplit, BorderLayout.CENTER);
    getContentPane().validate();
  }

  private SourceCodeEditor createSourceCodeEditor() {
    SourceCodeEditor editor = new SourceCodeEditor(UNTITLED);

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

    // error highlighting
    editor.setErrorHighlighting(new LatexErrorHighlighting(scePane, errorView));

    return editor;
  }

  private String readFile(String fileName) throws IOException {
    FileInputStream fileInputStream = new FileInputStream(fileName);
    byte data[] = StreamUtils.readBytesFromInputStream(fileInputStream);
    fileInputStream.close();

    // Set the text contend
    String text = new String(data);
    text = text.replaceAll("\n\r", "\n");
    text = text.replaceAll("\t", "  ");

    return text;
  }

  private int getTab(File file) {
    for(int tab = 0; tab < tabbedPane.getTabCount(); tab++) {
      SourceCodeEditor editor = (SourceCodeEditor) tabbedPane.getComponentAt(tab);
      if(new File(editor.getFileName()).equals(file)) {
        return tab;
      }
    }
    return -1;
  }

  private SourceCodeEditor getEditor(int tab) {
    return (SourceCodeEditor) tabbedPane.getComponentAt(tab);
  }

  public void open(File file) {
    try{
      String text = readFile(file.getAbsolutePath());

      // already open?
      int tab = getTab(file);
      if(tab != -1) { tabbedPane.setSelectedIndex(tab); return; }

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
        tabbedPane.addTab(file.getName(), editor);
        tabbedPane.setTabComponentAt(tabbedPane.getTabCount()-1,new TabLabel(file));
        tabbedPane.setSelectedIndex(tabbedPane.getTabCount()-1);
      }

      editor.setFileName(file.getAbsolutePath());
      editor.getTextPane().setText(text);
    } catch(IOException exc){
      System.out.println("Error opening file");
      exc.printStackTrace();
    }
  }

  public void saveAll() {
    for(int tab = 0; tab < tabbedPane.getTabCount(); tab++) {
      SourceCodeEditor editor = (SourceCodeEditor) tabbedPane.getComponentAt(tab);

      String fileName = editor.getFileName();
      File file = new File(fileName);
      File backup = new File(fileName + "~");
      if(backup.exists()) backup.delete();
      file.renameTo(backup);

      String text = editor.getTextPane().getText();
      try{
        PrintWriter writer = new PrintWriter(new FileOutputStream(fileName));
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

    latexCompiler.addLatexCompileListener(editor.getErrorHighlighting());

    latexCompiler.start();
    latexCompiler = null;
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

    // compile
    if(e.getActionCommand().equals("compile")){
      saveAll();
      compile();
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

    @Override
    public boolean contains(int x, int y) {
      return x >= -4 && x <= getWidth() + 4 && y >= -2 && y <= getHeight() + 2;
    }

    public void mouseClicked(MouseEvent e) {
      tabbedPane.setSelectedIndex(getTab(file));
      if(e.getClickCount() >= 2) {

      }
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

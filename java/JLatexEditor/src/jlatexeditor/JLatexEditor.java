
/**
 * @author Jörg Endrullis
 */

package jlatexeditor;

import jlatexeditor.errorhighlighting.LatexCompiler;
import jlatexeditor.errorhighlighting.LatexErrorHighlighting;
import sce.component.SCEDocument;
import sce.component.SCEPane;
import sce.component.SourceCodeEditor;
import jlatexeditor.codehelper.LatexCodeHelper;
import jlatexeditor.quickhelp.LatexQuickHelp;
import jlatexeditor.syntaxhighlighting.LatexStyles;
import jlatexeditor.syntaxhighlighting.LatexSyntaxHighlighting;
import sce.syntaxhighlighting.SyntaxHighlighting;
import util.StreamUtils;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.*;
import java.util.Properties;

import qaqua.QaquaLookAndFeel;
import qaqua.QaquaFrame;

public class JLatexEditor extends QaquaFrame implements ActionListener{
  private static String UNTITLED = "Untitled";

  Properties properties = new Properties();

  // the MenuBar
  private JMenuBar menuBar = null;

  // the Latex TextPane
  private SourceCodeEditor editor = null;
  // error messages
  private ErrorView errorView = null;
  private JSplitPane textErrorSplit = null;

  // the underlying document
  private SCEDocument document = null;

  // syntax highlighter
  private SyntaxHighlighting syntaxHighlightning = null;

  // error highlighter
  private LatexErrorHighlighting errorHighlightning = null;

  // compile thread
  private LatexCompiler latexCompiler = null;

  // current file
  private String fileName = null;

  public static void main(String args[]){
    // set LookAndFeel
    try{
      UIManager.setLookAndFeel(new QaquaLookAndFeel());
    } catch(UnsupportedLookAndFeelException e){
      e.printStackTrace();
    }

    JLatexEditor latexEditor = new JLatexEditor(new JFrame("jlatexeditor.JLatexEditor"));
    latexEditor.setSize(1024, 450);
    latexEditor.setVisible(true);
  }

  public JLatexEditor(JFrame owner){
    super(owner);

    // set Layout
    getContentPane().setLayout(new BorderLayout());
    setDefaultCloseOperation(QaquaFrame.EXIT_ON_CLOSE);

    // create menu
    menuBar = new JMenuBar();
    setJMenuBar(menuBar);

    JMenu fileMenu = new JMenu("Datei");
    menuBar.add(fileMenu);

    JMenuItem openMenuItem = new JMenuItem("Öffnen");
    openMenuItem.setActionCommand("open");
    openMenuItem.setAccelerator(KeyStroke.getKeyStroke("control O"));
    openMenuItem.addActionListener(this);
    fileMenu.add(openMenuItem);

    JMenuItem saveMenuItem = new JMenuItem("Speichern");
    saveMenuItem.setActionCommand("save");
    saveMenuItem.setAccelerator(KeyStroke.getKeyStroke("control S"));
    saveMenuItem.addActionListener(this);
    fileMenu.add(saveMenuItem);

    // text pane for editing
    editor = new SourceCodeEditor(null);
    SCEPane scePane = editor.getTextPane();
    document = scePane.getDocument();

    // add some styles to the document
    LatexStyles.addStyles(document);

    // TextArea for error messages
    errorView = new ErrorView(new JLatexEditorJFrame("", new String[]{}));
    errorView.setFont(new Font("MonoSpaced", 0, 13));

    textErrorSplit = new JSplitPane(JSplitPane.VERTICAL_SPLIT, true, editor, new JScrollPane(errorView));
    textErrorSplit.setOneTouchExpandable(true);
    textErrorSplit.setResizeWeight(0.5);

    // add the ScrollPane
    getContentPane().add(textErrorSplit, BorderLayout.CENTER);
    getContentPane().validate();

    // syntax highlighting
    syntaxHighlightning = new LatexSyntaxHighlighting(scePane);
    syntaxHighlightning.start();

	  // code completion and quick help
	  scePane.setCodeHelper(new LatexCodeHelper("data/codehelper/commands.xml"));
	  scePane.setQuickHelp(new LatexQuickHelp("data/quickhelp/"));
	  
    // error highlighting
    errorHighlightning = new LatexErrorHighlighting();
  }

  // ActionListener methods
  public void actionPerformed(ActionEvent e){
    // open a file
    if(e.getActionCommand().equals("open")){
      FileDialog dialog = new FileDialog(getJFrameOwner(), "Tex-Dokument öffnen", FileDialog.LOAD);
      dialog.pack();
      dialog.show();
      if(dialog.getFile() == null) return;

      // Remember the file name
      fileName = dialog.getDirectory() + dialog.getFile();

      // Try to open the file
      try{
        // Read the file
        FileInputStream fileInputStream = new FileInputStream(fileName);
        byte data[] = StreamUtils.readBytesFromInputStream(fileInputStream);
        fileInputStream.close();

        // Set the text contend
        String text = new String(data);
        text = text.replaceAll("\n\r", "\n");
        text = text.replaceAll("\t", "  ");
        document.setText(text);
      } catch(IOException exc){
        System.out.println("Error opening file");
        exc.printStackTrace();
      }
    }
    if(e.getActionCommand().equals("save")){
      // Write the actual content to a file
      String text = document.getText();
      try{
        PrintWriter writer = new PrintWriter(new FileOutputStream(fileName));
        writer.write(text);
        writer.close();
      } catch(IOException ex){
        ex.printStackTrace();
      }

      // Compile thread
      latexCompiler = new LatexCompiler(0, editor, errorView);
      latexCompiler.addLatexCompileListener(errorHighlightning);

      latexCompiler.run();
    }
  }
}


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
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.*;

public class JLatexEditorJFrame extends JFrame implements ActionListener {
  private static String UNTITLED = "Untitled";

  private JMenuBar menuBar = null;
  private JTabbedPane tabbedPane = null;
  private JTextArea errorMessages = null;

  // last directory of the opening dialog
  private FileDialog openDialog = new FileDialog(this, "Open", FileDialog.LOAD);

  // compile thread
  private LatexCompiler latexCompiler = null;

  public static void main(String args[]){
    JLatexEditorJFrame latexEditor = new JLatexEditorJFrame("jlatexeditor.JLatexEditor");
    latexEditor.setSize(1024, 450);
    latexEditor.setVisible(true);
  }

  public JLatexEditorJFrame(String name){
    super(name);

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

    // error messages
    errorMessages = new JTextArea();
    errorMessages.setFont(new Font("MonoSpaced", 0, 13));

    // tabs for the files
    tabbedPane = new JTabbedPane();
    tabbedPane.addTab(UNTITLED, createSourceCodeEditor());

    JSplitPane textErrorSplit = new JSplitPane(JSplitPane.VERTICAL_SPLIT, true, tabbedPane, new JScrollPane(errorMessages));
    textErrorSplit.setOneTouchExpandable(true);
    textErrorSplit.setResizeWeight(0.5);

    getContentPane().add(textErrorSplit, BorderLayout.CENTER);
    getContentPane().validate();
  }

  private SourceCodeEditor createSourceCodeEditor() {
    SourceCodeEditor editor = new SourceCodeEditor();

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
    new LatexErrorHighlighting(scePane, errorMessages);

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

  // ActionListener methods
  public void actionPerformed(ActionEvent e){
    // open a file
    if(e.getActionCommand().equals("open")){
      //openDialog.pack();
      openDialog.setVisible(true);
      if(openDialog.getFile() == null) return;

      String fileName = openDialog.getDirectory() + openDialog.getFile();

      try{
        String text = readFile(fileName);

        // replacing the untitled tab?
        SourceCodeEditor editor = null;
        if(tabbedPane.getTabCount() == 1
                && tabbedPane.getTitleAt(0).equals(UNTITLED)
                && ((SourceCodeEditor) tabbedPane.getComponentAt(0)).getText().trim().equals("")) {
          tabbedPane.setTitleAt(0, openDialog.getFile());
          editor = ((SourceCodeEditor) tabbedPane.getComponentAt(0));
        } else {
          editor = createSourceCodeEditor();
          tabbedPane.addTab(openDialog.getFile(), editor);
        }
        tabbedPane.set

        editor.getTextPane().getDocument().setText(text);
      } catch(IOException exc){
        System.out.println("Error opening file");
        exc.printStackTrace();
      }
    }

    // save a file
    if(e.getActionCommand().equals("save")){
      /*
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
      if(latexCompiler != null) return;
      latexCompiler = new LatexCompiler(editor.getTextPane(), fileName);
      latexCompiler.addLatexCompileListener(errorHighlighting);

      latexCompiler.run();
      latexCompiler = null;
      */
    }
  }
}

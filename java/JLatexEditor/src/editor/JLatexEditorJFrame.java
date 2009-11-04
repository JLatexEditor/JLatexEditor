
/**
 * @author Jörg Endrullis
 * @author Stefan Endrullis
 */

package editor;

import editor.component.SCEDocument;
import editor.component.SCEPane;
import editor.component.SourceCodeEditor;
import editor.errorhighlighting.LatexCompiler;
import editor.errorhighlighting.LatexErrorHighlighting;
import editor.latex.LatexCodeHelper;
import editor.quickhelp.LatexQuickHelp;
import editor.syntaxhighlighting.LatexStyles;
import editor.syntaxhighlighting.LatexSyntaxHighlighting;
import editor.syntaxhighlighting.SyntaxHighlighting;
import qaqua.QaquaFrame;
import util.StreamUtils;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Properties;

public class JLatexEditorJFrame extends JFrame implements ActionListener {
  Properties properties = new Properties();

  // the MenuBar
  private JMenuBar menuBar = null;

  // the Latex TextPane
  private SourceCodeEditor editor = null;
  // error messages
  private JTextArea errorMessages = null;
  private JSplitPane textErrorSplit = null;

  // the underlying document
  private SCEDocument document = null;

  // syntax highlighter
  private SyntaxHighlighting syntaxHighlighting = null;

  // error highlighter
  private LatexErrorHighlighting errorHighlighting = null;

  // compile thread
  private LatexCompiler latexCompiler = null;

  // current file
  private String fileName = null;

	public static void main(String args[]){
    JLatexEditorJFrame latexEditor = new JLatexEditorJFrame("editor.JLatexEditor");
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
    editor = new SourceCodeEditor();
	  SCEPane scePane = editor.getTextPane();
	  document = scePane.getDocument();
	  
    // add some styles to the document
    LatexStyles.addStyles(document);

    // TextArea for error messages
    errorMessages = new JTextArea();
    errorMessages.setFont(new Font("MonoSpaced", 0, 13));

    textErrorSplit = new JSplitPane(JSplitPane.VERTICAL_SPLIT, true, editor, new JScrollPane(errorMessages));
    textErrorSplit.setOneTouchExpandable(true);
    textErrorSplit.setResizeWeight(0.5);

    // add the ScrollPane
    getContentPane().add(textErrorSplit, BorderLayout.CENTER);
    getContentPane().validate();

    // syntax highlighting
    syntaxHighlighting = new LatexSyntaxHighlighting(scePane);
    syntaxHighlighting.start();

	  // code completion and quick help
	  scePane.setCodeHelper(new LatexCodeHelper("data/codehelper/commands.xml"));
	  scePane.setQuickHelp(new LatexQuickHelp("data/quickhelp/"));
	  
    // error highlighting
    errorHighlighting = new LatexErrorHighlighting(scePane, errorMessages);
  }

  // ActionListener methods
  public void actionPerformed(ActionEvent e){
    // open a file
    if(e.getActionCommand().equals("open")){
      FileDialog dialog = new FileDialog(this, "Tex-Dokument öffnen", FileDialog.LOAD);
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
      latexCompiler = new LatexCompiler(editor.getTextPane(), fileName);
      latexCompiler.addLatexCompileListener(errorHighlighting);

      latexCompiler.run();
    }
  }
}

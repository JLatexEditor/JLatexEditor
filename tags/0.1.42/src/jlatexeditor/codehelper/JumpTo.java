package jlatexeditor.codehelper;

import jlatexeditor.Doc;
import jlatexeditor.JLatexEditorJFrame;
import jlatexeditor.Doc.FileDoc;
import sce.codehelper.PatternPair;
import sce.codehelper.WordWithPos;
import sce.component.SCEDocumentPosition;
import sce.component.SCEPane;
import sce.component.SCEPosition;
import sce.component.SourceCodeEditor;

import java.awt.event.*;
import java.io.File;
import java.util.Arrays;
import java.util.List;

/**
 * Jump to completion (control+b).
 *
 * @author Stefan Endrullis
 */
public class JumpTo implements KeyListener, MouseListener {
  private static List<String> defaultExtensions = Arrays.asList("", ".tex", ".bib");

  private PatternPair parameterPattern = new PatternPair("\\{([^\\{]*)", "([^\\}]*)\\}");
	private PatternPair commandParamPattern = new PatternPair("\\\\(\\w+)\\{[^\\{]*");
	private PatternPair commandPattern = new PatternPair("\\\\(\\w*)", "(\\w+)");

  private SourceCodeEditor editor;
  private JLatexEditorJFrame jLatexEditorJFrame;

	private BackgroundParser backgroundParser;

  public JumpTo(SourceCodeEditor editor, JLatexEditorJFrame jLatexEditorJFrame, BackgroundParser backgroundParser) {
    this.editor = editor;
    this.jLatexEditorJFrame = jLatexEditorJFrame;
	  this.backgroundParser = backgroundParser;
    editor.getTextPane().addKeyListener(this);
    editor.getTextPane().addMouseListener(this);
  }

  public void keyTyped(KeyEvent e) {
  }

  public void keyPressed(KeyEvent e) {
    // control+b
    if (e.getKeyCode() == KeyEvent.VK_B && e.getModifiers() == KeyEvent.CTRL_MASK) {
      jumpTo(editor.getTextPane().getCaret(), e);
    }
  }

  private void jumpTo(SCEPosition pos, InputEvent e) {
    SCEPane pane = editor.getTextPane();

	  // cursor under command?
	  List<WordWithPos> words = commandPattern.find(pane, pos);
	  if (words != null) {
		  // extract command name
	    WordWithPos word = words.get(0);

		  // try to jump to command definition
		  BackgroundParser.FilePos filePos = backgroundParser.getCommands().get(word.word);
		  if (filePos != null) {
			  jLatexEditorJFrame.open(new FileDoc(new File(filePos.getFile())), filePos.getLineNr());
			  return;
		  }
	  }

	  // cursor placed under command parameter?
	  words = parameterPattern.find(pane, pos);
    if (words != null) {
	    // extract the parameter
      WordWithPos word = words.get(0);

	    // get command name
	    List<WordWithPos> commandList = commandParamPattern.find(pane, pos);
	    if (commandList != null) {
		    String command = commandList.get(0).word;

		    // if \ref or \eqref -> try to jump to label definition
		    if (command.equals("ref") || command.equals("eqref")) {
			    BackgroundParser.FilePos filePos = backgroundParser.getLabels().get(word.word);
			    if (filePos != null) {
						jLatexEditorJFrame.open(new FileDoc(new File(filePos.getFile())), filePos.getLineNr());
						return;
			    }
		    }
	    }

	    // try to jump to file un
      if (editor.getResource() instanceof FileDoc) {
        FileDoc fileDoc = (Doc.FileDoc) editor.getResource();
        String thisFileName = fileDoc.getFile().getName();
        File dir = fileDoc.getFile().getParentFile();

        for (String extension : defaultExtensions) {
          String thatFileName = word.word + extension;
          if (thisFileName.equals(thatFileName)) continue;
          File fileUnderCaret = new File(dir, thatFileName);

          if (fileUnderCaret.exists() && fileUnderCaret.isFile()) {
            jLatexEditorJFrame.open(new FileDoc(fileUnderCaret));
            e.consume();
            return;
          }
        }
      }

	    return;
    }
  }

  public void keyReleased(KeyEvent e) {
  }

  public void mouseClicked(MouseEvent e) {
  }

  public void mousePressed(MouseEvent e) {
    SCEDocumentPosition position = editor.getTextPane().viewToModel(e.getX(), e.getY());

    // control + mouse button 1
    if ((e.getModifiers() & MouseEvent.BUTTON1_MASK) != 0 && (e.getModifiers() & KeyEvent.CTRL_MASK) != 0) {
      jumpTo(position, e);
    }
  }

  public void mouseReleased(MouseEvent e) {
  }

  public void mouseEntered(MouseEvent e) {
  }

  public void mouseExited(MouseEvent e) {
  }
}
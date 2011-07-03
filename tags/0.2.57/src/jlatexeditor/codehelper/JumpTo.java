package jlatexeditor.codehelper;

import jlatexeditor.Doc;
import jlatexeditor.Doc.FileDoc;
import jlatexeditor.SCEManager;
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

  private SourceCodeEditor editor;

  public JumpTo(SourceCodeEditor editor) {
    this.editor = editor;
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
	  List<WordWithPos> words = CodePattern.commandPattern.find(pane, pos);
	  if (words != null) {
		  // extract command name
	    WordWithPos word = words.get(0);

		  // try to jump to command definition
		  BackgroundParser.FilePos filePos = SCEManager.getBackgroundParser().getCommands().get(word.word);
		  if (filePos != null) {
			  SCEManager.getInstance().open(new FileDoc(new File(filePos.getFile())), filePos.getLineNr());
			  return;
		  }
	  }

	  // cursor placed under command parameter?
	  words = CodePattern.parameterPattern.find(pane, pos);
    if (words != null) {
	    // extract the parameter
      WordWithPos word = words.get(0);

	    // get command name
	    List<WordWithPos> commandList = CodePattern.commandParamPattern.find(pane, pos);
	    if (commandList != null) {
		    String command = commandList.get(0).word;

		    // if \ref or \eqref -> try to jump to label definition
		    if (command.equals("ref") || command.equals("eqref")) {
			    BackgroundParser.FilePos filePos = SCEManager.getBackgroundParser().getLabelDefs().get(word.word);
			    if (filePos != null) {
						SCEManager.getInstance().open(new FileDoc(new File(filePos.getFile())), filePos.getLineNr());
						return;
			    }
		    } else
		    if (command.equals("cite")) {
			    BackgroundParser.FilePos filePos = SCEManager.getBackgroundParser().getBibKeys2bibEntries().get(word.word);
			    if (filePos != null) {
						SCEManager.getInstance().open(new FileDoc(new File(filePos.getFile())), filePos.getLineNr());
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
            SCEManager.getInstance().open(new FileDoc(fileUnderCaret));
            e.consume();
            return;
          }
        }
      }
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

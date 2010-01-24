package jlatexeditor.codehelper;

import jlatexeditor.JLatexEditorJFrame;
import jlatexeditor.JLatexEditorJFrame.FileDoc;
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

	private PatternPair pattern = new PatternPair("\\{([^\\{]*)", "([^\\}]*)\\}");

	private SourceCodeEditor editor;
	private JLatexEditorJFrame jLatexEditorJFrame;

	public JumpTo(SourceCodeEditor editor, JLatexEditorJFrame jLatexEditorJFrame) {
		this.editor = editor;
		this.jLatexEditorJFrame = jLatexEditorJFrame;
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

		WordWithPos word = pattern.find(pane, pos);
		if (word != null) {
			if (editor.getResource() instanceof FileDoc) {
			  FileDoc fileDoc = (FileDoc) editor.getResource();
				String thisFileName = fileDoc.getFile().getName();
				File dir = fileDoc.getFile().getParentFile();

				for (String extension : defaultExtensions) {
					String thatFileName = word.word + extension;
					if (thisFileName.equals(thatFileName)) continue;
					File fileUnderCaret = new File (dir, thatFileName);

					if (fileUnderCaret.exists() && fileUnderCaret.isFile()) {
						jLatexEditorJFrame.open(new FileDoc(fileUnderCaret));
						e.consume();
						return;
					}
				}
			}
		}
	}

	public void keyReleased(KeyEvent e) {}
	public void mouseClicked(MouseEvent e) {}

	public void mousePressed(MouseEvent e){
		SCEDocumentPosition position = editor.getTextPane().viewToModel(e.getX(), e.getY());
		
	  // control + mouse button 1
	  if((e.getModifiers() & MouseEvent.BUTTON1_MASK) != 0 && (e.getModifiers() & KeyEvent.CTRL_MASK) != 0) {
		  jumpTo(position, e);
	  }
	}

	public void mouseReleased(MouseEvent e) {}
	public void mouseEntered(MouseEvent e) {}
	public void mouseExited(MouseEvent e) {}
}

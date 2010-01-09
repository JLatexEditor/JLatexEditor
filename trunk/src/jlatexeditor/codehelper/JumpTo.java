package jlatexeditor.codehelper;

import jlatexeditor.JLatexEditorJFrame;
import jlatexeditor.JLatexEditorJFrame.FileDoc;
import sce.component.SCEPane;
import sce.component.SourceCodeEditor;

import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.io.File;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Jump to completion (control+b).
 *
 * @author Stefan Endrullis
 */
public class JumpTo implements KeyListener {
	private static List<String> defaultExtensions = Arrays.asList("", ".tex");

	private PatternPair pattern = new PatternPair("\\{([^\\{]*)", "([^\\}]*)\\}");

	private SourceCodeEditor editor;
	private JLatexEditorJFrame jLatexEditorJFrame;

	public JumpTo(SourceCodeEditor editor, JLatexEditorJFrame jLatexEditorJFrame) {
		this.editor = editor;
		this.jLatexEditorJFrame = jLatexEditorJFrame;
		editor.getTextPane().addKeyListener(this);
	}

	public void keyTyped(KeyEvent e) {
	}

	public void keyPressed(KeyEvent e) {
		if (e.getKeyCode() == KeyEvent.VK_B && e.getModifiers() == KeyEvent.CTRL_MASK) {
			SCEPane pane = editor.getTextPane();

			WordWithPos word = pattern.find(pane);
			if (word != null) {
				if (editor.getResource() instanceof FileDoc) {
				  FileDoc fileDoc = (FileDoc) editor.getResource();
					File dir = fileDoc.getFile().getParentFile();

					for (String extension : defaultExtensions) {
						File fileUnderCaret = new File (dir, word.word + extension);

						if (fileUnderCaret.exists() && fileUnderCaret.isFile()) {
							jLatexEditorJFrame.open(new FileDoc(fileUnderCaret));
							e.consume();
							return;
						}
					}
				}
			}
		}
	}

	public void keyReleased(KeyEvent e) {
	}
}

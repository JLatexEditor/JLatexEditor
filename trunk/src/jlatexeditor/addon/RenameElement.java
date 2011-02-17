package jlatexeditor.addon;

import jlatexeditor.JLatexEditorJFrame;

/**
 * Rename element under cursor.
 *
 * @author Stefan Endrullis &lt;stefan@endrullis.de&gt;
 */
public class RenameElement extends AddOn {
	protected RenameElement() {
		super("rename element", "Rename Element", "F6");
	}

	@Override
	public void run(JLatexEditorJFrame jle) {
	}
}

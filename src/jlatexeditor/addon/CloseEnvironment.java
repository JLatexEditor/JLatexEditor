package jlatexeditor.addon;

import jlatexeditor.JLatexEditorJFrame;
import sce.codehelper.WordWithPos;
import sce.component.SCEPane;

import java.util.Iterator;

/**
 * @author Stefan Endrullis &lt;stefan@endrullis.de&gt;
 */
public class CloseEnvironment extends AddOn {
	protected CloseEnvironment() {
		super("close environment", "Close Environment", "control alt E");
	}

	@Override
	public void run(JLatexEditorJFrame jle) {
		SCEPane pane = jle.getActiveEditor().getTextPane();
		Iterator<WordWithPos> openEnvIterator = EnvironmentUtils.getOpenEnvIterator(pane);
		WordWithPos env = openEnvIterator.next();
		if (env != null) {
			pane.insert("\\end{" + env.word + "}");
		}
	}
}

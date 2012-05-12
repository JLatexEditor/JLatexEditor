package jlatexeditor.addon;

import jlatexeditor.JLatexEditorJFrame;
import sce.codehelper.WordWithPos;
import sce.component.SCEPane;

import java.util.Iterator;

/**
 * Close current environment.
 *
 * @author Stefan Endrullis &lt;stefan@endrullis.de&gt;
 */
public class CompileAndOpen extends AddOn {
	protected CompileAndOpen() {
		super("build pdf and open", "Build pdf and Open Document", "alt B");
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

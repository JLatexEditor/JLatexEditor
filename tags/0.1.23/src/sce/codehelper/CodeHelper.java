package sce.codehelper;

import sce.component.SCEDocument;
import sce.component.SCEPane;

/**
 * Code helper which is responsible for the code completion.
 *
 * @author Stefan Endrullis
 */
public abstract class CodeHelper {
	protected SCEPane pane = null;
	protected SCEDocument document = null;

	public abstract boolean matches();
	public boolean documentChanged() {
		return matches();
	}
	public abstract WordWithPos getWordToReplace();
	public abstract Iterable<? extends CHCommand> getCompletions();
	public abstract String getMaxCommonPrefix();

  // getters and setters
	public void setSCEPane(SCEPane pane) {
		this.pane = pane;
		this.document = pane.getDocument();
	}
}

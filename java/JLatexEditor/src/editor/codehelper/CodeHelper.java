package editor.codehelper;

import editor.component.SCEDocument;

import java.util.Iterator;

/**
 * Code helper which is responsible for the code completion.
 *
 * @author Stefan Endrullis
 */
public abstract class CodeHelper {
	protected SCEDocument document = null;

	public abstract Iterable<CHCommand> getCommandsAt(int row, int column);

// getters and setters
	public void setDocument(SCEDocument document) {
		this.document = document;
	}
}

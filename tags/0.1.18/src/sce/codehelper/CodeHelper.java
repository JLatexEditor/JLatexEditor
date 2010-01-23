package sce.codehelper;

import sce.component.SCEDocument;

/**
 * Code helper which is responsible for the code completion.
 *
 * @author Stefan Endrullis
 */
public abstract class CodeHelper {
	protected SCEDocument document = null;

  public abstract Iterable<? extends CHCommand> getCommands(String search);
  public abstract String getCompletion(String search);

  // getters and setters
	public void setDocument(SCEDocument document) {
		this.document = document;
	}
}

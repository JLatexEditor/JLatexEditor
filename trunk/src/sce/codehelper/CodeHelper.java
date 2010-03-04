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
	protected boolean autoCompletion = false;
	protected int autoCompletionMinLetters = 3;
	protected int autoCompletionDelay = 1000;

	/**
	 * Checks whether it can be applied to the current document position.
	 *
	 * @return true if it can be applied to the current document position
	 */
  public abstract boolean matches();

	/**
	 * Informs the code helper about a document change.  The code helper can use this information
	 * to check whether it still matches the current document position.
	 *
	 * @return true if it can be applied to the current document position
	 */
  public boolean documentChanged() {
    return matches();
  }

	/**
	 * Returns the word that shall be replaced.
	 *
	 * @return word that shall be replaced.
	 */
  public abstract WordWithPos getWordToReplace();

	/**
	 * Returns a list of possible completions.
	 *
	 * @return list of completion suggestions.
	 */
  public abstract Iterable<? extends CHCommand> getCompletions();

	/**
	 * Returns the maximal common prefix of the completion suggestions
	 * that could be applied to the current context.
	 *
	 * @return maximal common prefix of the completion suggestions
	 */
  public abstract String getMaxCommonPrefix();

  // getters and setters

  public void setSCEPane(SCEPane pane) {
    this.pane = pane;
    this.document = pane.getDocument();
  }

	public void setAutoCompletion(boolean activated) {
		this.autoCompletion = activated;
	}

	public void setAutoCompletionMinLetters(int autoCompletionMinLetters) {
		this.autoCompletionMinLetters = autoCompletionMinLetters;
	}

	public void setAutoCompletionDelay(int autoCompletionDelay) {
		this.autoCompletionDelay = autoCompletionDelay;
	}
}

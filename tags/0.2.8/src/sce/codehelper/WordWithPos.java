package sce.codehelper;

import sce.component.SCEDocumentPosition;
import sce.component.SCEDocumentRange;
import sce.component.SCEPosition;

/**
 * Word and its position in the document.
 *
 * @author Stefan Endrullis
 */
public class WordWithPos extends SCEDocumentRange {
  public String word;

  public WordWithPos(String word, int row, int startColumn) {
    this(word, new SCEDocumentPosition(row, startColumn));
  }

	public WordWithPos(String word, SCEPosition position) {
		super(position, new SCEDocumentPosition(position.getRow(), position.getColumn() + (word == null ? 0 : word.length())));
		this.word = word;
	}

  @Override
  public String toString() {
    return word + " @(" + startPos.getRow() + "," + startPos.getColumn() + ")";
  }
}

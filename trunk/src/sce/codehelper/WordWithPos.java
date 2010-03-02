package sce.codehelper;

import sce.component.SCERange;

/**
 * Word and its position in the document.
 *
 * @author Stefan Endrullis
 */
public class WordWithPos extends SCERange {
  public String word;

  public WordWithPos(String word, int row, int startColumn) {
    super(row, startColumn, row, startColumn + (word == null ? 0 : word.length()));
    this.word = word;
  }

  @Override
  public String toString() {
    return word + " @(" + startRow + "," + startCol + ")";
  }
}

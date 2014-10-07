package sce.codehelper;

import sce.component.SCECaret;
import sce.component.SCEPane;
import sce.component.SCEPosition;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Pair of two regular expressions.  First one matches the string left to the cursor,
 * second one the string right to the cursor.
 *
 * @author Stefan Endrullis
 */
public class PatternPair {
  private Pattern leftPattern;
  private Pattern rightPattern;
  private boolean combine;

  public PatternPair(String leftPattern, String rightPattern) {
    this(leftPattern, true, rightPattern);
  }

  public PatternPair(String leftPattern, boolean combine, String rightPattern) {
    this.leftPattern = Pattern.compile(leftPattern + "$");
    this.combine = combine;
    this.rightPattern = Pattern.compile("^" + rightPattern);
  }

  public PatternPair(String leftPattern) {
    this(leftPattern, false, "");
  }

  public List<WordWithPos> find(SCEPane pane) {
    return find(pane, null, null);
  }

  public List<WordWithPos> find(SCEPane pane, SCEPosition pos) {
    return find(pane, pos.getRow(), pos.getColumn(), null, null);
  }

  public List<WordWithPos> find(SCEPane pane, SCEPosition start, SCEPosition end) {
    SCECaret caret = pane.getCaret();

    return find(pane, caret.getRow(), caret.getColumn(), start, end);
  }

  public List<WordWithPos> find(SCEPane pane, int row, int column, SCEPosition start, SCEPosition end) {
    String string = pane.getDocument().getRowsModel().getRowAsString(row);

    if(end != null && row == end.getRow()) {
      string = string.substring(0,end.getColumn());
    }
    int shift = 0;
    if(start != null && row == start.getRow()) {
      string = string.substring(start.getColumn());
      shift = start.getColumn();
      column -= shift;
    }

    return find(string, row, column, shift);
  }

  public List<WordWithPos> find(String rowString, int row, int column) {
    return find(rowString, row, column, 0);
  }

  /**
   * Applies the pattern pair to the given position in the given row and
   * returns the list of groups as WordWithPos if the pattern could be applied or null if it could not match.
   *
   * @param rowString row string
   * @param row row number
   * @param column column number of the cursor position
   * @return list of groups as WordWithPos if the pattern could be applied or null if it could not match
   */
  public List<WordWithPos> find(String rowString, int row, int column, int columnShift) {
	  if (rowString.length() < column) return null;

    Matcher leftMatcher = leftPattern.matcher(rowString.substring(0, column));
    Matcher rightMatcher = rightPattern.matcher(rowString.substring(column, rowString.length()));

    if (leftMatcher.find() && rightMatcher.find()) {
	    int leftGroupCount = leftMatcher.groupCount();
	    int rightGroupCount = rightMatcher.groupCount();
      ArrayList<WordWithPos> groups = new ArrayList<WordWithPos>(leftGroupCount + rightGroupCount);

	    int leftGroupMax = combine ? leftGroupCount - 1 : leftGroupCount;
      int rightGroupMin = combine ? 2 : 1;
      for (int i = 1; i <= leftGroupMax; i++) {
        groups.add(new WordWithPos(leftMatcher.group(i), row, leftMatcher.start(i)+columnShift));
      }
      if (combine) {
        groups.add(new WordWithPos(leftMatcher.group(leftGroupCount) + rightMatcher.group(1), row, leftMatcher.start(leftGroupCount)+columnShift));
      }
      for (int i = rightGroupMin; i <= rightGroupCount; i++) {
        groups.add(new WordWithPos(rightMatcher.group(i) + rightMatcher.group(i), row, rightMatcher.start(i)+columnShift));
      }

      return groups;
    }

    return null;
  }
}

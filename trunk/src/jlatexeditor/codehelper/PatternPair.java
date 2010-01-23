package jlatexeditor.codehelper;

import sce.component.SCECaret;
import sce.component.SCEDocumentPosition;
import sce.component.SCEPane;
import sce.component.SCEPosition;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Pair of two regular expressions. First one for the string left from the cursor.
 * Second one for the string right from the cursor.
 *
 * @author Stefan Endrullis
 */
public class PatternPair {
	private Pattern leftPattern;
	private Pattern rightPattern;
	private int leftGroup;
	private int rightGroup;

	public PatternPair(String leftPattern, String rightPattern) {
		this(leftPattern, 1, rightPattern, 1);
	}

	public PatternPair(String leftPattern, int leftGroup, String rightPattern, int rightGroup) {
		this.leftPattern = Pattern.compile(leftPattern + "$");
		this.leftGroup = leftGroup;
		this.rightPattern = Pattern.compile("^" + rightPattern);
		this.rightGroup = rightGroup;
	}

	public WordWithPos find (SCEPane pane) {
		SCECaret caret = pane.getCaret();

		return find (pane.getDocument().getRow(caret.getRow()), caret.getRow(), caret.getColumn());
	}

	public WordWithPos find (SCEPane pane, SCEPosition pos) {
		SCECaret caret = pane.getCaret();

		return find (pane.getDocument().getRow(pos.getRow()), pos.getRow(), pos.getColumn());
	}

	public WordWithPos find (String rowString, int row, int column) {
		Matcher leftMatcher = leftPattern.matcher(rowString.substring(0, column));
		Matcher rightMatcher = rightPattern.matcher(rowString.substring(column, rowString.length()));

		if (leftMatcher.find() && rightMatcher.find()) {
			return new WordWithPos(leftMatcher.group(leftGroup) + rightMatcher.group(rightGroup), row, leftMatcher.start(leftGroup));
		}

		return null;
	}
}

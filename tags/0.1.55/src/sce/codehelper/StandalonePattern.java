package sce.codehelper;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Standalone pattern matcher to retrieve occurrences as list of WordWithPos.
 *
 * @author Stefan Endrullis &lt;stefan@endrullis.de&gt;
 */
public class StandalonePattern {
	private Pattern pattern;

	public StandalonePattern(String pattern) {
		this.pattern = Pattern.compile(pattern);
	}

	/**
	 * Applies the left pattern of the pattern pair to the given row without considering the a cursor position and
	 * returns the list of groups as WordWithPos if the pattern could be applied or null if it could not match.
	 *
	 * @param rowString row string
	 * @param row row number
	 * @return list of groups as WordWithPos if the pattern could be applied or null if it could not match
	 */
  public List<WordWithPos> findInRow(String rowString, int row) {
	  Matcher leftMatcher = pattern.matcher(rowString);

	  if (leftMatcher.find()) {
		  int leftGroupCount = leftMatcher.groupCount();
	    ArrayList<WordWithPos> groups = new ArrayList<WordWithPos>(leftGroupCount);

	    for (int i = 1; i <= leftGroupCount; i++) {
	      groups.add(new WordWithPos(leftMatcher.group(i), row, leftMatcher.start(i)));
	    }

	    return groups;
	  }

	  return null;
  }

	/**
	 * Applies the left pattern of the pattern pair to the given row without considering the a cursor position and
	 * returns the list of groups as WordWithPos if the pattern could be applied or null if it could not match.
	 *
	 * @param rowString row string
	 * @param row row number
	 * @return list of groups as WordWithPos if the pattern could be applied or null if it could not match
	 */
  public List<List<WordWithPos>> findAllInRow(String rowString, int row) {
    Matcher leftMatcher = pattern.matcher(rowString);

	  ArrayList<List<WordWithPos>> list = new ArrayList<List<WordWithPos>>();

    while (leftMatcher.find()) {
	    int leftGroupCount = leftMatcher.groupCount();
      ArrayList<WordWithPos> groups = new ArrayList<WordWithPos>(leftGroupCount);

      for (int i = 1; i <= leftGroupCount; i++) {
        groups.add(new WordWithPos(leftMatcher.group(i), row, leftMatcher.start(i)));
      }

      list.add(groups);
    }

    return list;
  }
}

package jlatexeditor.codehelper;

import sce.codehelper.PatternPair;

/**
 * @author Stefan Endrullis &lt;stefan@endrullis.de&gt;
 */
public class CodePattern {
  // allow ...({...})*...
  public static final String flatPattern = "[^\\{\\}]*(?:\\{[^\\{\\}]*\\}[^\\{\\}]*)*(?:\\{[^\\{\\}]*)?";

	public static final PatternPair parameterPattern = new PatternPair("\\{([^\\{]*)", "([^\\}]*)\\}");
	public static final PatternPair commandParamPattern = new PatternPair("\\\\(\\p{L}+)(?:\\[[^\\{\\}\\[\\]]*\\])?\\{" + flatPattern);
	public static final PatternPair commandPattern = new PatternPair("\\\\(\\p{L}*)", "(\\p{L}*)");
	public static final PatternPair environmentPattern = new PatternPair("\\\\(begin|end)\\{(\\p{L}*)", "(\\p{L}*)\\}");

  public static final PatternPair citeParameterPattern = new PatternPair("(?:\\{|, *)([^\\{\\}, ]*)", "([^\\{\\}, ]*) *(?:\\}|,)");
  public static final PatternPair bibItemPattern = new PatternPair("@[\\w\\W]+ *\\{ *([^ ,\\}]*)", "([^ ,\\}]+)[ ,\\}]");
}

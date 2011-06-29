package jlatexeditor.codehelper;

import sce.codehelper.PatternPair;

/**
 * @author Stefan Endrullis &lt;stefan@endrullis.de&gt;
 */
public class CodePattern {
	public static final PatternPair parameterPattern = new PatternPair("\\{([^\\{]*)", "([^\\}]*)\\}");
	public static final PatternPair commandParamPattern = new PatternPair("\\\\(\\p{L}+)\\{[^\\{]*");
	public static final PatternPair commandPattern = new PatternPair("\\\\(\\p{L}*)", "(\\p{L}+)");
}
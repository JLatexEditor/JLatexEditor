package jlatexeditor.codehelper;

import sce.codehelper.PatternPair;

import java.util.Arrays;
import java.util.List;

/**
 * @author Stefan Endrullis &lt;stefan@endrullis.de&gt;
 */
public class CodePattern {
	public static final PatternPair parameterPattern = new PatternPair("\\{([^\\{]*)", "([^\\}]*)\\}");
	public static final PatternPair commandParamPattern = new PatternPair("\\\\(\\w+)\\{[^\\{]*");
	public static final PatternPair commandPattern = new PatternPair("\\\\(\\w*)", "(\\w+)");
}

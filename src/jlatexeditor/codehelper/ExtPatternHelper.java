package jlatexeditor.codehelper;

import jlatexeditor.PackagesExtractor;
import sce.codehelper.CHCommand;
import util.Function1;
import util.TrieSet;

import java.util.Comparator;

/**
 * @author Stefan Endrullis &lt;stefan@endrullis.de&gt;
 */
public abstract class ExtPatternHelper extends PatternHelper {
	protected static final Comparator<String> STRING_COMPARATOR = new Comparator<String>() {
		@Override
		public int compare(String o1, String o2) {
			return o1.compareTo(o2);
		}
	};
	protected  static final Function1<String,CHCommand> STRING_2_CHCOMMAND = new Function1<String, CHCommand>() {
		public CHCommand apply(String packageName) {
			return new ValueCompletion(packageName);
		}
	};
}

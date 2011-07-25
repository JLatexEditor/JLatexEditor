package jlatexeditor.codehelper;

import de.endrullis.utils.BetterProperties2;
import jlatexeditor.PackagesExtractor;
import jlatexeditor.gproperties.GProperties;
import sce.codehelper.CHCommand;
import sce.codehelper.WordWithPos;
import util.Function1;
import util.TrieSet;

import java.util.Comparator;

/**
 * @author Stefan Endrullis &lt;stefan@endrullis.de&gt;
 */
public abstract class ExtPatternHelper<T> extends PatternHelper {
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

	protected String type;
	protected WordWithPos word;

	protected ExtPatternHelper(String type) {
		this.type = type;
	}

	@Override
	public WordWithPos getWordToReplace() {
	  return word;
	}

	@Override
	public Iterable<? extends CHCommand> getCompletions(int level) {
		int minUsageCount = GProperties.getInt("editor.completion." + type + ".filter.level" + level);
	  return getCompletions(word.word, minUsage(minUsageCount));
	}

	@Override
	public String getMaxCommonPrefix() {
		return getMaxCommonPrefix(word.word);
	}

	protected abstract String getMaxCommonPrefix(String search);

	protected abstract Iterable<CHCommand> getCompletions(String search, Function1<T, Boolean> filterFunc);

	protected abstract Function1<T, Boolean> minUsage(final int minUsageCount);

	protected String maxCommonPrefix(String prefix1, String prefix2) {
		char[] chars1 = prefix1.toCharArray();
		char[] chars2 = prefix2.toCharArray();

		int i;
		for (i = 0; i < chars1.length && i < chars2.length; i++) {
			if (chars1[i] != chars2[i]) {
				return prefix1.substring(0, i);
			}
		}

		return prefix1.substring(0, i);
	}
}

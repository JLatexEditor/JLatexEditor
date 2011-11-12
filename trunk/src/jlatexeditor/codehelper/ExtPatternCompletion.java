package jlatexeditor.codehelper;

import jlatexeditor.gproperties.GProperties;
import sce.codehelper.CHCommand;
import sce.codehelper.WordWithPos;
import util.Function1;
import util.SimpleMergedTrie;

import java.util.Comparator;

/**
 * @author Stefan Endrullis &lt;stefan@endrullis.de&gt;
 */
public abstract class ExtPatternCompletion<T> extends PatternCompletion {
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

	protected ExtPatternCompletion(String type) {
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

	public static String maxCommonPrefix(String prefix1, String prefix2) {
		return SimpleMergedTrie.maxCommonPrefix(prefix1, prefix2);
	}
}

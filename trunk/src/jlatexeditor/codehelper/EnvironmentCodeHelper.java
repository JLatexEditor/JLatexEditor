package jlatexeditor.codehelper;

import de.endrullis.utils.collections.ExtIterable;
import de.endrullis.utils.collections.MergeSortIterable;
import jlatexeditor.PackagesExtractor;
import jlatexeditor.SCEManager;
import sce.codehelper.CHCommand;
import sce.codehelper.PatternPair;
import sce.codehelper.WordWithPos;
import util.Function1;
import util.TrieSet;

/**
 * CodeHelper for \\begin{...} and \\end{...}.
 *
 * @author Stefan Endrullis &lt;stefan@endrullis.de&gt;
 */
public class EnvironmentCodeHelper extends ExtPatternHelper {
	protected static final Function1<TrieSet<PackagesExtractor.Environment>,String> TRIE_SET_2_STRING_FUNCTION = new Function1<TrieSet<PackagesExtractor.Environment>, String>() {
		@Override
		public String apply(TrieSet<PackagesExtractor.Environment> trieSet) {
			return trieSet.getObjects().iterator().next().getName();
		}
	};
	protected  static final Function1<Environment,String> ENVIRONMENT_2_STRING_FUNCTION = new Function1<Environment, String>() {
		public String apply(Environment env) {
			return env.getName();
		}
	};

	protected WordWithPos word;

	public EnvironmentCodeHelper() {
	  pattern = new PatternPair("\\\\(?:begin|end)\\s*\\{([^{}]*)");
  }

	@Override
	public boolean matches() {
	  if (super.matches()) {
	    word = params.get(0);
	    return true;
	  }
	  return false;
	}

	@Override
	public WordWithPos getWordToReplace() {
	  return word;
	}

	@Override
	public Iterable<? extends CHCommand> getCompletions(int level) {
		int minUsageCount = 0;
		switch (level) {
			case 1:  minUsageCount = 50; break;
			case 2:  minUsageCount =  1; break;
			default: minUsageCount = -1; break;
		}
	  return getCompletions(word.word, minUsage(minUsageCount));
	}

	@Override
	public String getMaxCommonPrefix() {
	  return getMaxCommonPrefix(word.word);
	}

	public Iterable<CHCommand> getCompletions(String search, Function1 filterFunc) {
		ExtIterable<String> userIter = SCEManager.getBackgroundParser().getEnvironments().getObjectsIterable(search).map(ENVIRONMENT_2_STRING_FUNCTION);
		ExtIterable<String> packEnvIter = PackagesExtractor.getPackageParser().getEnvironments().getTrieSetIterator(search).filter(filterFunc).map(TRIE_SET_2_STRING_FUNCTION);
		ExtIterable<String> dcEnvIter = PackagesExtractor.getDocClassesParser().getEnvironments().getTrieSetIterator(search).filter(filterFunc).map(TRIE_SET_2_STRING_FUNCTION);

		return new MergeSortIterable<String>(STRING_COMPARATOR, userIter, packEnvIter, dcEnvIter).distinct().map(STRING_2_CHCOMMAND).toList(20);
	}

	protected Function1<TrieSet<PackagesExtractor.Environment>, Boolean> minUsage(final int minUsageCount) {
		return new Function1<TrieSet<PackagesExtractor.Environment>, Boolean>() {
			@Override
			public Boolean apply(TrieSet<PackagesExtractor.Environment> trieSet) {
				return trieSet.getObjects().iterator().next().getUsageCount() >= minUsageCount;
			}
		};
	}

	public String getMaxCommonPrefix(String search) {
		// TODO
	  return search;
	}
}

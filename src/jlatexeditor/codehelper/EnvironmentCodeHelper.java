package jlatexeditor.codehelper;

import de.endrullis.utils.collections.ExtIterable;
import de.endrullis.utils.collections.MergeSortIterable;
import jlatexeditor.PackagesExtractor;
import jlatexeditor.SCEManager;
import sce.codehelper.CHCommand;
import sce.codehelper.PatternPair;
import sce.codehelper.WordWithPos;
import util.AbstractTrie;
import util.Function1;
import util.TrieSet;

import java.util.Arrays;
import java.util.List;

/**
 * CodeHelper for \\begin{...} and \\end{...}.
 *
 * @author Stefan Endrullis &lt;stefan@endrullis.de&gt;
 */
public class EnvironmentCodeHelper extends ExtPatternHelper<TrieSet<PackagesExtractor.Environment>> {
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

	public EnvironmentCodeHelper() {
		super("environments");
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

	public Iterable<CHCommand> getCompletions(String search, Function1<TrieSet<PackagesExtractor.Environment>, Boolean> filterFunc) {
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
		List<AbstractTrie<? extends Object>> tries = Arrays.asList(
			SCEManager.getBackgroundParser().getEnvironments(),
			PackagesExtractor.getPackageParser().getEnvironments(),
			PackagesExtractor.getDocClassesParser().getEnvironments()
		);

		String maxPrefix = null;
		for (AbstractTrie<? extends Object> trie : tries) {
			String maxCommonPrefix = trie.getMaxCommonPrefix(search);
			if (maxCommonPrefix.length() >= search.length()) {
				if (maxPrefix == null) {
					maxPrefix = maxCommonPrefix;
				} else {
					maxPrefix = maxCommonPrefix(maxPrefix, maxCommonPrefix);
				}
			}
		}

		return maxPrefix;
	}
}

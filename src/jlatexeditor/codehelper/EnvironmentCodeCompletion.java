package jlatexeditor.codehelper;

import de.endrullis.utils.collections.ExtIterable;
import de.endrullis.utils.collections.MergeSortIterable;
import jlatexeditor.PackagesExtractor;
import jlatexeditor.SCEManager;
import sce.codehelper.CHCommand;
import sce.codehelper.PatternPair;
import util.SetTrie;
import util.Trie;
import util.Function1;

import java.util.Arrays;
import java.util.List;

/**
 * CodeCompletion for \\begin{...} and \\end{...}.
 *
 * @author Stefan Endrullis &lt;stefan@endrullis.de&gt;
 */
public class EnvironmentCodeCompletion extends ExtPatternCompletion<SetTrie<PackagesExtractor.Environment>> {
	protected static final Function1<SetTrie<PackagesExtractor.Environment>,String> TRIE_SET_2_STRING_FUNCTION = new Function1<SetTrie<PackagesExtractor.Environment>, String>() {
		@Override
		public String apply(SetTrie<PackagesExtractor.Environment> setTrie) {
			return setTrie.getObjects().iterator().next().getName();
		}
	};
	protected  static final Function1<Environment,String> ENVIRONMENT_2_STRING_FUNCTION = new Function1<Environment, String>() {
		public String apply(Environment env) {
			return env.getName();
		}
	};
	private static final Function1<CHCommand,String> CHCOMMAND_2_STING_FUNCTION = new Function1<CHCommand, String>() {
		public String apply(CHCommand a1) {
			return a1.getName();
		}
	};

	public EnvironmentCodeCompletion() {
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

	public Iterable<CHCommand> getCompletions(String search, Function1<SetTrie<PackagesExtractor.Environment>, Boolean> filterFunc) {
		ExtIterable<String> userIter = SCEManager.getBackgroundParser().getEnvironments().getObjectsIterable(search).map(ENVIRONMENT_2_STRING_FUNCTION);
		ExtIterable<String> standardIter = SCEManager.getLatexCommands().getEnvironments().getObjectsIterable(search).map(CHCOMMAND_2_STING_FUNCTION);
		ExtIterable<String> packEnvIter = PackagesExtractor.getPackageParser().getEnvironments().getTrieSetIterator(search).filter(filterFunc).map(TRIE_SET_2_STRING_FUNCTION);
		ExtIterable<String> dcEnvIter = PackagesExtractor.getDocClassesParser().getEnvironments().getTrieSetIterator(search).filter(filterFunc).map(TRIE_SET_2_STRING_FUNCTION);

		return new MergeSortIterable<String>(STRING_COMPARATOR, userIter, standardIter, packEnvIter, dcEnvIter).distinct().map(STRING_2_CHCOMMAND).toList(20);
	}

	protected Function1<SetTrie<PackagesExtractor.Environment>, Boolean> minUsage(final int minUsageCount) {
		return new Function1<SetTrie<PackagesExtractor.Environment>, Boolean>() {
			@Override
			public Boolean apply(SetTrie<PackagesExtractor.Environment> setTrie) {
				return setTrie.getObjects().iterator().next().getUsageCount() >= minUsageCount;
			}
		};
	}

	public String getMaxCommonPrefix(String search) {
		List<Trie<? extends Object>> tries = Arrays.asList(
			SCEManager.getBackgroundParser().getEnvironments(),
			SCEManager.getLatexCommands().getEnvironments(),
			PackagesExtractor.getPackageParser().getEnvironments(),
			PackagesExtractor.getDocClassesParser().getEnvironments()
		);

		String maxPrefix = null;
		for (Trie<? extends Object> trie : tries) {
			String maxCommonPrefix = trie.getMaxCommonPrefix(search);
			if (maxCommonPrefix.length() >= search.length()) {
				if (maxPrefix == null) {
					maxPrefix = maxCommonPrefix;
				} else {
					maxPrefix = maxCommonPrefix(maxPrefix, maxCommonPrefix);
				}
			}
		}
		if (maxPrefix == null) {
			maxPrefix = search;
		}

		return maxPrefix;
	}
}

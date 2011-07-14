package jlatexeditor.codehelper;

import de.endrullis.utils.CollectionUtils;
import de.endrullis.utils.ExtIterable;
import jlatexeditor.PackagesExtractor;
import jlatexeditor.SCEManager;
import sce.codehelper.CHCommand;
import sce.codehelper.PatternPair;
import sce.codehelper.WordWithPos;
import util.Function1;
import util.MergeSortIterable;
import util.TrieSet;

import java.util.Comparator;

/**
 * CodeHelper for \\begin{...} and \\end{...}.
 *
 * @author Stefan Endrullis &lt;stefan@endrullis.de&gt;
 */
public class EnvironmentCodeHelper extends PatternHelper {
	private static final Comparator<String> STRING_COMPARATOR = new Comparator<String>() {
		@Override
		public int compare(String o1, String o2) {
			return o1.compareTo(o2);
		}
	};
	private static final Function1<TrieSet<PackagesExtractor.Environment>,String> TRIE_SET_2_STRING_FUNCTION = new Function1<TrieSet<PackagesExtractor.Environment>, String>() {
		@Override
		public String apply(TrieSet<PackagesExtractor.Environment> trieSet) {
			return trieSet.getObjects().iterator().next().getName();
		}
	};
	private static final Function1<String,CHCommand> STRING_2_CHCOMMAND = new Function1<String, CHCommand>() {
		public CHCommand apply(String packageName) {
			return new ValueCompletion(packageName);
		}
	};
	private static final Function1<Environment,String> ENVIRONMENT_2_STRING_FUNCTION = new Function1<Environment, String>() {
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
	public Iterable<? extends CHCommand> getCompletions() {
	  return getCompletions(word.word);
	}

	@Override
	public String getMaxCommonPrefix() {
	  return getMaxCommonPrefix(word.word);
	}

	public Iterable<CHCommand> getCompletions(String search) {
		ExtIterable<String> userIter = SCEManager.getBackgroundParser().getEnvironments().getObjectsIterable(search).map(ENVIRONMENT_2_STRING_FUNCTION);
		int minUsageCount = 0;
		ExtIterable<String> packEnvIter = PackagesExtractor.getPackageParser().getEnvironments().getTrieSetIterator(search).filter(minUsage(minUsageCount)).map(TRIE_SET_2_STRING_FUNCTION);
		ExtIterable<String> dcEnvIter = PackagesExtractor.getDocClassesParser().getEnvironments().getTrieSetIterator(search).filter(minUsage(minUsageCount)).map(TRIE_SET_2_STRING_FUNCTION);

		ExtIterable<CHCommand> mergedIter = new MergeSortIterable<String>(STRING_COMPARATOR, userIter, packEnvIter, dcEnvIter).map(STRING_2_CHCOMMAND);

		return mergedIter.toList(20);

		/*
		List<String> envNames = PackagesExtractor.getPackageParser().getEnvironments().getStrings(search, 20);
		if (envNames == null) envNames = new ArrayList<String>();

		return CollectionUtils.map(envNames, STRING_2_CHCOMMAND);
		*/
	}

	private Function1<TrieSet<PackagesExtractor.Environment>, Boolean> minUsage(final int minUsageCount) {
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

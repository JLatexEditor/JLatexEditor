package jlatexeditor.codehelper;

import de.endrullis.utils.CollectionUtils;
import de.endrullis.utils.ExtIterable;
import jlatexeditor.PackagesExtractor;
import jlatexeditor.gproperties.GPropertiesCodeHelper;
import sce.codehelper.CHCommand;
import sce.codehelper.PatternPair;
import sce.codehelper.WordWithPos;
import util.Function1;
import util.MergeSortIterable;
import util.TrieSet;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;

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
		// TODO merge envs of docclasses and envs of packages
		ExtIterable<String> packEnvIter = PackagesExtractor.getPackageParser().getEnvironments().getTrieSetIterator(search).map(TRIE_SET_2_STRING_FUNCTION);
		ExtIterable<String> dcEnvIter = PackagesExtractor.getDocClassesParser().getEnvironments().getTrieSetIterator(search).map(TRIE_SET_2_STRING_FUNCTION);

		ExtIterable<CHCommand> mergedIter = new MergeSortIterable<String>(STRING_COMPARATOR, packEnvIter, dcEnvIter).map(STRING_2_CHCOMMAND);

		return CollectionUtils.take(mergedIter, 20);

		/*
		List<String> envNames = PackagesExtractor.getPackageParser().getEnvironments().getStrings(search, 20);
		if (envNames == null) envNames = new ArrayList<String>();

		return CollectionUtils.map(envNames, STRING_2_CHCOMMAND);
		*/
	}

	public String getMaxCommonPrefix(String search) {
		// TODO
	  return search;
	}
}

package jlatexeditor.codehelper;

import de.endrullis.utils.collections.ExtIterable;
import jlatexeditor.PackagesExtractor;
import jlatexeditor.SCEManager;
import sce.codehelper.CHCommand;
import sce.codehelper.PatternPair;
import sce.codehelper.WordWithPos;
import util.AbstractTrie;
import util.Function1;
import de.endrullis.utils.collections.MergeSortIterable;
import util.Trie;
import util.TrieSet;

import java.util.Arrays;
import java.util.List;

/**
 * Code helper for all LaTeX commands (static, user defined, and used ones).
 *
 * @author Stefan Endrullis &lt;stefan@endrullis.de&gt;
 */
public class CommandsCodeHelper extends ExtPatternHelper<TrieSet<PackagesExtractor.Command>> {
	protected static final Function1<TrieSet<PackagesExtractor.Command>,String> TRIE_SET_2_STRING_FUNCTION = new Function1<TrieSet<PackagesExtractor.Command>, String>() {
		@Override
		public String apply(TrieSet<PackagesExtractor.Command> trieSet) {
			return trieSet.getObjects().iterator().next().getName();
		}
	};
	protected static final Function1<Command,String> COMMAND_2_STRING_FUNCTION = new Function1<Command, String>() {
		public String apply(Command env) {
			return env.getName();
		}
	};
	private static final Function1<CHCommand,String> CHCOMMAND_2_STING_FUNCTION = new Function1<CHCommand, String>() {
		public String apply(CHCommand a1) {
			return a1.getName();
		}
	};

	public CommandsCodeHelper() {
		super("commands");
	  pattern = new PatternPair("\\\\(\\p{L}*)");
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
	public String getMaxCommonPrefix() {
	  return getMaxCommonPrefix(word.word);
	}

	public Iterable<CHCommand> getCompletions(String search, Function1<TrieSet<PackagesExtractor.Command>, Boolean> filterFunc) {
		final Trie<Command> userCommands = SCEManager.getBackgroundParser().getCommands();
		final Trie<CHCommand> standardCommands = SCEManager.getLatexCommands().getCommands();

		ExtIterable<String> userIter = userCommands.getObjectsIterable(search).map(COMMAND_2_STRING_FUNCTION);
		ExtIterable<String> standardIter = standardCommands.getObjectsIterable(search).map(CHCOMMAND_2_STING_FUNCTION);
		ExtIterable<String> packEnvIter = PackagesExtractor.getPackageParser().getCommands().getTrieSetIterator(search).filter(filterFunc).map(TRIE_SET_2_STRING_FUNCTION);
		ExtIterable<String> dcEnvIter = PackagesExtractor.getDocClassesParser().getCommands().getTrieSetIterator(search).filter(filterFunc).map(TRIE_SET_2_STRING_FUNCTION);

		return new MergeSortIterable<String>(STRING_COMPARATOR, userIter, standardIter, packEnvIter, dcEnvIter).distinct().map(new Function1<String, CHCommand>() {
			@Override
			public CHCommand apply(String cmd) {
				Command command = userCommands.get(cmd);
				if (command != null) {
					return command.toCHCommand();
				}
				CHCommand chCommand = standardCommands.get(cmd);
				if (chCommand != null) {
					return chCommand;
				}
				return new ValueCompletion(cmd);
			}
		}).toList(20);
	}

	protected Function1<TrieSet<PackagesExtractor.Command>, Boolean> minUsage(final int minUsageCount) {
		return new Function1<TrieSet<PackagesExtractor.Command>, Boolean>() {
			@Override
			public Boolean apply(TrieSet<PackagesExtractor.Command> trieSet) {
				return trieSet.getObjects().iterator().next().getUsageCount() >= minUsageCount;
			}
		};
	}

	public String getMaxCommonPrefix(String search) {
		List<AbstractTrie<? extends Object>> tries = Arrays.asList(
			SCEManager.getBackgroundParser().getCommands(),
			SCEManager.getLatexCommands().getCommands(),
			PackagesExtractor.getPackageParser().getCommands(),
			PackagesExtractor.getDocClassesParser().getCommands()
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

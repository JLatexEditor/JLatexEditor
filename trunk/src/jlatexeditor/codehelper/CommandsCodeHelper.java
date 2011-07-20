package jlatexeditor.codehelper;

import de.endrullis.utils.collections.ExtIterable;
import jlatexeditor.PackagesExtractor;
import jlatexeditor.SCEManager;
import sce.codehelper.CHCommand;
import sce.codehelper.PatternPair;
import sce.codehelper.WordWithPos;
import util.Function1;
import de.endrullis.utils.collections.MergeSortIterable;
import util.Trie;
import util.TrieSet;

/**
 * Code helper for all LaTeX commands (static, user defined, and used ones).
 *
 * @author Stefan Endrullis &lt;stefan@endrullis.de&gt;
 */
public class CommandsCodeHelper extends ExtPatternHelper {
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

	protected WordWithPos word;

	public CommandsCodeHelper() {
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
	public Iterable<? extends CHCommand> getCompletions() {
	  return getCompletions(word.word);
	}

	@Override
	public String getMaxCommonPrefix() {
	  return getMaxCommonPrefix(word.word);
	}

	public Iterable<CHCommand> getCompletions(String search) {
		final Trie<Command> userCommands = SCEManager.getBackgroundParser().getCommands();
		final Trie<CHCommand> standardCommands = SCEManager.getLatexCommands().getCommands();
		ExtIterable<String> userIter = userCommands.getObjectsIterable(search).map(COMMAND_2_STRING_FUNCTION);
		standardCommands.getObjectsIterable(search).map(new Function1<CHCommand, Object>() {
			public Object apply(CHCommand a1) {
				return a1.getName();
			}
		});
		int minUsageCount = 0;
		ExtIterable<String> packEnvIter = PackagesExtractor.getPackageParser().getCommands().getTrieSetIterator(search).filter(minUsage(minUsageCount)).map(TRIE_SET_2_STRING_FUNCTION);
		ExtIterable<String> dcEnvIter = PackagesExtractor.getDocClassesParser().getCommands().getTrieSetIterator(search).filter(minUsage(minUsageCount)).map(TRIE_SET_2_STRING_FUNCTION);

		ExtIterable<CHCommand> mergedIter = new MergeSortIterable<String>(STRING_COMPARATOR, userIter, packEnvIter, dcEnvIter).distinct().map(new Function1<String, CHCommand>() {
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
		});

		return mergedIter.toList(20);

		/*
		List<String> envNames = PackagesExtractor.getPackageParser().getEnvironments().getStrings(search, 20);
		if (envNames == null) envNames = new ArrayList<String>();

		return CollectionUtils.map(envNames, STRING_2_CHCOMMAND);
		*/
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
		// TODO
	  return search;
	}

	/*
	public CommandsCodeHelper() {
		super("\\\\(\\p{L}*)", SCEManager.getLatexCommands());
	}

	@Override
	public Iterable<CHCommand> getCompletions(String prefix) {
		ArrayList<CHCommand> dynamicCommands = new ArrayList<CHCommand>();
		BackgroundParser backgroundParser = SCEManager.getBackgroundParser();
		if (backgroundParser != null) {
			List<String> commandNames = backgroundParser.getCommandNames().getStrings(prefix.substring(1), 10);
			Trie<Command> userCommands = backgroundParser.getCommands();
			if (commandNames != null) {
				for (String commandName : commandNames) {
					Command userCommand = userCommands.get(commandName);
					if (userCommand != null) {
						dynamicCommands.add(userCommand.toCHCommand());
					} else {
						dynamicCommands.add(new CHCommand("\\" + commandName));
					}
				}
			}
		}
		Iterator<CHCommand> dynamicCommandsIter = dynamicCommands.iterator();
		ExtIterator<CHCommand> staticCommandsIter = commands.getObjectsIterable(prefix).iterator();

		ArrayList<CHCommand> list = new ArrayList<CHCommand>();

		CHCommand dynamicCommand = dynamicCommandsIter.hasNext() ? dynamicCommandsIter.next() : null;
		CHCommand staticCommand = staticCommandsIter.next();
		while (dynamicCommand != null && staticCommand != null) {
			int comp = staticCommand.compareTo(dynamicCommand);
			if (comp <= 0) {
				list.add(staticCommand);
				staticCommand = staticCommandsIter.next();
				if (comp == 0) {
					dynamicCommand = dynamicCommandsIter.hasNext() ? dynamicCommandsIter.next() : null;
				}
			} else {
				list.add(dynamicCommand);
				dynamicCommand = dynamicCommandsIter.hasNext() ? dynamicCommandsIter.next() : null;
			}
		}
		if (dynamicCommand != null) {
			list.add(dynamicCommand);
			while (dynamicCommandsIter.hasNext()) {
				list.add(dynamicCommandsIter.next());
			}
		}
		if (staticCommand != null) {
			list.add(staticCommand);
			while ((staticCommand = staticCommandsIter.next()) != null) {
				list.add(staticCommand);
			}
		}

		return list;
	}
		*/

	/**
	 * Searches for the best completion of the prefix.
	 *
	 * @param prefix the prefix
	 * @return the completion suggestion (without the prefix)
	 */
	/*
	@Override
	public String getMaxCommonPrefix(String prefix) {
		String maxCommonPrefix = commands.getMaxCommonPrefix(prefix);
		if (maxCommonPrefix.length() < prefix.length()) {
			maxCommonPrefix = null;
		}

		BackgroundParser backgroundParser = SCEManager.getBackgroundParser();
		if (backgroundParser != null) {
			String dynMaxCommonPrefix = "\\" + backgroundParser.getCommandNames().getMaxCommonPrefix(prefix.substring(1));
			if (dynMaxCommonPrefix.length() < prefix.length()) {
				dynMaxCommonPrefix = null;
			}

			if (maxCommonPrefix == null) {
				return dynMaxCommonPrefix;
			} else
			if (dynMaxCommonPrefix == null) {
				return maxCommonPrefix;
			} else {
				int length = Math.min(maxCommonPrefix.length(), dynMaxCommonPrefix.length());
				int i;
				for (i=0; i<length; i++) {
					if (maxCommonPrefix.charAt(i) != dynMaxCommonPrefix.charAt(i)) {
						break;
					}
				}
				return maxCommonPrefix.substring(0, i);
			}
		}

		return maxCommonPrefix;
	}
	*/
}

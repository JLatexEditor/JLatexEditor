package jlatexeditor.codehelper;

import de.endrullis.utils.collections.ExtIterable;
import jlatexeditor.PackagesExtractor;
import jlatexeditor.SCEManager;
import sce.codehelper.CHCommand;
import sce.codehelper.CHCommandArgument;
import sce.codehelper.PatternPair;
import util.Trie;
import util.Function1;
import de.endrullis.utils.collections.MergeSortIterable;
import util.SimpleTrie;
import util.SetTrie;

import java.util.*;

/**
 * Code helper for all LaTeX commands (static, user defined, and used ones).
 *
 * @author Stefan Endrullis &lt;stefan@endrullis.de&gt;
 */
public class CommandsCodeHelper extends ExtPatternHelper<SetTrie<PackagesExtractor.Command>> {
	protected static final Function1<SetTrie<PackagesExtractor.Command>,String> TRIE_SET_2_STRING_FUNCTION = new Function1<SetTrie<PackagesExtractor.Command>, String>() {
		@Override
		public String apply(SetTrie<PackagesExtractor.Command> setTrie) {
			return setTrie.getObjects().iterator().next().getName();
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
	  pattern = new PatternPair("(\\\\\\p{L}*)");
  }

	@Override
	public boolean matches() {
	  if (super.matches()) {
	    word = params.get(0);
	    return true;
	  }
	  return false;
	}

	public Iterable<CHCommand> getCompletions(String _search, Function1<SetTrie<PackagesExtractor.Command>, Boolean> filterFunc) {
		String search = _search.substring(1);
		final SimpleTrie<Command> userCommands = SCEManager.getBackgroundParser().getCommands();
		final SimpleTrie<CHCommand> standardCommands = SCEManager.getLatexCommands().getCommands();

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

				return new LazyCHCommand(cmd);
			}
		}).toList(20);
	}

	protected Function1<SetTrie<PackagesExtractor.Command>, Boolean> minUsage(final int minUsageCount) {
		return new Function1<SetTrie<PackagesExtractor.Command>, Boolean>() {
			@Override
			public Boolean apply(SetTrie<PackagesExtractor.Command> setTrie) {
				return setTrie.getObjects().iterator().next().getUsageCount() >= minUsageCount;
			}
		};
	}

	public String getMaxCommonPrefix(String _search) {
		String search = _search.substring(1);

		List<Trie<? extends Object>> tries = Arrays.asList(
			SCEManager.getBackgroundParser().getCommands(),
			SCEManager.getLatexCommands().getCommands(),
			PackagesExtractor.getPackageParser().getCommands(),
			PackagesExtractor.getDocClassesParser().getCommands()
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

		return "\\" + maxPrefix;
	}

	private class LazyCHCommand extends CHCommand {
		private CHCommand lazyCHCommand;
		private final String cmd;

		public LazyCHCommand(String cmd) {
			super(cmd);
			this.cmd = cmd;
		}

		private CHCommand getLazyCHCommand() {
			if (lazyCHCommand == null) {
				HashSet<PackagesExtractor.Command> commands = new HashSet<PackagesExtractor.Command>();
				commands.addAll(PackagesExtractor.getPackageParser().getCommands().getOrEmpty(cmd));
				commands.addAll(PackagesExtractor.getDocClassesParser().getCommands().getOrEmpty(cmd));

				PackagesExtractor.Command shortestCommand = null;
				for (PackagesExtractor.Command command1 : commands) {
					if (shortestCommand == null) {
						shortestCommand = command1;
					} else if (shortestCommand.getArgCount() > command1.getArgCount()) {
						shortestCommand = command1;
					}
				}

				assert shortestCommand != null;
				lazyCHCommand = shortestCommand.toCHCommand();
			}

			return lazyCHCommand;
		}

		@Override
		public String getUsage() {
			return getLazyCHCommand().getUsage();
		}

		@Override
		public String getHint() {
			return getLazyCHCommand().getHint();
		}

		@Override
		public ArrayList<CHCommandArgument> getArguments() {
			return getLazyCHCommand().getArguments();
		}
	}
}

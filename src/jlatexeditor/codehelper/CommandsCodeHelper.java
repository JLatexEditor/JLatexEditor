package jlatexeditor.codehelper;

import de.endrullis.utils.ExtIterable;
import de.endrullis.utils.ExtIterator;
import jlatexeditor.PackagesExtractor;
import jlatexeditor.SCEManager;
import sce.codehelper.CHCommand;
import sce.codehelper.StaticCommandsCodeHelper;
import util.Function1;
import util.MergeSortIterable;
import util.Trie;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * Code helper for all LaTeX commands (static, user defined, and used ones).
 *
 * @author Stefan Endrullis &lt;stefan@endrullis.de&gt;
 */
public class CommandsCodeHelper extends StaticCommandsCodeHelper {
	public CommandsCodeHelper() {
		super("(\\\\\\p{L}*)", SCEManager.getLatexCommands());
	}

	@Override
	public Iterable<CHCommand> getCompletions(String prefix) {
		/*
		ExtIterable<String> userIter = SCEManager.getBackgroundParser().getCommands().getObjectsIterable(search).map(COMMAND_2_STRING_FUNCTION);
		int minUsageCount = 0;
		ExtIterable<String> packEnvIter = PackagesExtractor.getPackageParser().getEnvironments().getTrieSetIterator(search).filter(minUsage(minUsageCount)).map(TRIE_SET_2_STRING_FUNCTION);
		ExtIterable<String> dcEnvIter = PackagesExtractor.getDocClassesParser().getEnvironments().getTrieSetIterator(search).filter(minUsage(minUsageCount)).map(TRIE_SET_2_STRING_FUNCTION);

		ExtIterable<CHCommand> mergedIter = new MergeSortIterable<String>(STRING_COMPARATOR, userIter, packEnvIter, dcEnvIter).map(STRING_2_CHCOMMAND);

		return mergedIter.toList(20);
		*/

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

	/**
	 * Searches for the best completion of the prefix.
	 *
	 * @param prefix the prefix
	 * @return the completion suggestion (without the prefix)
	 */
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
}

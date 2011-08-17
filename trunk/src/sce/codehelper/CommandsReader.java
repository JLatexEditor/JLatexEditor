package sce.codehelper;

import util.AbstractSimpleTrie;
import util.SimpleTrie;

/**
 * Interface for getting a Trie of commands.
 *
 * @author Stefan Endrullis &lt;stefan@endrullis.de&gt;
 */
public interface CommandsReader {
	public AbstractSimpleTrie<CHCommand> getCommands();
}

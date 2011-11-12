package sce.codehelper;

import util.AbstractSimpleTrie;
import util.SimpleTrie;

/**
 * Simple commands reader implementation.
 *
 * @author Stefan Endrullis &lt;stefan@endrullis.de&gt;
 */
public class SimpleCommandsReader implements CommandsReader {
	private AbstractSimpleTrie<CHCommand> commands;

	public SimpleCommandsReader(AbstractSimpleTrie<CHCommand> commands) {
		this.commands = commands;
	}

	@Override
	public AbstractSimpleTrie<CHCommand> getCommands() {
		return commands;
	}
}

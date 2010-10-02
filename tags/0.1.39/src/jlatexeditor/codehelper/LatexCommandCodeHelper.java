package jlatexeditor.codehelper;

import sce.codehelper.CHCommand;
import sce.codehelper.StaticCommandsCodeHelper;
import util.Trie;

/**
 * Code completion for LaTeX.
 *
 * @author Jörg Endrullis
 * @author Stefan Endrullis
 */
public class LatexCommandCodeHelper extends StaticCommandsCodeHelper {
  public LatexCommandCodeHelper(String patternString, String commandsXml) {
    super(patternString);
    // collect all commands
    readCommands(commandsXml);
  }

	public Trie<CHCommand> getCommands() {
		return commands;
	}
}

package jlatexeditor.codehelper;

import sce.codehelper.StaticCommandsCodeHelper;

/**
 * Code completion for LaTeX.
 *
 * @author Jörg Endrullis
 * @author Stefan Endrullis
 */
public class LatexCodeHelper extends StaticCommandsCodeHelper {
	public LatexCodeHelper(String commandsXml) {
		// collect all commands
		readCommands(commandsXml);
	}
}

package jlatexeditor.codehelper;

import sce.codehelper.StaticCommandsCodeHelper;

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
}

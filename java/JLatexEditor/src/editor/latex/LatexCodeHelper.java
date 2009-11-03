package editor.latex;

import editor.codehelper.CHCommand;
import editor.codehelper.CodeHelper;
import editor.codehelper.StaticCommandsCodeHelper;
import editor.component.SCEDocument;

import java.util.Iterator;

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

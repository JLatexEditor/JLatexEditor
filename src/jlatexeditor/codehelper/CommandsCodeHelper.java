package jlatexeditor.codehelper;

import jlatexeditor.JLatexEditorJFrame;
import sce.codehelper.CHCommand;
import sce.codehelper.PatternPair;
import sce.codehelper.StaticCommandsCodeHelper;
import sce.codehelper.WordWithPos;

import java.util.ArrayList;
import java.util.List;

/**
 * Code helper for all LaTeX commands (static, user defined, and used ones).
 *
 * @author Stefan Endrullis &lt;stefan@endrullis.de&gt;
 */
public class CommandsCodeHelper extends StaticCommandsCodeHelper {
	private JLatexEditorJFrame jle;

	public CommandsCodeHelper(JLatexEditorJFrame jle) {
		super("(\\\\\\p{L}*)", jle.getLatexCommands());
		this.jle = jle;
	}

	@Override
	public Iterable<? extends CHCommand> getCompletions(String prefix) {
		List<CHCommand> objects = commands.getObjects(prefix, 1000);
		if (objects != null) {
		  return objects;
		} else {
			return new ArrayList<CHCommand>();
		}
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
			return null;
		} else {
			return maxCommonPrefix;
		}
	}
}

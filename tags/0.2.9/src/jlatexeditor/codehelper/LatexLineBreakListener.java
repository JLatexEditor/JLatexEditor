package jlatexeditor.codehelper;

import jlatexeditor.addon.EnvironmentUtils;
import jlatexeditor.gproperties.GProperties;
import sce.codehelper.LineBreakListener;
import sce.codehelper.WordWithPos;
import sce.component.*;

import java.util.Iterator;
import java.util.regex.Pattern;

/**
 * Line break listener for Latex.
 *
 * @author Stefan Endrullis &lt;stefan@endrullis.de&gt;
 */
public class LatexLineBreakListener implements LineBreakListener {
	public static final Pattern itemPattern = Pattern.compile("\\\\item\\b");

	public void linedWrapped(SCEPane pane) {
		SCEDocument doc = pane.getDocument();
		SCECaret caret = pane.getCaret();
		int rowNr = caret.getRow();

		if (rowNr > 0) {
			String lastLine = doc.getRowsModel().getRowAsString(rowNr - 1);
			boolean indent = false;
			boolean closeEnv = false;
			String envName = null;

			// determine if last line is a comment
			if (lastLine.startsWith("%")) {
				if (GProperties.getBoolean("editor.auto_continue_comment")) {
					pane.insert("% ");
				}
			}
			// determine if a new environment was opened in the last line
			Iterator<WordWithPos> openEnvIterator = EnvironmentUtils.getOpenEnvIterator(pane);
			if (openEnvIterator.hasNext()) {
				WordWithPos openEnvWord = openEnvIterator.next();
				if (openEnvWord.getStartRow() == rowNr - 1) {
					indent   = GProperties.getBoolean("editor.auto_indentation.after_begin");
					closeEnv = GProperties.getBoolean("editor.auto_close_environment");
					envName  = openEnvWord.word;

					if (closeEnv) {
						// do not close env if it is already closed
						Iterator<WordWithPos> closeEnvIterator = EnvironmentUtils.getCloseEnvIterator(pane);
						if (closeEnvIterator.hasNext()) {
							WordWithPos closeEnvWord = closeEnvIterator.next();
							if (closeEnvWord.word.equals(envName)) {
								closeEnv = closeEnvWord.getStartCol() != openEnvWord.getStartCol() - 2;
							}
						}
					}
				}
			}
			// check further if we need to indent the new line
			if (!indent && GProperties.getBoolean("editor.auto_indentation.after_item")) {
				// indent if a new item was started in the last line
				indent = itemPattern.matcher(lastLine).find();
			}
			if (!indent && GProperties.getBoolean("editor.auto_indentation.after_opening_brace")) {
				int openingBraces = 0;
				for (char c : lastLine.toCharArray()) {
					switch (c) {
						case '{':
							openingBraces++;
							break;
						case '}':
							openingBraces = Math.max(0, openingBraces-1);
					}
				}
				indent = openingBraces > 0;
			}

			if (indent) {
				pane.insert("  ");
			}
			if (closeEnv) {
				SCEPosition oldPos = new SCEDocumentPosition(caret.getRow(), caret.getColumn());
				String currLine = doc.getRowsModel().getRowAsString(rowNr);
				// String restOfLine = currLine.substring(caret.getColumn());
				String nextIndentation = currLine.substring(0, Math.max(0, caret.getColumn()-2));
				pane.insert("\n" + nextIndentation + "\\end{" + envName + '}');
				caret.moveTo(oldPos, false);
			}
		}
	}
}

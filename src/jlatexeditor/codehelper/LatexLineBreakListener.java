package jlatexeditor.codehelper;

import jlatexeditor.addon.EnvironmentUtils;
import jlatexeditor.gproperties.GProperties;
import org.omg.CORBA.Environment;
import sce.codehelper.LineBreakListener;
import sce.codehelper.WordWithPos;
import sce.component.SCEDocument;
import sce.component.SCEPane;

import javax.naming.OperationNotSupportedException;
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
		int row = pane.getCaret().getRow();

		if (row > 0) {
			String lastLine = doc.getRow(row - 1);
			boolean indent = false;

			// check if we need to indent the new line
			if (GProperties.getBoolean("editor.auto_indentation.after_begin")) {
				// indent if a new environment was opened in the last row
				Iterator<WordWithPos> openEnvIterator = EnvironmentUtils.getOpenEnvIterator(pane);
				if (openEnvIterator.hasNext()) {
					indent = openEnvIterator.next().getStartRow() == row - 1;
				}
			}
			if (!indent && GProperties.getBoolean("editor.auto_indentation.after_item")) {
				// indent if a new item was started in the last row
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
		}
	}
}

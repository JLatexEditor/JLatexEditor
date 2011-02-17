package jlatexeditor.addon;

import jlatexeditor.cursorelement.CursorElement;
import sce.codehelper.WordWithPos;
import sce.component.SCECaret;
import sce.component.SCEDocument;
import sce.component.SCEPane;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Environment utils.
 *
 * @author Stefan Endrullis &lt;stefan@endrullis.de&gt;
 */
public class EnvironmentUtils {
	enum State {needSearch, searched, finished}

	private static final Pattern envPattern = Pattern.compile("\\\\(begin|end)\\{(\\w+)\\}");

	public static Iterator<WordWithPos> getOpenEnvIterator(final SCEPane pane) {
		return new OpenEnvIterator(pane);
	}

	public static class OpenEnvIterator implements Iterator<WordWithPos> {
		private State state = EnvironmentUtils.State.needSearch;
		private SCEPane pane;
		private SCECaret caret;
		private SCEDocument document;
		private LinkedList<WordWithPos> openEnvStack = new LinkedList<WordWithPos>();
		private LinkedList<String> closeEnvStack = new LinkedList<String>();
		private int rowNr;

		public OpenEnvIterator(SCEPane pane) {
			this.pane = pane;
			caret = pane.getCaret();
			document = pane.getDocument();
			rowNr = caret.getRow();

			String row = document.getRow(rowNr).substring(0, caret.getColumn());
			parseEnvs(openEnvStack, closeEnvStack, rowNr, row);
		}

		@Override
		public boolean hasNext() {
			if (openEnvStack.size() == 0) {
				// search above cursor
				for(rowNr--; rowNr >= 0; rowNr--) {
					String row = document.getRow(rowNr);
					parseEnvs(openEnvStack, closeEnvStack, rowNr, row);
					if (!openEnvStack.isEmpty()) break;
				}
			}
			return openEnvStack.peek() != null;
		}

		@Override
		public WordWithPos next() {
			if (hasNext()) {
				return openEnvStack.pop();
			} else {
				return null;
			}
		}

		@Override
		public void remove() {
			if (hasNext()) {
				openEnvStack.pop();
			}
		}

		/**
		 * Parses the row for environment openings and closings and updates the envStack correspondingly.
		 *
		 * @param openEnvStack open env stack
		 * @param closeEnvStack close env stack
		 * @param rowNr rowNr
		 * @param row row
		 */
		private static void parseEnvs(LinkedList<WordWithPos> openEnvStack, LinkedList<String> closeEnvStack, int rowNr, String row) {
			LinkedList<String> tmpCloseEnvStack = null;

			Matcher matcher = envPattern.matcher(row);
			while (matcher.find()) {
				if(matcher.group(1).equals("begin")) {
					if (closeEnvStack.isEmpty()) {
						openEnvStack.push(new CursorElement(matcher.group(2), rowNr, matcher.start(2)));
					} else {
						closeEnvStack.pop();
					}
				} else
				if(matcher.group(1).equals("end")) {
					if (!openEnvStack.isEmpty()) {
						if (openEnvStack.peek().word.equals(matcher.group(2))) {
							openEnvStack.pop();
						}
					} else {
						if (tmpCloseEnvStack == null) {
							tmpCloseEnvStack = new LinkedList<String>();
						}
						tmpCloseEnvStack.push(matcher.group(2));
					}
				}
			}

			if (tmpCloseEnvStack != null) {
				while (!tmpCloseEnvStack.isEmpty()) {
					String elem = tmpCloseEnvStack.pop();
					closeEnvStack.add(elem);
				}
			}
		}
	}
}

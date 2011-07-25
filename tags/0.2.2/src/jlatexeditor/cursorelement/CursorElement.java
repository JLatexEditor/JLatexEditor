package jlatexeditor.cursorelement;

import sce.codehelper.WordWithPos;

/**
 * Element under cursor.
 *
 * @author Stefan Endrullis &lt;stefan@endrullis.de&gt;
 */
public class CursorElement extends WordWithPos {
	public CursorElement(String word, int row, int startColumn) {
		super(word, row, startColumn);
	}
}

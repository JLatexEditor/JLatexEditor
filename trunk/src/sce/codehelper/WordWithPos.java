package sce.codehelper;

import sce.component.SCERange;

/**
 * Word and its position in the document.
 *
* @author Stefan Endrullis
*/
public class WordWithPos implements SCERange {
	public String word;
	public int row;
	public int startColumn;
	public int endColumn;

	public WordWithPos(String word, int row, int startColumn) {
		this.word = word;
		this.row = row;
		this.startColumn = startColumn;
		this.endColumn = startColumn + (word == null ? 0 : word.length());
	}

	@Override
	public int getStartRow() {
		return row;
	}

	@Override
	public int getStartCol() {
		return startColumn;
	}

	@Override
	public int getEndRow() {
		return row;
	}

	@Override
	public int getEndCol() {
		return endColumn;
	}
}

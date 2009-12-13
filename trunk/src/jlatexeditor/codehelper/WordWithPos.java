package jlatexeditor.codehelper;

/**
 * Word and its position in the document.
 *
* @author Stefan Endrullis
*/
public class WordWithPos {
	String word;
	int row;
	int startColumn;
	int endColumn;

	WordWithPos(String word, int row, int startColumn) {
		this.word = word;
		this.row = row;
		this.startColumn = startColumn;
		this.endColumn = startColumn + word.length();
	}
}

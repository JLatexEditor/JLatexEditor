package jlatexeditor.cursorelement;

import java.io.File;

/**
 * @author Stefan Endrullis &lt;stefan@endrullis.de&gt;
 */
public class FileElement extends CursorElement {
	private File file;

	public FileElement(String word, int row, int startColumn, File file) {
		super(word, row, startColumn);
		this.file = file;
	}
}

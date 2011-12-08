package jlatexeditor.codehelper;

/**
 * @author Stefan Endrullis &lt;stefan@endrullis.de&gt;
 */
public class DocumentClass extends BackgroundParser.FilePos {
	private String[] options;

	public DocumentClass(String name, String file, int lineNr, String[] options) {
		super(name, file, lineNr);
		this.options = options;
	}
}

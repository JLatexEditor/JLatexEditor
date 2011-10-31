package jlatexeditor.codehelper;

import java.util.ArrayList;

/**
 * @author Stefan Endrullis &lt;stefan@endrullis.de&gt;
 */
public class Package extends BackgroundParser.FilePos {
	private String[] options;

	public Package(String name, String file, int lineNr, String[] options) {
		super(name, file, lineNr);
		this.options = options;
	}
}

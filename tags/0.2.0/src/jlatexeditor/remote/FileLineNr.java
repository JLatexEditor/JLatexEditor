package jlatexeditor.remote;

import java.io.File;

/**
 * @author Stefan Endrullis &lt;stefan@endrullis.de&gt;
 */
public class FileLineNr {
	public File file;
	public int lineNr = -1;

	public FileLineNr (String fileString) {
		int colonIndex = fileString.lastIndexOf(':');
		if (colonIndex == -1) {
			file = new File(fileString);
		} else {
			file = new File(fileString.substring(0, colonIndex));
			try {
				lineNr = Integer.parseInt(fileString.substring(colonIndex + 1));
			} catch (NumberFormatException ignored) {}
		}
	}
}

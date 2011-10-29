package jlatexeditor.quickhelp;

import util.ProcessUtil;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLStreamHandler;

/**
 * @author Stefan Endrullis &lt;stefan@endrullis.de&gt;
 */
public class TexDocHandler extends URLStreamHandler {
	@Override
	protected URLConnection openConnection(URL u) throws IOException {
		String pack = u.toExternalForm().substring(7);
		try {
			ProcessUtil.exec(new String[]{"texdoc", "--view", pack}, new File("."));
		} catch (IOException e) {
			e.printStackTrace();
		}
		return null;
	}
}

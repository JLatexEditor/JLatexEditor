package jlatexeditor.quickhelp;

import de.endrullis.utils.CollectionUtils;
import jlatexeditor.PackagesExtractor;
import sun.net.www.protocol.file.FileURLConnection;
import util.ConfigurableStreamHandlerFactory;
import util.StreamUtils;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLStreamHandler;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;

/**
 * @author Stefan Endrullis &lt;stefan@endrullis.de&gt;
 */
public class HelpUrlHandler extends URLStreamHandler {
	protected URLConnection openConnection(final URL u) throws IOException {
		String command = null;

		String realUrlString = u.toExternalForm().substring(5);
		if (realUrlString.contains("#")) {
			int index = realUrlString.indexOf("#");
			command = realUrlString.substring(index + 1);

			realUrlString = realUrlString.substring(0, index);
		}
		final URL realUrl = new URL(realUrlString);
		final String finalCommand = command;

		return new URLConnection(realUrl) {
			private URLConnection realConnection = realUrl.openConnection();

			@Override
			public void connect() throws IOException {
				realConnection.connect();
			}

			@Override
			public String getContentType() {
				return realConnection.getContentType();
			}

			@Override
			public InputStream getInputStream() throws IOException {
				String content = HelpUrlHandler.getHelpTextAt(finalCommand, realConnection);

				return new ByteArrayInputStream(content.getBytes());
			}
		};
	}

	public static String getHelpTextAt(String command, URLConnection urlConnection) {
		String content = "content-type: text/html\n\n";
		content += "<html><body>";

		if (urlConnection != null) {
			try {
				BufferedInputStream in = (BufferedInputStream) urlConnection.getContent();
				content = readContent(in);
			} catch (IOException e) {
				e.printStackTrace();
				return null;
			}
		}

		// append packages providing the command
		if (command != null) {
			String commandsPack = getPackagesString(PackagesExtractor.getPackageParser().getCommands().get(command.substring(1)));
			String commandsDoc = getPackagesString(PackagesExtractor.getDocClassesParser().getCommands().get(command.substring(1)));
			if (commandsPack != null || commandsDoc != null) {
				content += "<hr/>";
				content += "<h3>Packages providing this command</h3>";
				content += "<ul>";
				if (commandsPack != null) {
					content += "<li><b>package(s)</b>: " + commandsPack + "</li>";
				}
				if (commandsDoc != null) {
					content += "<li><b>documentclass(es)</b>: " + commandsDoc + "</li>";
				}
				content += "</ul>";
			}
		}

		content += "</body></html>";

	  return content;
	}

	private static String getPackagesString(HashSet<PackagesExtractor.Command> commands) {
		if (commands == null || commands.isEmpty()) {
			return null;
		} else {
			ArrayList<String> packs = new ArrayList<String>();
			for (PackagesExtractor.Command cmd : commands) {
				String hintString = "";
				if (cmd.getPack().getDescription() != null) {
					hintString = " - " + cmd.getPack().getDescription();
				}
				packs.add("<li>" + cmd.getPack().getName() + hintString + "</li>");
			}
			Collections.sort(packs);
			return "<ul>" + CollectionUtils.join(packs, "") + "</ul>";
		}
	}

	private static String readContent(BufferedInputStream in) throws IOException {
		StringBuilder sb = new StringBuilder();
		BufferedReader r = new BufferedReader(new InputStreamReader(in));
		String line;
		while ((line = r.readLine()) != null) {
			if (line.equals("</body>")) break;
			sb.append(line).append("\n");
		}
		return sb.toString();
	}

	public static void register() {
		ConfigurableStreamHandlerFactory.register("help", new HelpUrlHandler());
	}

	public static void main(String[] args) {
		HelpUrlHandler.register();

		try {
			System.out.println(new URL("help:file:/home/stefan/programmierung/java/JLatexEditor/data/quickhelp/index.html").getContent());
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}

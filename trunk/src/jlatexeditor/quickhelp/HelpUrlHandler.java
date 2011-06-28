package jlatexeditor.quickhelp;

import de.endrullis.utils.CollectionUtils;
import jlatexeditor.PackagesExtractor;
import util.ConfigurableStreamHandlerFactory;
import util.StreamUtils;

import java.io.*;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLStreamHandler;
import java.util.ArrayList;
import java.util.HashSet;

/**
 * @author Stefan Endrullis &lt;stefan@endrullis.de&gt;
 */
public class HelpUrlHandler extends URLStreamHandler {
	protected URLConnection openConnection(final URL u) throws IOException {
		System.out.println("yeah help");

		return new URLConnection(u) {
			@Override
			public void connect() throws IOException {
			}

			@Override
			public Object getContent() throws IOException {
				/*
				InputStream in = (InputStream) realUrl.getContent();
				String content = new String(StreamUtils.readBytesFromInputStream(in));
				content = content.replace("href=\"", "href=\"help:");

				ByteArrayOutputStream out = new ByteArrayOutputStream();
				*/

				String command = null;

				String realUrlString = u.toExternalForm().substring(5);
				if (realUrlString.contains("#")) {
					int index = realUrlString.indexOf("#");
					command = realUrlString.substring(index);

					//realUrlString = realUrlString.substring()
				}
				URL realUrl = new URL(realUrlString);

				//String file = url.getPath().indexOf("#");

				String content = HelpUrlHandler.getHelpTextAt(command, realUrl);

				return new ByteArrayInputStream(content.getBytes());
			}
		};
	}

	public static String getHelpTextAt(String command, URL url) {
		String content = "<html><body>";

		if (url != null) {
			try {
				BufferedInputStream in = (BufferedInputStream) url.getContent();
				content = readContent(in);
			} catch (IOException e) {
				e.printStackTrace();
				return null;
			}
		}

		// append packages providing the command
		String commandsPack = getPackagesString(PackagesExtractor.getPackageParser().getCommands().get(command.substring(1)));
		String commandsDoc = getPackagesString(PackagesExtractor.getDocClassesParser().getCommands().get(command.substring(1)));
		if (commandsPack != null || commandsDoc != null) {
			content += "<h3>This command is provided by ...</h3>";
			content += "<ul>";
			if (commandsPack != null) {
			  content += "<li><b>package(s)</b>: " + commandsPack + "</li>";
			}
			if (commandsDoc != null) {
			  content += "<li><b>documentclass(es)</b>: " + commandsDoc + "</li>";
			}
			content += "</ul>";
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
				packs.add(cmd.getPack().getName());
			}
			return CollectionUtils.join(packs, ", ");
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
			System.out.println(new URL("help:file:data/quickhelp/index.html").getContent());
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}

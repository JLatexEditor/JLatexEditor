package jlatexeditor.remote;

import jlatexeditor.Doc;
import jlatexeditor.JLatexEditorJFrame;

import java.io.*;
import java.net.InetAddress;
import java.net.Socket;
import java.util.logging.Logger;

/**
 * Slave node.
 *
 * @author Stefan Endrullis &lt;stefan@endrullis.de&gt;
 */
public class SlaveNode {
	/** Logger. */
	private static Logger logger = Logger.getLogger(SlaveNode.class.getName());

	private JLatexEditorJFrame jle;
	private Socket socket;
	private BufferedWriter writer;
	private BufferedReader reader;

	public SlaveNode (JLatexEditorJFrame jle, int port) throws IOException {
		this.jle = jle;
		this.socket = new Socket(InetAddress.getLocalHost(), port);
		reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
		writer = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));

		writer.write("register\n");
		writer.flush();

		run();
	}

	public void run() throws IOException {
		while (true) {
			String line = reader.readLine();
			logger.info("reading: " + line);
			if (line == null) throw new IOException("InputStream closed");

			if (line.startsWith("is open?: ")) {
				FileLineNr fileLineNr = new FileLineNr(line.substring("is open?: ".length()));
				File file = fileLineNr.file;

				if (jle.isOpen(file)) {
					writer.write("true\n");
				} else {
					writer.write("false\n");
				}
				writer.flush();
			} else
			if (line.startsWith("is responsible for?: ")) {
				FileLineNr fileLineNr = new FileLineNr(line.substring("is responsible for?: ".length()));
				File file = fileLineNr.file;

				if (jle.isResponsibleFor(file)) {
					writer.write("true\n");
				} else {
					writer.write("false\n");
				}
				writer.flush();
			} else
			if (line.startsWith("open: ")) {
				FileLineNr fileLineNr = new FileLineNr(line.substring("open: ".length()));
				File file = fileLineNr.file;
				int lineNr = fileLineNr.lineNr - 1;

				if (file.exists() && file.isFile()) {
					jle.open(new Doc.FileDoc(file), lineNr);
					jle.requestFocus();
				}
			}
		}
	}
}

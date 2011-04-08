package jlatexeditor.remote;

import jlatexeditor.Doc;
import jlatexeditor.JLatexEditorJFrame;

import javax.swing.*;
import java.io.File;
import java.io.IOException;
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
	private SocketConnection conn;

	public SlaveNode (JLatexEditorJFrame jle, int port) throws IOException {
		this.jle = jle;
		Socket socket = new Socket(InetAddress.getLocalHost(), port);
		conn = new SocketConnection(socket);
		conn.send("register");

		run();
	}

	public void run() throws IOException {
		while (true) {
			String line = conn.receive();
			logger.fine("reading: " + line);
			if (line == null) throw new IOException("InputStream closed");

			if (line.startsWith("is open?: ")) {
				FileLineNr fileLineNr = new FileLineNr(line.substring("is open?: ".length()));
				File file = fileLineNr.file;

				conn.send(jle.isOpen(file) ? "true" : "false");
			} else
			if (line.startsWith("is responsible for?: ")) {
				FileLineNr fileLineNr = new FileLineNr(line.substring("is responsible for?: ".length()));
				File file = fileLineNr.file;

				conn.send(jle.isResponsibleFor(file) ? "true" : "false");
			} else
			if (line.startsWith("open: ")) {
				FileLineNr fileLineNr = new FileLineNr(line.substring("open: ".length()));
				File file = fileLineNr.file;
				int lineNr = fileLineNr.lineNr - 1;

				if (file.exists() && file.isFile()) {
					open(file, lineNr);
				}
			}
		}
	}

  private void open(final File file, final int lineNr) {
    SwingUtilities.invokeLater(new Runnable() {
      public void run() {
        jle.open(new Doc.FileDoc(file), lineNr);
        jle.bringToFront();
      }
    });
  }
}

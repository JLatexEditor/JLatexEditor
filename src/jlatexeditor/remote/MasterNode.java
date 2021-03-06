package jlatexeditor.remote;

import jlatexeditor.Doc;
import jlatexeditor.JLatexEditorJFrame;

import javax.swing.*;
import java.io.File;
import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.logging.Logger;

/**
 * @author Stefan Endrullis &lt;stefan@endrullis.de&gt;
 */
public class MasterNode {
	/** Logger. */
	private static Logger logger = Logger.getLogger(MasterNode.class.getName());

	private ServerSocket serverSocket;
	private JLatexEditorJFrame jle;
	private ArrayList<RemoteSlave> slaves = new ArrayList<RemoteSlave>();

	public MasterNode (JLatexEditorJFrame jle, int port) throws IOException {
		this.jle = jle;
		serverSocket = new ServerSocket(port, 0, InetAddress.getByName(null));

		run();
	}

	public void run() throws IOException {
		while (!Thread.interrupted()) {
			//serverSocket.bind(InetAddress.getLocalHost());
			Socket socket = serverSocket.accept();

			SocketConnection conn = new SocketConnection(socket);
			String line = conn.receive();
			logger.fine("reading: " + line);

			if (line == null) {
				conn.close();
				continue;
			}

			if (line.equals("register")) {
				// register client as slave node
				slaves.add(new RemoteSlave(conn));
			} else {
				if (line.startsWith("open: ")) {
					String fileString = line.substring("open: ".length());

					FileLineNr fileLineNr = new FileLineNr(fileString);
					File file = fileLineNr.file;
					int lineNr = fileLineNr.lineNr - 1;

					if (file.exists() && file.isFile()) {
						// check if master has opened this file
						if (jle.isOpen(file)) {
              open(file, lineNr);
						} else {
							// check if a slave has opened the file
							boolean found = false;

							for (RemoteSlave slave : slaves) {
								if (slave.isOpen(fileString)) {
									slave.open(fileString);
									found = true;
									break;
								}
							}

							if (!found) {
								// check if master is responsible for the file
								if (jle.isResponsibleFor(file)) {
                  open(file, lineNr);
								} else {
									// check if a slave has opened the file
									found = false;

									for (RemoteSlave slave : slaves) {
										if (slave.isResponsibleForFile(fileString)) {
											slave.open(fileString);
											found = true;
											break;
										}
									}

									// if no one want to be responsible the master editor has to open the file
									if (!found) open(file, lineNr);
								}
							}
						}
						// jle.actionPerformed(new ActionEvent(this, 0, line));
					}
				}

				conn.close();
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

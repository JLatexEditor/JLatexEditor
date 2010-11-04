package jlatexeditor.remote;

import jlatexeditor.Doc;
import jlatexeditor.JLatexEditorJFrame;
import jlatexeditor.gproperties.GProperties;

import java.awt.event.ActionEvent;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketAddress;
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
		serverSocket = new ServerSocket(port); //, 0, InetAddress.getLocalHost());

		run();
	}

	public void run() throws IOException {
		while (true) {
			//serverSocket.bind(InetAddress.getLocalHost());
			Socket socket = serverSocket.accept();

			BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
			String line = reader.readLine();
			logger.info("reading: " + line);

			if (line == null) {
				reader.close();
				socket.close();
				continue;
			}

			if (line.equals("register")) {
				// client as slave node
				slaves.add(new RemoteSlave(socket, reader));
			} else {
				if (line.startsWith("open: ")) {
					String fileString = line.substring("open: ".length());

					FileLineNr fileLineNr = new FileLineNr(fileString);
					File file = fileLineNr.file;
					int lineNr = fileLineNr.lineNr - 1;

					if (file.exists() && file.isFile()) {
						// check if master has opened this file
						if (jle.isOpen(file)) {
							jle.open(new Doc.FileDoc(file), lineNr);
							jle.requestFocus();
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
									jle.open(new Doc.FileDoc(file), lineNr);
									jle.requestFocus();
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
									if (!found) {
										jle.open(new Doc.FileDoc(file), lineNr);
										jle.requestFocus();
									}
								}
							}
						}
						// jle.actionPerformed(new ActionEvent(this, 0, line));
					}
				}

				reader.close();
				socket.close();
			}
		}
	}
}

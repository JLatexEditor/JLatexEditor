package jlatexeditor.remote;

import java.io.*;
import java.net.Socket;

/**
 * Remove slave node.
 *
 * @author Stefan Endrullis &lt;stefan@endrullis.de&gt;
 */
public class RemoteSlave {
	private Socket socket;
	private BufferedWriter writer;
	private BufferedReader reader;

	public RemoteSlave (Socket socket, BufferedReader reader) throws IOException {
		this.socket = socket;
		this.reader = reader;
		this.writer = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));

		writer.write("registered\n");
		writer.flush();
	}

	public boolean isOpen(String fileString) throws IOException {
		writer.write("is open?: " + fileString + "\n");
		writer.flush();
		return reader.readLine().equals("true");
	}

	public boolean isResponsibleForFile(String fileString) throws IOException {
		writer.write("is responsible for?: " + fileString + "\n");
		writer.flush();
		return reader.readLine().equals("true");
	}

	public void open(String fileString) throws IOException {
		writer.write("open: " + fileString + "\n");
		writer.flush();
	}
}

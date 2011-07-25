package jlatexeditor.remote;

import java.io.IOException;

/**
 * Remove slave node.
 *
 * @author Stefan Endrullis &lt;stefan@endrullis.de&gt;
 */
public class RemoteSlave {
	private SocketConnection conn;

	public RemoteSlave (SocketConnection conn) throws IOException {
		this.conn = conn;

		conn.send("registered");
	}

	public boolean isOpen(String fileString) throws IOException {
		conn.send("is open?: " + fileString);
		return conn.receive().equals("true");
	}

	public boolean isResponsibleForFile(String fileString) throws IOException {
		conn.send("is responsible for?: " + fileString);
		return conn.receive().equals("true");
	}

	public void open(String fileString) throws IOException {
		conn.send("open: " + fileString);
	}
}

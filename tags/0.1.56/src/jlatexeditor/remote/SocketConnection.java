package jlatexeditor.remote;

import java.io.*;
import java.net.Socket;

/**
 * Simple utility to manage socket connections.
 *
 * @author Stefan Endrullis &lt;stefan@endrullis.de&gt;
 */
public class SocketConnection implements Closeable {
	private Socket socket;
	private BufferedWriter writer;
	private BufferedReader reader;

	public SocketConnection(Socket socket) throws IOException {
		this.socket = socket;
		reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
		writer = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));
	}

	/**
	 * Receives a message (one line) over the TCP connection.
	 *
	 * @return message received over the TCP connection
	 * @throws java.io.IOException if an I/O error occurs
	 */
	public String receive() throws IOException {
		return reader.readLine();
	}

	/**
	 * Sends a message (one line) over the TCP connection.
	 *
	 * @param message message sent over the TCP connection
	 * @throws IOException if an I/O error occurs
	 */
	public void send(String message) throws IOException {
		writer.write(message + "\n");
		writer.flush();
	}

	public void close() throws IOException {
		reader.close();
		writer.close();
		socket.close();
	}
}

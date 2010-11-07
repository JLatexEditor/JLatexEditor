package jlatexeditor.remote;

import jlatexeditor.JLatexEditorJFrame;
import jlatexeditor.gproperties.GProperties;

import java.io.IOException;
import java.net.BindException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Network interface.
 *
 * @author Stefan Endrullis &lt;stefan@endrullis.de&gt;
 */
public class NetworkNode extends Thread {
	/** Logger. */
	private static Logger logger = Logger.getLogger(NetworkNode.class.getName());

	public static final int PORT = GProperties.getInt("inverse search.port");

	private JLatexEditorJFrame jle;

	public NetworkNode(JLatexEditorJFrame jle) {
		super("NetworkNode");
		setDaemon(true);
		this.jle = jle;
	}

	@Override
	public void run() {
		int tries = 0;
		while (!isInterrupted() && tries < 20) {
			// first try to start master node
			try {
				logger.fine("Running in master mode...");
				new MasterNode(jle, PORT);
			} catch (BindException e) {
				logger.fine("Master port in use");
			} catch (IOException e) {
				logger.log(Level.FINE, "Connection failure in master node", e);
			}

			// if master node fails try to start slave node
			try {
				logger.fine("Running in slave mode...");
				new SlaveNode(jle, PORT);
			} catch (IOException e) {
				logger.log(Level.FINE, "Connection to master node failed", e);
			}

			logger.fine("Restarting network mode");

			try {
				Thread.sleep(tries*500);
			} catch (InterruptedException e) {
				break;
			}

			tries++;
		}
	}
}

package util;

import com.sun.org.apache.bcel.internal.generic.INSTANCEOF;
import jlatexeditor.quickhelp.HelpUrlHandler;

import java.net.URL;
import java.net.URLStreamHandler;
import java.net.URLStreamHandlerFactory;
import java.util.HashMap;
import java.util.Map;

/**
 * @author Stefan Endrullis &lt;stefan@endrullis.de&gt;
 */
public class ConfigurableStreamHandlerFactory implements URLStreamHandlerFactory {
	private static ConfigurableStreamHandlerFactory INST = new ConfigurableStreamHandlerFactory();

	static {
		URL.setURLStreamHandlerFactory(INST);
	}

	public static void register(String protocol, URLStreamHandler urlStreamHandler) {
		INST.addHandler(protocol, urlStreamHandler);
	}

	private final Map<String, URLStreamHandler> protocolHandlers = new HashMap<String, URLStreamHandler>();

	public void addHandler(String protocol, URLStreamHandler urlHandler) {
		protocolHandlers.put(protocol, urlHandler);
	}

	public URLStreamHandler createURLStreamHandler(String protocol) {
		return protocolHandlers.get(protocol);
	}
}

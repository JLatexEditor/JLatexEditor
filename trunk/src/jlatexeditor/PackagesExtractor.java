package jlatexeditor;

import com.sun.deploy.panel.ITreeNode;
import my.XML.XMLDocument;
import my.XML.XMLParser;
import org.xml.sax.*;
import org.xml.sax.helpers.DefaultHandler;
import org.xml.sax.helpers.XMLReaderFactory;
import util.StreamUtils;

import javax.xml.parsers.SAXParserFactory;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;

/**
 * @author Stefan Endrullis &lt;stefan@endrullis.de&gt;
 */
public class PackagesExtractor {
	private static final String FILENAME = "data/codehelper/packages.xml";

	private static HashMap<String, Command> commands = new HashMap<String, Command>();

	public static void main(String[] args) {
		XMLParser xmlParser = new XMLParser();
		XMLDocument commandsDocument;
		try {
			long startTime = System.nanoTime();
			//commandsDocument = xmlParser.parse(StreamUtils.readFile(FILENAME));
			System.out.println((System.nanoTime() - startTime) / (1000 * 1000));
		} catch (Exception e) {
			e.printStackTrace();
			return;
		}

		//This class may be any SAX parser, but the classpath
		// must be modified to include it.

		//SAXParserFactory.newInstance().newSAXParser();
		try {
			XMLReader parser = XMLReaderFactory.createXMLReader();
			DefaultHandler handler = new DebugHandler();
			parser.setContentHandler(handler);
			parser.setErrorHandler(handler);
			try {
				parser.parse(new InputSource(StreamUtils.getInputStream(FILENAME)));
			} catch (IOException ioe) {
				ioe.printStackTrace();
			}
		} catch (SAXException se) {
			se.printStackTrace();
		}
	}

	/**
	 * Test Handler that prints debug statements
	 */
	public static class DebugHandler extends org.xml.sax.helpers.DefaultHandler implements org.xml.sax.ContentHandler {
		public DebugHandler() {
			super();
		}

		/*
		public void characters(char[] chars, int start, int length) {
			String string = new String(chars, start, length);
			System.out.println("chars=" + string + "!");
		}
		*/

		public void startDocument() {
			System.out.println("start document");
		}

		public void endDocument() {
			System.out.println("end document");
		}

		public void startElement(String namespaceURI, String localName, String qname, Attributes attrList) {
			System.out.println("start: " + localName);
			if (localName.equals("command")) {
				String name = attrList.getValue("name");
				int argCount = Integer.parseInt(attrList.getValue("argCount"));
				Command command = new Command(name, argCount, null, null, null);
				commands.put(name, command);
			}
		}

		public void endElement(String namespaceURI, String localName, String qName) {
			System.out.println("end: " + localName);
		}

		@Override
		public void error(SAXParseException e) throws SAXException {
			System.out.println(e.getLineNumber());
			super.error(e);
		}

		@Override
		public void fatalError(SAXParseException e) throws SAXException {
			System.out.println(e.getLineNumber());
			super.fatalError(e);
		}
	}

	public static class Command {
		private String name;
		private int argCount;
		private String optionalArg = null;
		private String pack;
		private String debPack;

		public Command(String name, int argCount, String optionalArg, String pack, String debPack) {
			this.name = name;
			this.argCount = argCount;
			this.optionalArg = optionalArg;
			this.pack = pack;
			this.debPack = debPack;
		}
	}
}

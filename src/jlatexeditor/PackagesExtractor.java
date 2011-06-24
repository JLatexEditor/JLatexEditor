package jlatexeditor;

import my.XML.XMLDocument;
import my.XML.XMLParser;
import org.xml.sax.*;
import org.xml.sax.helpers.DefaultHandler;
import org.xml.sax.helpers.XMLReaderFactory;
import util.StreamUtils;
import util.TrieSet;

import javax.xml.parsers.SAXParserFactory;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;

/**
 * @author Stefan Endrullis &lt;stefan@endrullis.de&gt;
 */
public class PackagesExtractor {
	private static final String FILENAME = "data/codehelper/packages.xml";

	public static void main(String[] args) {
		XMLParser xmlParser = new XMLParser();
		XMLDocument commandsDocument;
		try {
			long startTime = System.nanoTime();

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

			System.out.println((System.nanoTime() - startTime) / (1000 * 1000));
		} catch (Exception e) {
			e.printStackTrace();
			return;
		}
	}

	/**
	 * Test Handler that prints debug statements
	 */
	public static class DebugHandler extends org.xml.sax.helpers.DefaultHandler implements org.xml.sax.ContentHandler {
		private String pack;
		private String debPack;
		private TrieSet<Command> commands = new TrieSet<Command>();

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
			//System.out.println("start document");
		}

		public void endDocument() {
			//System.out.println("end document");
		}

		public void startElement(String namespaceURI, String localName, String qname, Attributes attrList) {
			//System.out.println("start: " + localName);
			if (localName.equals("command")) {
				String name = attrList.getValue("name");
				int argCount = Integer.parseInt(attrList.getValue("argCount"));
				String optionalArg = attrList.getValue("optionalArg");
				Command command = new Command(name, argCount, optionalArg, pack, debPack);
				commands.add(name, command);
			} else
			if (localName.equals("package")) {
				pack = attrList.getValue("name");
				debPack = attrList.getValue("debPackage");
			}
		}

		public void endElement(String namespaceURI, String localName, String qName) {
			//System.out.println("end: " + localName);
		}

		@Override
		public void error(SAXParseException e) throws SAXException {
			//System.out.println(e.getLineNumber());
			super.error(e);
		}

		@Override
		public void fatalError(SAXParseException e) throws SAXException {
			//System.out.println(e.getLineNumber());
			super.fatalError(e);
		}
	}

	public static class Command {
		private String name;
		private int argCount;
		private String optionalArg;
		private String pack;
		private String debPack;

		public Command(String name, int argCount, String optionalArg, String pack, String debPack) {
			this.name = name;
			this.argCount = argCount;
			this.optionalArg = optionalArg;
			this.pack = pack;
			this.debPack = debPack;
		}

		@Override
		public int hashCode() {
			return name.hashCode() + 37*pack.hashCode();
		}

		@Override
		public boolean equals(Object obj) {
			if (obj instanceof Command) {
				Command that = (Command) obj;
				return this.pack.equals(that.pack) && this.name.equals(that.name);
			}
			return false;
		}
	}
}

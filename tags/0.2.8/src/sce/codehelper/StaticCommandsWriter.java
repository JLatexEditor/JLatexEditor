package sce.codehelper;

import my.XML.XMLDocument;
import my.XML.XMLElement;
import util.AbstractSimpleTrie;
import util.SystemUtils;
import util.Trie;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintStream;

/**
 * Writes a Trie of commands to a file.
 *
 * @author Stefan Endrullis &lt;stefan@endrullis.de&gt;
 */
public class StaticCommandsWriter {
	public static void writeToFile(File file, CommandsReader commandsReader) throws FileNotFoundException {
		AbstractSimpleTrie<CHCommand> commands = commandsReader.getCommands();

		XMLElement xml = new XMLElement("commandList");
		for (CHCommand command : commands.getObjectsIterable()) {
			xml.addChildElement(getXml(command));
		}

		XMLDocument document = new XMLDocument();
		document.setRootElement(xml);

		PrintStream out = new PrintStream(file);
		out.println(document.toString());
		out.flush();
		out.close();
	}

	private static XMLElement getXml(CHCommand command) {
		XMLElement xml = new XMLElement("command");
		xml.setAttribute("name", encode(command.getName()));
		xml.setAttribute("usage", encode(command.getUsage()));
		setAttribute(xml, "style", command.getStyle());
		setAttribute(xml, "hint", encode(command.getHint()));
		xml.setAttribute("enabled", "" + command.isEnabled());

		for (CHCommandArgument argument : command.getArguments()) {
			xml.addChildElement(getXml(argument));
		}

		return xml;
	}

	private static XMLElement getXml(CHCommandArgument argument) {
		XMLElement xml = new XMLElement("argument");
		xml.setAttribute("name", encode(argument.getName()));
		xml.setAttribute("value", encode(argument.getInitialValue()));
		xml.setAttribute("completion", "" + argument.isCompletion());
		if (argument.getType() != null) {
			xml.setAttribute("type", argument.getType().toString());
		}
		setAttribute(xml, "hint", encode(argument.getHint()));
		if (argument.getValues().size() > 0) {
			StringBuilder sb = new StringBuilder(argument.getValues().get(0));
			for (int i = 1; i < argument.getValues().size(); i++) {
				String value = argument.getValues().get(i);
				sb.append("|").append(value);
			}
			xml.setAttribute("values", sb.toString());
		}
		for (CHArgumentGenerator generator : argument.getGenerators()) {
			xml.addChildElement(getXml(generator));
		}
		return xml;
	}

	private static XMLElement getXml(CHArgumentGenerator generator) {
		XMLElement xml = new XMLElement("generate");
		xml.setAttribute("name", encode(generator.getArgument().getName()));
		xml.setAttribute("function", generator.getFunctionName());
		return xml;
	}

	private static void setAttribute(XMLElement xml, String attribute, String value) {
		if (value != null) {
			xml.setAttribute(attribute, value);
		}
	}

	private static String encode(String text) {
		if (text == null) return null;
		return text.replaceAll("&", "&amp;").replaceAll("<", "&lt;").replaceAll(">", "&gt;").replaceAll("\n", "&nl;").replaceAll("\"", "&quot;");
	}
}

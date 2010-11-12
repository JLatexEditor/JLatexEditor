package sce.codehelper;

import my.XML.XMLDocument;
import my.XML.XMLElement;
import my.XML.XMLParser;
import util.StreamUtils;
import util.Trie;

import java.util.ArrayList;
import java.util.HashMap;

/**
 * @author Stefan Endrullis &lt;stefan@endrullis.de&gt;
 */
public class StaticCommandsReader {
	/** The command reference. */
	protected Trie<CHCommand> commands = null;
	/** Maps an environment name to a list of commands. */
	protected HashMap<String,ArrayList<CHCommand>> environments = new HashMap<String, ArrayList<CHCommand>>();

	public StaticCommandsReader(String filename) {
		readCommands(filename);
	}

	/**
	 * Reads the commands from xml file.
	 *
	 * @param filename the filename
	 */
	public void readCommands(String filename) {
	  commands = new Trie<CHCommand>();

	  XMLParser xmlParser = new XMLParser();
	  XMLDocument commandsDocument;
	  try {
	    commandsDocument = xmlParser.parse(StreamUtils.readFile(filename));
	  } catch (Exception e) {
	    e.printStackTrace();
	    return;
	  }

	  XMLElement commandListXML = commandsDocument.getRootElement();
	  if (!commandListXML.getName().equalsIgnoreCase("commandList")) {
	    System.out.println("Error in commands.xml: root element must be 'commandList' instead of element '" + commandListXML.getName() + "'");
	    return;
	  }

	  // extract commands
	  for (XMLElement commandXML : commandListXML.getChildElements()) {

	    if (commandXML.getName().equals("command")) {
		    // extract command form XML element
		    CHCommand command = getCommand(commandXML);
		    if (command == null) continue;

		    // add the command to the commands list
		    commands.add(command.getName(), command);
	    } else

	    if (commandXML.getName().equals("environment")) {
		    String envName = decode(commandXML.getAttribute("name"));
		    if (envName == null) {
		      System.out.println("Error in commands.xml: environment must have a name");
			    continue;
		    }

		    ArrayList<CHCommand> envCommands = new ArrayList<CHCommand>();
		    for (XMLElement element : commandXML.getChildElements()) {
			    // extract command form XML element
			    CHCommand command = getCommand(element);
			    if (command == null) continue;

			    // add the command to the commands list
			    envCommands.add(command);
		    }
		    environments.put(envName, envCommands);
	    } else {
		    System.out.println("Error in commands.xml: expected element 'command' or 'environment' instead of element '" + commandXML.getName() + "'");
	    }
	  }

	  //Collections.sort(commands);
	}

	private CHCommand getCommand(XMLElement commandXML) {
		String commandName = decode(commandXML.getAttribute("name"));
		if (commandName == null) {
			System.out.println("Error in commands.xml: command must have a name");
			return null;
		}

		// create the command and set usage + hint
		CHCommand command = new CHCommand(commandName);
		command.setUsage(decode(commandXML.getAttribute("usage")));
		command.setStyle(decode(commandXML.getAttribute("style")));
		command.setHint(decode(commandXML.getAttribute("hint")));

		// read the arguments
		for (XMLElement argumentXML : commandXML.getChildElements()) {
			if (!argumentXML.getName().equalsIgnoreCase("argument")) {
			  System.out.println("Error in commands.xml: expected element 'argument' - " + argumentXML.getName());
			  continue;
			}
			String argumentName = decode(argumentXML.getAttribute("name"));
			if (argumentName == null) {
			  System.out.println("Error in commands.xml: argument must have a name");
			  continue;
			}
			String argumentValue = decode(argumentXML.getAttribute("value"));
			if (argumentValue == null) {
				argumentValue = argumentName;
			}
			String argumentCompletionString = decode(argumentXML.getAttribute("completion"));
			boolean argumentCompletion = argumentCompletionString != null && argumentCompletionString.equals("true");

			// check if the argument is optional
			boolean optional = false;

			String commandUsage = command.getUsage();
			if (commandUsage == null) {
			  System.out.println("Error in commands.xml: wrong usage declaration");
			  continue;
			}
			int argumentStart = commandUsage.indexOf("@" + argumentName + "@");
			if (argumentStart == -1) {
			  System.out.println("Error in commands.xml: couldn't find argument in usage declaration - " + argumentName);
			  continue;
			}
			int argumentEnd = argumentStart + argumentName.length() + 2;
			if (commandUsage.substring(0, argumentStart).trim().endsWith("[")) optional = true;
			if (commandUsage.substring(argumentEnd).trim().startsWith("]")) optional = true;

			boolean secondOptional = false;
			if (optional) {
				if (commandUsage.substring(0, argumentStart-1).trim().endsWith("]")) {
					secondOptional = true;
					command.setUsage(commandUsage.replaceAll("\\[@" + argumentName + "@\\]", ""));
					continue;
				}
			}

			// create the argument
			CHCommandArgument argument = new CHCommandArgument(argumentName, argumentValue, optional, secondOptional, argumentCompletion);
			argument.setType(decode(argumentXML.getAttribute("type")));
			argument.setHint(decode(argumentXML.getAttribute("hint")));

			String argumentValues = decode(argumentXML.getAttribute("values"));
			if (argumentValues != null) {
				for (String value : argumentValues.split("\\|")) {
					argument.addValue(value);
				}
			}

			// read the suggested values if there are some
			for (XMLElement valueXML : argumentXML.getChildElements()) {
			  argument.addValue(decode(valueXML.getAttribute("value")));
			}

			// add the argument to the command
			command.addArgument(argument);
		}
		return command;
	}

	private String decode(String htmlString) {
		if (htmlString == null) return null;
		return htmlString.replaceAll("&nbsp;", " ").replaceAll("&lt;", "<").replaceAll("&gt;", ">").replaceAll("&nl;", "\n").replaceAll("&quot;", "\"").replaceAll("&amp;", "&");
	}

	public Trie<CHCommand> getCommands() {
		return commands;
	}

	public HashMap<String, ArrayList<CHCommand>> getEnvironments() {
		return environments;
	}
}

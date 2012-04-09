package sce.codehelper;

import my.XML.XMLDocument;
import my.XML.XMLElement;
import my.XML.XMLParser;
import util.StreamUtils;
import util.SimpleTrie;

import java.util.ArrayList;
import java.util.HashMap;

/**
 * Static commands reader.
 *
 * @author Stefan Endrullis &lt;stefan@endrullis.de&gt;
 */
public class StaticCommandsReader implements CommandsReader {
	/** The command reference. */
	protected SimpleTrie<CHCommand> commands = new SimpleTrie<CHCommand>();
	/** The environment reference. */
	protected SimpleTrie<CHCommand> environments = new SimpleTrie<CHCommand>();
	/** Maps an environment name to a list of commands. */
	protected HashMap<String,ArrayList<CHCommand>> scopes = new HashMap<String, ArrayList<CHCommand>>();

	public StaticCommandsReader(String filename) {
		this(filename, false);
	}

	public StaticCommandsReader(String filename, boolean userDefined) {
		readCommands(filename, userDefined, userDefined);
	}

	/**
	 * Reads the commands from xml file.
	 *
	 * @param filename the filename
	 */
	public void readCommands(String filename, boolean userDefined, boolean mayFail) {
	  XMLParser xmlParser = new XMLParser();
	  XMLDocument commandsDocument;
	  try {
	    commandsDocument = xmlParser.parse(StreamUtils.readFile(filename));
	  } catch (Exception e) {
		  if (!mayFail) {
	      e.printStackTrace();
		  }
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
		    CHCommand command = getComEnv(commandXML, false);
		    if (command == null) continue;

        // user defined command?
        command.setUserDefined(userDefined);

		    // add the command to the commands list
		    commands.add(command.getName(), command);
	    } else

	    if (commandXML.getName().equals("environment")) {
		    // extract command form XML element
		    CHCommand env = getComEnv(commandXML, true);
		    if (env == null) continue;

        // user defined command?
        env.setUserDefined(userDefined);

		    // add the command to the commands list
		    environments.add(env.getName(), env);
	    } else

	    if (commandXML.getName().equals("scope")) {
		    String envName = decode(commandXML.getAttribute("name"));
		    if (envName == null) {
		      System.out.println("Error in commands.xml: environment must have a name");
			    continue;
		    }

		    ArrayList<CHCommand> envCommands = new ArrayList<CHCommand>();
		    for (XMLElement element : commandXML.getChildElements()) {
			    // extract command form XML element
			    CHCommand command = getComEnv(element, false);
			    if (command == null) continue;

			    // add the command to the commands list
			    envCommands.add(command);
		    }
		    scopes.put(envName, envCommands);
	    } else {
		    System.out.println("Error in commands.xml: expected element 'command' or 'environment' instead of element '" + commandXML.getName() + "'");
	    }
	  }

	  //Collections.sort(commands);
	}

	private CHCommand getComEnv(XMLElement commandXML, boolean env) {
		String commandName = decode(commandXML.getAttribute("name"));
		if (commandName == null) {
			System.out.println("Error in commands.xml: command must have a name");
			return null;
		}

		// create the command and set usage + hint
		CHCommand command = new CHCommand(commandName);
		String usage = decode(commandXML.getAttribute("usage"));
		command.setUsage(usage);
		if (usage == null) {
			if (!env) {
				command.setUsage("\\" + command);
			} else {
				command.setUsage("\\begin{" + command + "}@|@\n\\end{" + command + "}");
			}
		}
		command.setStyle(decode(commandXML.getAttribute("style")));
		command.setHint(decode(commandXML.getAttribute("hint")));
		if (commandXML.getAttribute("enabled") != null) {
			command.setEnabled(commandXML.getAttribute("enabled").equals("true"));
		}

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
			if (argumentXML.getAttribute("type") != null) {
				argument.setType(new CHArgumentType(decode(argumentXML.getAttribute("type"))));
			}
			argument.setHint(decode(argumentXML.getAttribute("hint")));

			String argumentValues = decode(argumentXML.getAttribute("values"));
			if (argumentValues != null) {
				for (String value : argumentValues.split("\\|")) {
					argument.addValue(value);
				}
			}

			// read the suggested values if there are some
			for (XMLElement xml : argumentXML.getChildElements()) {
				if (xml.getName().equals("value")) {
			    argument.addValue(decode(xml.getAttribute("value")));
				} else
				if (xml.getName().equals("generate")) {
			    argument.addGenerator(decode(xml.getAttribute("name")), decode(xml.getAttribute("function")));
				}
			}

			// add the argument to the command
			command.addArgument(argument);
		}

		command.finalizeArguments();

		return command;
	}

	private String decode(String htmlString) {
		if (htmlString == null) return null;
		return htmlString.replaceAll("&nbsp;", " ").replaceAll("&lt;", "<").replaceAll("&gt;", ">").replaceAll("&nl;", "\n").replaceAll("&quot;", "\"").replaceAll("&amp;", "&");
	}

	public SimpleTrie<CHCommand> getCommands() {
		return commands;
	}

	public SimpleTrie<CHCommand> getEnvironments() {
		return environments;
	}

	public HashMap<String, ArrayList<CHCommand>> getScopes() {
		return scopes;
	}
}

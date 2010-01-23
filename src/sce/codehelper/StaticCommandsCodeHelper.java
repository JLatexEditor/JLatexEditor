package sce.codehelper;

import my.XML.XMLDocument;
import my.XML.XMLElement;
import my.XML.XMLParser;
import util.StreamUtils;

import java.util.ArrayList;
import java.util.Collections;

/**
 * Provides a code completion with static commands (command templates).
 *
 * @author Jörg Endrullis
 * @author Stefan Endrullis
 */
public class StaticCommandsCodeHelper extends CodeHelper {
	// the command reference
	private ArrayList<CHCommand> commands = null;

	/**
	 * Reads the commands from xml file.
	 *
	 * @param filename the filename
	 */
	public void readCommands(String filename){
	  commands = new ArrayList<CHCommand>();

	  XMLParser xmlParser = new XMLParser();
	  XMLDocument commandsDocument;
	  try {
	    commandsDocument = xmlParser.parse(StreamUtils.readFile(filename));
	  } catch (Exception e) {
		  e.printStackTrace();
	    return;
	  }

		XMLElement commandListXML = commandsDocument.getRootElement();
	  if(!commandListXML.getName().equalsIgnoreCase("commandList")){
	    System.out.println("Error in commands.xml: root element must be 'commandList'");
	    return;
	  }

	  // extract commands
		for (XMLElement commandXML: commandListXML.getChildElements()) {
			if (!commandXML.getName().equals("command")) {
				System.out.println("Error in commands.xml: expected element 'command'");
				continue;
			}
			String commandName = commandXML.getAttribute("name");
			if (commandName == null) {
				System.out.println("Error in commands.xml: command must have a name");
				continue;
			}

			// create the command and set usage + hint
			CHCommand command = new CHCommand(commandName);
			String usage = commandXML.getAttribute("usage");
			if (usage != null) usage = usage.replaceAll("&nl;", "\n");
			command.setUsage(usage);
			command.setHint(commandXML.getAttribute("hint"));

			// read the arguments
			for (XMLElement argumentXML : commandXML.getChildElements()) {
				if (!argumentXML.getName().equalsIgnoreCase("argument")) {
					System.out.println("Error in commands.xml: expected element 'argument' - " + argumentXML.getName());
					continue;
				}
				String argumentName = argumentXML.getAttribute("name");
				if (argumentName == null) {
					System.out.println("Error in commands.xml: argument must have a name");
					continue;
				}

				// check if the command is optional
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

				// create the argument
				CHCommandArgument argument = new CHCommandArgument(argumentName, optional);
				argument.setHint(argumentXML.getAttribute("hint"));

				// read the suggested values if there are some
				for (XMLElement valueXML : argumentXML.getChildElements()) {
					argument.addValue(valueXML.getAttribute("value"));
				}

				// add the argument to the command
				command.addArgument(argument);
			}

			// add the command to the commands list
			commands.add(command);
		}

	  Collections.sort(commands);
	}

	public Iterable<CHCommand> getCommands(String prefix) {
    ArrayList<CHCommand> commandsFiltered = new ArrayList<CHCommand>(commands.size());
    for(CHCommand command : commands) {
      if(command.getName().startsWith(prefix)) commandsFiltered.add(command);
    }
		return commandsFiltered;
	}

  /**
   * Searches for the best completion of the prefix.
   *
   * @param prefix the prefix
   * @return the completion suggestion (without the prefix)
   */
  public String getCompletion(String prefix){
    int prefixLength = prefix.length();
    String completion = null;

	  for (CHCommand command : getCommands(prefix)) {
		  String commandName = command.getName();
		  if (commandName.startsWith(prefix)) {
			  if (completion == null) {
				  completion = commandName;
			  } else {
				  // find the common characters
				  int commonIndex = prefixLength;
				  int commonLength = Math.min(completion.length(), commandName.length());
				  while (commonIndex < commonLength) {
					  if (completion.charAt(commonIndex) != commandName.charAt(commonIndex)) break;
					  commonIndex++;
				  }
				  completion = completion.substring(0, commonIndex);
			  }
		  }
	  }

    return completion;
  }
}

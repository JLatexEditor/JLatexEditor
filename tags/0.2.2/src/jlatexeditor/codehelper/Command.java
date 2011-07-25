package jlatexeditor.codehelper;

import sce.codehelper.CHCommand;
import sce.codehelper.CHCommandArgument;
import util.ParseUtil;
import util.Trie;

import java.util.ArrayList;

/**
 * Command defined via \newcommand or \renewcommand.
 */
public class Command extends BackgroundParser.FilePos {
  private String name;
  private int numberOfArgs;
  private String optional;
  private String body;

	public Command(String name, String file, int lineNr, int numberOfArgs, String optional, String body) {
		super(name, file, lineNr);
		this.name = name;
		this.numberOfArgs = numberOfArgs;
		this.optional = optional;
		this.body = body;
	}

	public String getName() {
    return name;
  }

  public int getNumberOfArgs() {
    return numberOfArgs;
  }

  public String getOptional() {
    return optional;
  }

  public String getBody() {
    return body;
  }

  public String toString() {
    return "\\newcommand{\\" + name + "}" +
            (numberOfArgs > 0 ? "[" + numberOfArgs + "]" : "") +
            (optional != null ? "[" + optional + "]" : "") +
            "{" + body + "}";
  }

	public CHCommand toCHCommand() {
		CHCommand chCommand = new CHCommand("<html><body><b>" + name + "</b>");
		String template = "\\" + name;
		if (optional != null) {
			template += "[@opt@]";
			chCommand.addArgument(new CHCommandArgument("opt", optional, true));
		}
		for (int i=1; i<=numberOfArgs; i++) {
			template += "{@arg" + i + "@}";
			chCommand.addArgument(new CHCommandArgument("arg" + i, "", false));
		}
		chCommand.setUsage(template);
		return chCommand;
	}

  /**
   * Unfolding macros once.
   */
  public static String unfoldOnce(String text, Trie<Command> commands) {
    int index = 0;
    while (index < text.length()) {
      char c = text.charAt(index);

      // starting of commands?
      if (c != '\\') { index++; continue; }
      index++;
      if (index >= text.length()) return text;
      if (!Character.isLetter(text.charAt(index))) { index++; continue; }

      // find the end of the command
      int begin = index;
      index++;
      while (index < text.length() && Character.isLetter(text.charAt(index))) index++;

      String commandName = text.substring(begin, index);
      Command command = commands.get(commandName);
      if(command == null) { continue; }

      // parse arguments
      String optional = command.getOptional();
      if(optional != null && text.charAt(index) == '[') {
        try {
          optional = ParseUtil.parseBalanced(text, index+1, ']');
          index += 2 + optional.length();
        } catch(NumberFormatException ignore) {}
      }

      ArrayList<String> arguments = new ArrayList<String>();
      if(optional != null) arguments.add(optional);

      int argsCount = command.getNumberOfArgs() - (optional != null ? 1 : 0);
      for(int argNr = 0; argNr < argsCount; argNr++) {
        String argument = ParseUtil.parseBalanced(text, index+1, '}');
        index += 2 + argument.length();
        arguments.add(argument);
      }
      int end = index;

      // unfold arguments in the body
      StringBuilder builder = new StringBuilder();
      boolean escape = false;
      for(char d : command.getBody().toCharArray()) {
        if (escape) {
          if ('1' <= d && d <= '9') {
            int groupNr = d - '1';
            if (groupNr <= arguments.size()) {
              builder.append(arguments.get(groupNr));
            }
            escape = false;
            continue;
          } else{
            if (d == '#') { builder.append("#"); escape = true; } else builder.append(d);
          }
        } else {
          if (d == '#') escape = true; else builder.append(d);
        }
      }
      String replacement = builder.toString();

      // replace the command
      text = text.substring(0, begin-1) + replacement + text.substring(end);
      index = begin + replacement.length();
    }
    return text;
  }

  /**
   * Unfolding macros recursively.
   */
  public static String unfoldRecursive(String text, Trie<Command> commands, int max) {
    while(max-- > 0) {
      String unfolded = unfoldOnce(text, commands);
      if(unfolded.equals(text)) return unfolded;
      text = unfolded;
    }
    return text;
  }
}

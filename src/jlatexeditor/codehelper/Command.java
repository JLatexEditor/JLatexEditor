package jlatexeditor.codehelper;

import util.Trie;

import java.util.regex.Matcher;

/**
 * Command defined via \newcommand or \renewcommand.
 */
public class Command {
  private String name;
  private int numberOfArgs;
  private String def;
  private String body;

  public Command(String name, int numberOfArgs, String def, String body) {
    this.name = name;
    this.numberOfArgs = numberOfArgs;
    this.def = def;
    this.body = body;
  }

  public String getName() {
    return name;
  }

  public int getNumberOfArgs() {
    return numberOfArgs;
  }

  public String getDef() {
    return def;
  }

  public String getBody() {
    return body;
  }

  public String toString() {
    return "\\newcommand{" + name + "}" +
            (numberOfArgs > 0 ? "[" + numberOfArgs + "]" : "") +
            (def != null ? "[" + def + "]" : "") +
            "{" + body + "}";
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
      if(command != null) {

      }
    }

    return text;
  }
}

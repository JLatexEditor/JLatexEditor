package sce.codehelper;

import jlatexeditor.codehelper.PatternHelper;
import util.AbstractSimpleTrie;
import util.SimpleTrie;

import java.util.ArrayList;
import java.util.HashMap;

/**
 * Provides a code completion with static commands (command templates).
 *
 * @author Jörg Endrullis
 * @author Stefan Endrullis
 */
public class StaticCommandsCodeHelper extends PatternHelper {
  /** Command reference. */
  protected AbstractSimpleTrie<CHCommand> commands = null;
	/** Maps an environment name to a list of commands. */
	//protected HashMap<String,ArrayList<CHCommand>> scopes = new HashMap<String, ArrayList<CHCommand>>();
  /** Last command that has been found. */
  protected WordWithPos command = null;

  public StaticCommandsCodeHelper(String patternString, CommandsReader commandsReader) {
    pattern = new PatternPair(patternString);
	  commands = commandsReader.getCommands();
	  // scopes = commandsReader.getScopes();
  }

	@Override
  public boolean matches() {
    if (super.matches()) {
      command = params.get(0);
      return true;
    }
    return false;
  }

  @Override
  public WordWithPos getWordToReplace() {
    return command;
  }

  @Override
  public Iterable<? extends CHCommand> getCompletions(int level) {
    return getCompletions(command.word);
  }

  @Override
  public String getMaxCommonPrefix() {
    return getMaxCommonPrefix(command.word);
  }

  public Iterable<? extends CHCommand> getCompletions(String prefix) {
	  return commands.getObjects(prefix, 1000);
  }

  /**
   * Searches for the best completion of the prefix.
   *
   * @param prefix the prefix
   * @return the completion suggestion (without the prefix)
   */
  public String getMaxCommonPrefix(String prefix) {
	  String maxCommonPrefix = commands.getMaxCommonPrefix(prefix);
	  if (maxCommonPrefix.length() < prefix.length()) {
		  return null;
	  } else {
		  return maxCommonPrefix;
	  }
  }
}

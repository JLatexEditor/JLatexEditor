package sce.codehelper;

import jlatexeditor.codehelper.PatternHelper;
import util.Trie;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * Provides a code completion with static commands (command templates).
 *
 * @author Jörg Endrullis
 * @author Stefan Endrullis
 */
public class StaticCommandsCodeHelper extends PatternHelper {
  /** The command reference. */
  protected Trie<CHCommand> commands = null;
	/** Maps an environment name to a list of commands. */
	protected HashMap<String,ArrayList<CHCommand>> environments = new HashMap<String, ArrayList<CHCommand>>();
  /**
   * The last command that has been found.
   */
  protected WordWithPos command = null;

  public StaticCommandsCodeHelper(String patternString, StaticCommandsReader commandsReader) {
    pattern = new PatternPair(patternString);
	  commands = commandsReader.getCommands();
	  environments = commandsReader.getEnvironments();
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
  public Iterable<? extends CHCommand> getCompletions() {
    return getCompletions(command.word);
  }

  @Override
  public String getMaxCommonPrefix() {
    return getMaxCommonPrefix(command.word);
  }

  public Iterable<? extends CHCommand> getCompletions(String prefix) {
	  List<CHCommand> objects = commands.getObjects(prefix, 1000);
	  if (objects != null) {
	    return objects;
	  } else {
		  return new ArrayList<CHCommand>();
	  }
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

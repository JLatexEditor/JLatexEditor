/**
 * @author Jörg Endrullis
 */

package sce.codehelper;

import java.util.ArrayList;

public class CHCommand implements Comparable {
  // the name
  private String name = null;
  // the usage string
  private String usage = null;
	/** Style of the command. */
	private String style = null;
  // the hint and arguments
  private String hint = null;
	/** Arguments. */
  private ArrayList<CHCommandArgument> arguments = new ArrayList<CHCommandArgument>();

  /**
   * Creates a command with the given name.
   *
   * @param name the name
   */
  public CHCommand(String name) {
    this.name = name;
  }

  /**
   * Returns the name of the command.
   *
   * @return name
   */
  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  /**
   * Returns the usage string of the command (parameters are surrounded by @param@)
   *
   * @return usage string
   */
  public String getUsage() {
    if (usage == null) return name;
    return usage;
  }

	/**
	 * Returns the command style.
	 *
	 * @return command style
	 */
	public String getStyle() {
		return style;
	}

	/**
	 * Sets the command style.
	 *
	 * @param style command style
	 */
	public void setStyle(String style) {
		this.style = style;
	}

	/**
   * Sets the usage string of this command.
   *
   * @param usage the usage string
   */
  public void setUsage(String usage) {
    this.usage = usage;
  }

  /**
   * Returns the hint for this command.
   *
   * @return the hint.
   */
  public String getHint() {
    return hint;
  }

  /**
   * Sets the hint for this command.
   *
   * @param hint the hint
   */
  public void setHint(String hint) {
    this.hint = hint;
  }

  /**
   * Adds a argument to the command.
   *
   * @param argument the argument
   */
  public void addArgument(CHCommandArgument argument) {
    arguments.add(argument);
  }

  /**
   * Returns the arguments of the command.
   *
   * @return the arguments of the command
   */
  public ArrayList<CHCommandArgument> getArguments() {
    return arguments;
  }

  // Comparable methods

  public int compareTo(Object o) {
    if (!(o instanceof CHCommand)) return -1;
    return getName().compareTo(((CHCommand) o).getName());
  }

  /**
   * Converts the command into string.
   *
   * @return command as string
   */
  public String toString() {
    return getName();
  }
}
/**
 * @author Jörg Endrullis
 */

package sce.codehelper;

import java.util.ArrayList;
import java.util.HashMap;

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

  /** User defined? */
  private boolean isUserDefined = false;

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

  /**
   * Returns true if the command is defined or changed by the user.
   *
   * @return true if user defined
   */
  public boolean isUserDefined() {
    return isUserDefined;
  }

  /**
   * Set whether this command is defined or changed by the user.
   *
   * @param userDefined true if user defined
   */
  public void setUserDefined(boolean userDefined) {
    isUserDefined = userDefined;
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

  public boolean equals(Object obj) {
    if(!(obj instanceof CHCommand)) return false;
    CHCommand o = (CHCommand) obj;

    if(!equalsNull(name, o.name)
            || !equalsNull(usage, o.usage)
            || !equalsNull(style, o.style)
            || !equalsNull(hint, o.hint)) return false;

    if(arguments.size() != o.arguments.size()) return false;
    /* TODO: comparison of arguments
    for(int argNr = 0; argNr < arguments.size(); argNr++) {
      if(!arguments.get(argNr).equals(o.arguments.get(argNr))) return false;
    }
    */

    return true;
  }

  private boolean equalsNull(Object o1, Object o2) {
    if(o1 == null || o2 == null) return o1 != o2;
    return o1.equals(o2);
  }

	public void finalizeArguments() {
		// create map from argument name to argument
		HashMap<String, CHCommandArgument> argName2arg = new HashMap<String, CHCommandArgument>();
		for (CHCommandArgument argument : arguments) {
			argName2arg.put(argument.getName(), argument);
		}
		// assign arguments where generators reference argument names
		for (CHCommandArgument argument : arguments) {
			for (CHArgumentGenerator generator : argument.getGenerators()) {
				generator.setArgument(argName2arg.get(generator.getArgumentName()));
			}
		}
	}
}

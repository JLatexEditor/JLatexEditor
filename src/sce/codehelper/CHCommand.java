/**
 * @author Jörg Endrullis
 */

package sce.codehelper;

import java.util.ArrayList;
import java.util.HashMap;

/**
 * Command.
 *
 * @author Jörg Endrullis
 * @author Stefan Endrullis
 */
public class CHCommand implements Comparable, Cloneable {
  // the name
  private String name = null;
  // the usage string
  private String usage = null;
	/** Style of the command. */
	private String style = null;
  // the hint and arguments
  private String hint = null;
	/** Arguments. */
  private ArgumentsHashMap argumentsHashMap = new ArgumentsHashMap();

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
   * Sets the usage string of this command.
   *
   * @param usage the usage string
   */
  public void setUsage(String usage) {
    this.usage = usage;
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
    argumentsHashMap.put(argument.getName(), argument);
  }

  /**
   * Returns the arguments of the command.
   *
   * @return the arguments of the command
   */
  public ArgumentsHashMap getArgumentsHashMap() {
    return argumentsHashMap;
  }

  /**
   * Returns the arguments of the command as a new list.
   *
   * @return the arguments of the command as a new list
   */
  public ArrayList<CHCommandArgument> getArguments() {
    return argumentsHashMap.getList();
  }

	public boolean hasArgument(String argument) {
		return argumentsHashMap.containsKey(argument);
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

	/**
	 * Simple equals method that does not compare the arguments but only command properties and the number of arguments.
	 * Use @{deepEquals} if you want the arguments also be compared.
	 *
	 * @param obj other commands object
	 * @return true if both commands are equal on the surface
	 */
  public boolean equals(Object obj) {
    if(!(obj instanceof CHCommand)) return false;
    CHCommand o = (CHCommand) obj;

    if(!equalsNull(name, o.name)
            || !equalsNull(usage, o.usage)
            || !equalsNull(style, o.style)
            || !equalsNull(hint, o.hint)) return false;

	  return argumentsHashMap.size() == o.argumentsHashMap.size();
  }

	public boolean deepEquals(CHCommand that) {
		return this.equals(that) && this.argumentsHashMap.equals(that.argumentsHashMap);
	}

  private boolean equalsNull(Object o1, Object o2) {
    if (o1 == null || o2 == null) return o1 == o2;
    return o1.equals(o2);
  }

	public void finalizeArguments() {
		// assign arguments where generators reference argument names
		for (CHCommandArgument argument : getArguments()) {
			for (CHArgumentGenerator generator : argument.getGenerators()) {
				generator.setArgument(argumentsHashMap.get(generator.getArgumentName()));
			}
		}
	}

	@Override
	public CHCommand clone() {
		CHCommand c = new CHCommand(name);
		c.setUsage(usage);
		c.setStyle(style);
		c.setHint(hint);
		for (CHCommandArgument argument : getArguments()) {
			c.addArgument(argument.clone());
		}
		c.finalizeArguments();
		return c;
	}

	public static class ArgumentsHashMap extends HashMap<String, CHCommandArgument> {
		private ArrayList<CHCommandArgument> list = new ArrayList<CHCommandArgument>();

		@Override
		public CHCommandArgument put(String key, CHCommandArgument value) {
			list.add(value);
			return super.put(key, value);
		}

		@Override
		public CHCommandArgument remove(Object key) {
			list.remove(get(key));
			return super.remove(key);
		}

		public void renameArg(CHCommandArgument arg, String newName) {
			String oldName = arg.getName();
			remove(oldName);
			arg.setName(newName);
			put(newName, arg);
		}

		public void moveArgUp(CHCommandArgument arg) {
			int index = list.indexOf(arg);
			if (index > 0) {
				swapArgs(index - 1, index);
			}
		}

		public void moveArgDown(CHCommandArgument arg) {
			int index = list.indexOf(arg);
			if (index < list.size() - 1) {
				swapArgs(index, index + 1);
			}
		}

		private void swapArgs(int i1, int i2) {
			CHCommandArgument firstArg = list.get(i1);
			list.set(i1, list.get(i2));
			list.set(i2, firstArg);
		}

		public ArrayList<CHCommandArgument> getList() {
			return list;
		}

		protected boolean hasMapEquals(Object o) {
			return super.equals(o);
		}

		@Override
		public boolean equals(Object o) {
			if (o instanceof ArgumentsHashMap) {
				ArgumentsHashMap that = (ArgumentsHashMap) o;
				return this.list.equals(that.list) && this.hasMapEquals(that);
			} else {
				return false;
			}
		}
	}
}

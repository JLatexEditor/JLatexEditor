/**
 * @author Jörg Endrullis
 */

package sce.codehelper;

import sce.component.SCEDocumentRange;

import java.util.ArrayList;

public class CHCommandArgument implements Cloneable {
  // the name
  private String name = null;
	private String initialValue = null;
  private boolean optional = false;
	private boolean secondOptional = false;
	private boolean completion;
	/** Argument type used for completion and for style selection. */
	private CHArgumentType type = null;
  // the hint
  private String hint = null;
  // possible values
  private ArrayList<String> values = new ArrayList<String>();
	// generates
  private ArrayList<CHArgumentGenerator> generators = new ArrayList<CHArgumentGenerator>();

  // while editing (for templates)
  private String value = null;
  private ArrayList<SCEDocumentRange> occurrences = null;

	/**
   * Creates a command argument.
   *
   * @param name the name
	 * @param value initial value
	 * @param optional whether the argument is optional
   */
  public CHCommandArgument(String name, String value, boolean optional) {
    this.name = name;
	  this.value = value;
	  this.initialValue = value;
    this.optional = optional;
  }

	/**
   * Creates a command argument.
   *
   * @param name the name
	 * @param value initial value
	 * @param optional whether the argument is optional
	 * @param secondOptional whether the argument is second optional
	 * @param completion show completion?
   */
	public CHCommandArgument(String name, String value, boolean optional, boolean secondOptional, boolean completion) {
		this.name = name;
		this.value = value;
		this.initialValue = value;
		this.optional = optional;
		this.secondOptional = secondOptional;
		this.completion = completion;
	}

	/**
   * Returns the name of the argument.
   *
   * @return argument name
   */
  public String getName() {
    return name;
  }

	/**
	 * Sets the name of the argument.
	 *
	 * @param name new argument name
	 */
	public void setName(String name) {
		this.name = name;
	}

	/**
   * Returns true if this argument is optional.
   *
   * @return true if optional
   */
  public boolean isOptional() {
    return optional;
  }

	/**
	 * Sets whether this argument is optional or not.
	 *
	 * @param optional whether this argument is optional or not
	 */
	public void setOptional(boolean optional) {
		this.optional = optional;
	}

	/**
	 * Returns true if the argument is second optional.
	 *
	 * @return true if second optional
	 */
	public boolean isSecondOptional() {
		return secondOptional;
	}

	/**
	 * Returns true if the auto completion is activated for this argument.
	 *
	 * @return true if the auto completion is activated for this argument
	 */
	public boolean isCompletion() {
		return completion;
	}

	/**
	 * Sets whether the auto-completion shall be used for this argument.
	 *
	 * @param completion whether auto-completion shall be used for this argument
	 */
	public void setCompletion(boolean completion) {
		this.completion = completion;
	}

	/**
	 * Returns the argument type.
	 *
	 * @return argument type
	 */
	public CHArgumentType getType() {
		return type;
	}

	/**
	 * Sets the argument type.
	 *
	 * @param type argument type
	 */
	public void setType(CHArgumentType type) {
		this.type = type;
	}

	/**
   * Returns the hint for this argument.
   *
   * @return the hint.
   */
  public String getHint() {
    return hint;
  }

  /**
   * Sets the hint for this argument.
   *
   * @param hint the hint
   */
  public void setHint(String hint) {
    this.hint = hint;
  }

  /**
   * Returns true if there are suggested predefined values.
   *
   * @return true if there are predefined values
   */
  public boolean hasSuggestion() {
    return values.size() != 0;
  }

  /**
   * Returns the possible/ suggested values for the argument.
   *
   * @return list of suggested values (every value is a String)
   */
  public ArrayList<String> getValues() {
    return values;
  }

  /**
   * Adds a value to the suggested value list.
   *
   * @param value the value to add
   */
  public void addValue(String value) {
    if (value == null) return;
    values.add(value);
  }

	public ArrayList<CHArgumentGenerator> getGenerators() {
		return generators;
	}

	public void addGenerator(String argumentName, String functionName) {
		if (argumentName == null || functionName == null) return;
		generators.add(new CHArgumentGenerator(argumentName, CHFunctions.get(functionName)));
	}

	/**
   * Returns the value of the argument.
   *
   * @return the value
   */
  public String getValue() {
    return value;
  }

	/**
   * Sets the value of the argument.
   *
   * @param value the value
   */
  public void setValue(String value) {
    this.value = value;
  }

	/**
	 * Returns the initial value (used as default value for optional parameters).
	 *
	 * @return initial value
	 */
	public String getInitialValue() {
		return initialValue;
	}

	/**
	 * Sets the initial value of this argument.
	 *
	 * @param initialValue initial value of this argument
	 */
	public void setInitialValue(String initialValue) {
		this.initialValue = initialValue;
	}

	/**
   * Returns the occurrences (SCEDocumentRange[]) of the argument.
   *
   * @return occurrences of the argument
   */
  public ArrayList<SCEDocumentRange> getOccurrences() {
    return occurrences;
  }

  /**
   * Sets the occurrences of the argument (SCEDocumentRange[]).
   *
   * @param occurrences the occurrences
   */
  public void setOccurrences(ArrayList<SCEDocumentRange> occurrences) {
    this.occurrences = occurrences;
  }

	@Override
	public boolean equals(Object obj) {
		if (obj instanceof CHCommandArgument) {
			CHCommandArgument that = (CHCommandArgument) obj;
			return
				this.name.equals(that.name) &&
				this.initialValue.equals(that.initialValue) &&
				this.optional == that.optional &&
				this.secondOptional == that.secondOptional &&
				this.completion == that.completion &&
				equalsNull(this.type, that.type) &&
				equalsNull(this.hint, that.hint) &&
				this.values.equals(that.values) &&
				this.generators.equals(that.generators);
		} else {
			return false;
		}
	}

	private boolean equalsNull(Object o1, Object o2) {
	  if (o1 == null || o2 == null) {
		  return o1 == o2;
	  }
	  return o1.equals(o2);
	}

	@Override
	public CHCommandArgument clone() {
		CHCommandArgument argument = new CHCommandArgument(name, initialValue, optional, secondOptional, completion);
		argument.setHint(hint);
		for (String value : values) {
			argument.addValue(value);
		}
		for (CHArgumentGenerator generator : generators) {
			argument.addGenerator(generator.getArgument().getName(), generator.getFunctionName());
		}
		return argument;
	}
}

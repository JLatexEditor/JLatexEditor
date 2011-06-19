/**
 * @author Jörg Endrullis
 */

package sce.codehelper;

import sce.component.SCEDocumentRange;

import java.util.ArrayList;

public class CHCommandArgument {
  // the name
  private String name = null;
  private boolean optional = false;
	private boolean secondOptional = false;
	private boolean completion;
	/** Argument type used for completion and for style selection. */
	private CHArgumentType type = null;
  // the hint
  private String hint = null;
  // possible values
  private ArrayList<String> values = new ArrayList<String>();

  // while editing (for templates)
  private String value = null;
  private String initialValue = null;
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
   * @return the name
   */
  public String getName() {
    return name;
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

  /**
   * Returns the value of the argument.
   *
   * @return the value
   */
  public String getValue() {
    return value;
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
   * Sets the value of the argument.
   *
   * @param value the value
   */
  public void setValue(String value) {
    this.value = value;
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
}

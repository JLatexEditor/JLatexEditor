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
  // the hint
  private String hint = null;
  // possible values
  private ArrayList<String> values = new ArrayList<String>();

  // while editing (for templates)
  private String value = null;
  private ArrayList<SCEDocumentRange> occurrences = null;

	/**
   * Creates a command argument.
   *
   * @param name the name
	 * @param optional whether the argument is optional
   */
  public CHCommandArgument(String name, boolean optional) {
    this.name = name;
    this.optional = optional;
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

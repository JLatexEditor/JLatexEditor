
/**
 * @author Jörg Endrullis
 */

package sce.codehelper;

import java.util.ArrayList;

public class CHCommandArgument {
  // the name
  private String name = null;
  private boolean optional = false;
  // the hint
  private String hint = null;
  // possible values
  private ArrayList values = new ArrayList();

  // while editing (for templates)
  private String value = null;
  private ArrayList occurences = null;

  /**
   * Creates a command argument.
   *
   * @param name the name
   */
  public CHCommandArgument(String name, boolean optional){
    this.name = name;
    this.optional = optional;
  }

  /**
   * Returns the name of the argument.
   *
   * @return the name
   */
  public String getName(){
    return name;
  }

  /**
   * Returns true if this argument is optional.
   *
   * @return true if optional
   */
  public boolean isOptional(){
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
   * Returns true if there are suggested predifined values.
   *
   * @return true if there are predifined values
   */
  public boolean hasSuggestion(){
    return values.size() != 0;
  }

  /**
   * Returns the possible/ suggested values for the argument.
   *
   * @return list of suggested values (every value is a String)
   */
  public ArrayList getValues(){
    return values;
  }

  /**
   * Adds a value to the suggested value list.
   *
   * @param value the value to add
   */
  public void addValue(String value){
    if(value == null) return;
    values.add(value);
  }

  /**
   * Returns the value of the argument.
   *
   * @return the value
   */
  public String getValue(){
    return value;
  }

  /**
   * Sets the value of the argument.
   *
   * @param value the value
   */
  public void setValue(String value){
    this.value = value;
  }

  /**
   * Returns the occurences (SCEDocumentRange[]) of the argument.
   *
   * @return occurences of the argument
   */
  public ArrayList getOccurrences(){
    return occurences;
  }

  /**
   * Sets the occurences of the argument (SCEDocumentRange[]).
   *
   * @param occurences the occurences
   */
  public void setOccurrences(ArrayList occurences){
    this.occurences = occurences;
  }
}

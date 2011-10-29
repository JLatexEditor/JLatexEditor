package jlatexeditor.codehelper;

import sce.codehelper.CHCommand;

/**
 * Completion of given value.
 *
 * @author Stefan Endrullis &lt;stefan@endrullis.de&gt;
 */
public class ValueCompletion extends CHCommand {
  /**
   * Creates a completion with the given value.
   *
   * @param value value
   */
  public ValueCompletion(String value) {
    super(value);
  }
}

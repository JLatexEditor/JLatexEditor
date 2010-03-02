/**
 * @author Jörg Endrullis
 */

package sce.component;

public class SCEDocumentChar {
  // the character and style
  public char character = ' ';
  public byte style = 0;
  public byte overlayStyle = 100;
  // the column number
  public int column_nr = 0;
  // the row that contains this character
  public SCEDocumentRow row = null;
}

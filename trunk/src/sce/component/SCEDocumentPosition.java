/**
 * @author Jörg Endrullis
 */

package sce.component;

public class SCEDocumentPosition implements SCEPosition {
  // dynamic position
  private SCEDocumentChar character = null;
  private SCEDocumentRow row = null;
  private int relative_row = 0;
  private int relative_column = 0;

  // static position
  private int row_nr = 0;
  private int column_nr = 0;

  /**
   * Creates a document position.
   *
   * @param character the character in the document
   */
  public SCEDocumentPosition(SCEDocumentChar character) {
    this.character = character;
  }

  /**
   * Creates a document position with a relative displacement.
   *
   * @param character       the character in the document
   * @param relative_row    relative row add
   * @param relative_column relative column add
   */
  public SCEDocumentPosition(SCEDocumentChar character, int relative_row, int relative_column) {
    this.character = character;
    this.relative_row = relative_row;
    this.relative_column = relative_column;
  }

  /**
   * Creates a document position.
   *
   * @param row             the row
   * @param relative_column 0 = start of the row, 1 = end of the row
   */
  public SCEDocumentPosition(SCEDocumentRow row, int relative_column) {
    this.row = row;
    this.relative_column = relative_column;
  }

  /**
   * Creates a fixed document position (will not be updated).
   *
   * @param row    the row
   * @param column the column
   */
  public SCEDocumentPosition(int row, int column) {
    this.row_nr = row;
    this.column_nr = column;
  }

  /**
   * Sets the position.
   *
   * @param character the character in the document
   */
  public void setPosition(SCEDocumentChar character) {
    this.character = character;
  }

	public void setPosition(SCEPosition pos) {
		this.row_nr = pos.getRow();
		this.column_nr = pos.getColumn();
	}

  /**
   * Sets a fixed position (will not be updated).
   *
   * @param row    the row
   * @param column the column
   */
  public void setPosition(int row, int column) {
    this.row_nr = row;
    this.column_nr = column;
  }

  /**
   * Returns the row.
   *
   * @return the row
   */
  public int getRow() {
    if (character != null) return character.row.row_nr + relative_row;
    if (row != null) return row.row_nr;
    return row_nr;
  }

  /**
   * Returns the column.
   *
   * @return the column
   */
  public int getColumn() {
    if (character != null) return character.column_nr + relative_column;
    if (row != null) {
      if (relative_column <= 0) {
        return relative_column;
      } else {
        return row.length + relative_column - 1;
      }
    }
    return column_nr;
  }

  // Comparable methods

  public int compareTo(Object o) {
    if (!(o instanceof SCEDocumentPosition)) return -1;

    SCEDocumentPosition oPos = (SCEDocumentPosition) o;

    if (getRow() < oPos.getRow() || (getRow() == oPos.getRow() && getColumn() < oPos.getColumn())) return -1;
    if (getRow() == oPos.getRow() && getColumn() == oPos.getColumn()) return 0;
    return 1;
  }

  public boolean equals(Object obj) {
    return compareTo(obj) == 0;
  }

  @Override
  public String toString() {
    return getRow() + ", " + getColumn();
  }

	public SCEPosition relative(int relativeRow, int relativeColumn) {
		return new SCEDocumentPosition(getRow() + relativeRow, getColumn() + relativeColumn);
	}
}

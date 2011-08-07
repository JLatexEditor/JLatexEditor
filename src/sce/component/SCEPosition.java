package sce.component;

/**
 * Abstract document position.
 *
 * @author Stefan Endrullis
 */
public abstract class SCEPosition implements Comparable {
  /**
   * Returns the row.
   *
   * @return the row
   */
  public abstract int getRow();

  /**
   * Returns the column.
   *
   * @return the column
   */
  public abstract int getColumn();

	/**
	 * Returns a new document position relative to this one.
	 *
	 * @param relativeRow relative row
	 * @param relativeColumn relative column
	 * @return new document position relative to this one
	 */
	public SCEPosition relative(int relativeRow, int relativeColumn) {
		return new SCEDocumentPosition(getRow() + relativeRow, getColumn() + relativeColumn);
	}
}

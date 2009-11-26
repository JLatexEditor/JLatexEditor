package sce.component;

/**
 * Abstract document position.
 *
 * @author Stefan Endrullis
 */
public interface SCEPosition {
	/**
	 * Returns the row.
	 *
	 * @return the row
	 */
	public int getRow();

	/**
	 * Returns the column.
	 *
	 * @return the column
	 */
	public int getColumn();	
}

/**
 * @author Jörg Endrullis
 */

package sce.component;

public class SCEDocumentRange {
  // start and end position
  protected SCEPosition startPos = null;
  protected SCEPosition endPos = null;

  /**
   * Creates a document range with the given start and end positions.
   *
   * @param startPos the beginning of the range
   * @param endPos   the end of the range
   */
  public SCEDocumentRange(SCEPosition startPos, SCEPosition endPos) {
    this.startPos = startPos;
    this.endPos = endPos;
  }

	public SCEDocumentRange(int startRow, int startCol, int endRow, int endCol) {
		this.startPos = new SCEDocumentPosition(startRow, startCol);
		this.endPos   = new SCEDocumentPosition(endRow, endCol);
	}

	/**
   * Returns the start position of the range.
   *
   * @return start position
   */
  public SCEPosition getStartPos() {
    return startPos;
  }

  /**
   * Returns the end position of the range.
   *
   * @return end position
   */
  public SCEPosition getEndPos() {
    return endPos;
  }

	public int getStartRow() {
		return startPos.getRow();
	}

	public int getStartCol() {
		return startPos.getColumn();
	}

	public int getEndRow() {
		return endPos.getRow();
	}

	public int getEndCol() {
		return endPos.getColumn();
	}

  public boolean contains(int row, int column) {
    if(row < startPos.getRow() || (row == startPos.getRow() && column < startPos.getColumn())) return false;
    if(row > endPos.getRow() || (row == endPos.getRow() && column > endPos.getColumn())) return false;
    return true;
  }
}

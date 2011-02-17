package sce.component;

/**
 * Range in a SECDocument.
 *
 * @author Stefan Endrullis &lt;stefan@endrullis.de&gt;
 */
public class SCERange {
  protected int startRow;
  protected int startCol;
  protected int endRow;
  protected int endCol;

  public SCERange(int startRow, int startCol, int endRow, int endCol) {
    this.startRow = startRow;
    this.startCol = startCol;
    this.endRow = endRow;
    this.endCol = endCol;
  }

  public int getStartRow() {
    return startRow;
  }

  public int getStartCol() {
    return startCol;
  }

  public int getEndRow() {
    return endRow;
  }

  public int getEndCol() {
    return endCol;
  }

  public SCEDocumentPosition getStartPos() {
    return new SCEDocumentPosition(getStartRow(), getStartCol());
  }

  public SCEDocumentPosition getEndPos() {
    return new SCEDocumentPosition(getEndRow(), getEndCol());
  }

  @Override
  public String toString() {
    return "SCERange{" +
            "startRow=" + startRow +
            ", startCol=" + startCol +
            ", endRow=" + endRow +
            ", endCol=" + endCol +
            '}';
  }
}

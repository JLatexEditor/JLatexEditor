/**
 * @author Jörg Endrullis
 */

package editor.component;

public class SCEDocumentRange{
  // start and end position
  private SCEDocumentPosition startPosition = null;
  private SCEDocumentPosition endPosition = null;

  /**
   * Creates a document range with the given start and end positions.
   *
   * @param startPosition the beginning of the range
   * @param endPosition the end of the range
   */
  public SCEDocumentRange(SCEDocumentPosition startPosition, SCEDocumentPosition endPosition){
    this.startPosition = startPosition;
    this.endPosition = endPosition;
  }

  /**
   * Returns the start position of the range.
   *
   * @return start position
   */
  public SCEDocumentPosition getStartPosition(){
    return startPosition;
  }

  /**
   * Returns the end position of the range.
   *
   * @return end position
   */
  public SCEDocumentPosition getEndPosition(){
    return endPosition;
  }
}

/**
 * @author Jörg Endrullis
 */

package editor.component;

import java.awt.*;

public class SCEEditRangeHighlight extends SCETextHighlight{
  /**
   * Creates a edit range highlight.
   *
   * @param pane the text pane
   * @param startPosition the start position
   * @param endPosition the end position
   * @param color the color
   */
  public SCEEditRangeHighlight(SCEPane pane, SCEDocumentPosition startPosition, SCEDocumentPosition endPosition, Color color){
    super(pane, startPosition, endPosition, color);
  }

  /**
   * Draws the highlight.
   *
   * @param g2D the graphics object
   * @param pane the text pane
   * @param start the start position
   * @param end the end position
   */
  public void paint(Graphics2D g2D, SCEPane pane, SCEDocumentPosition start, SCEDocumentPosition end){
    g2D.setColor(getColor());
    Point startPos = pane.modelToView(start.getRow(), start.getColumn());
    Point endPos = pane.modelToView(end.getRow(), end.getColumn());

    g2D.drawRect(startPos.x, startPos.y, endPos.x - startPos.x, pane.getLineHeight());
  }
}

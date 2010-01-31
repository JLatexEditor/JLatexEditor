
/**
 * @author Jörg Endrullis
 */

package sce.component;

import java.awt.*;

public class SCERowHighlight{
  // the row and color of the row highlight
  private SCEDocumentPosition position;
  private Color color;
  private boolean top;

  /**
   * Creates a row highlight.
   *
   * @param pane the text pane
   * @param row the row
   * @param color the color
   */
  public SCERowHighlight(SCEPane pane, int row, Color color, boolean top){
    position = pane.getDocument().createDocumentPosition(row, 0);
    this.color = color;
    this.top = top;
  }

  /**
   * Returns the positoin of this highlight.
   *
   * @return the row
   */
  public SCEDocumentPosition getPosition(){
    return position;
  }

  /**
   * Returns the color of this highlight.
   *
   * @return the color
   */
  public Color getColor(){
    return color;
  }

  /**
   * Draws the highlight.
   *
   * @param g2D the graphics object
   * @param pane the text pane
   */
  public void paint(Graphics2D g2D, SCEPane pane){
    g2D.setColor(getColor());
    Point highlightPos = pane.modelToView(position.getRow(), 0);
    if(top) {
      g2D.fillRect(0, highlightPos.y-1, pane.getWidth(), 3);
    } else {
      g2D.fillRect(0, highlightPos.y, pane.getWidth(), pane.getLineHeight());
    }
  }
}

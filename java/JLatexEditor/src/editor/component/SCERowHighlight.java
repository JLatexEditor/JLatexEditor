
/**
 * @author Jörg Endrullis
 */

package editor.component;

import java.awt.*;

public class SCERowHighlight{
  // the row and color of the row highlight
  private SCEDocumentPosition position;
  private Color color;

  /**
   * Creates a row highlight.
   *
   * @param pane the text pane
   * @param row the row
   * @param color the color
   */
  public SCERowHighlight(SCEPane pane, int row, Color color){
    position = pane.getDocument().createDocumentPosition(row, 0);
    this.color = color;
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
    g2D.fillRect(pane.getMargin(SCEPane.MARGIN_LEFT), highlightPos.y, pane.getWidth(), pane.getLineHeight());
  }
}

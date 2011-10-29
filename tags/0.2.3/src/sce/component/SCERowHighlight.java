/**
 * @author Jörg Endrullis
 */

package sce.component;

import java.awt.*;

public class SCERowHighlight {
  // the row and color of the row highlight
  private SCEDocumentPosition position;
  private Color color;
  private boolean top;
  private boolean frameTop, frameBottom;

  /**
   * Creates a row highlight.
   *
   * @param pane  the text pane
   * @param row   the row
   * @param color the color
   */
  public SCERowHighlight(SCEPane pane, int row, Color color, boolean top) {
    this(pane, row, color, top, false, false);
  }

  public SCERowHighlight(SCEPane pane, int row, Color color, boolean top, boolean frameTop, boolean frameBottom) {
    position = pane.getDocument().createDocumentPosition(row, 0);
    this.color = color;
    this.top = top;
    this.frameTop = frameTop;
    this.frameBottom = frameBottom;
  }

  /**
   * Returns the positoin of this highlight.
   *
   * @return the row
   */
  public SCEDocumentPosition getPosition() {
    return position;
  }

  /**
   * Returns the color of this highlight.
   *
   * @return the color
   */
  public Color getColor() {
    return color;
  }

  /**
   * Draws the highlight.
   *
   * @param g2D  the graphics object
   * @param pane the text pane
   */
  public void paint(Graphics2D g2D, SCEPane pane) {
    g2D.setColor(getColor());
    Point highlightPos = pane.modelToView(position.getRow(), 0);

    final int x,y,width,height;
    if (top) {
      x = 0;
      y = highlightPos.y - 2;
      width = pane.getWidth();
      height = 3;
    } else {
      x = 0;
      y = highlightPos.y - 1;
      width = pane.getWidth();
      height = pane.getLineHeight();
    }

    g2D.fillRect(x, y, width, height);

    g2D.setColor(Color.GRAY);
    g2D.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
    if(frameTop) g2D.drawLine(x, y, x + width, y);
    if(frameBottom) g2D.drawLine(x, y+height-1, x + width, y+height-1);
    g2D.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_OFF);
  }
}

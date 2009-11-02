
/**
 * @author Jörg Endrullis
 */

package editor.component;

import java.awt.*;

public class SCEErrorHighlight extends SCETextHighlight{
  /**
   * Creates a error highlight.
   *
   * @param pane the text pane
   * @param startPosition the start position
   * @param endPosition the end position
   * @param color the color
   */
  public SCEErrorHighlight(SCEPane pane, SCEDocumentPosition startPosition, SCEDocumentPosition endPosition, Color color){
    super(pane, startPosition, endPosition, color);
  }

  // paint line under one line of text
  private void paintLine(Graphics2D g2D, int x1, int x2, int y){
    if(x1 > x2 - 7) x2 += 7;

    for(int x = x1; x < x2; x += 4){
      g2D.drawLine(x, y, x + 2, y - 2);
      g2D.drawLine(x + 2, y - 2, x + 4, y);
    }
  }

  public void paint(Graphics2D g2D, SCEPane pane){
    Rectangle bounds = g2D.getClipBounds();

    SCEDocumentPosition startPosition = getStartPosition();
    SCEDocumentPosition endPosition = getEndPosition();
    g2D.setColor(getColor());
    Point startPos = pane.modelToView(startPosition.getRow(), startPosition.getColumn());
    Point endPos = pane.modelToView(endPosition.getRow(), endPosition.getColumn());

    int y = startPos.y + pane.getLineHeight() - 2;
    int xMin = bounds.getBounds().x;
    int xMax = bounds.getBounds().x + bounds.getBounds().width;

    // If they are on the same line
    if(startPos.y == endPos.y){
      paintLine(g2D, startPos.x, endPos.x + pane.getCharacterWidth(), y);
      return;
    }

    // Draw until the end of the line
    paintLine(g2D, startPos.x, xMax, y);

    // Draw the other lines
    y += pane.getLineHeight();
    while(y < endPos.y){
      paintLine(g2D, xMin, xMax, y);
      y += pane.getLineHeight();
    }

    // Draw the last line
    paintLine(g2D, xMin, endPos.x, y);
  }
}

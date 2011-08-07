/**
 * @author Jörg Endrullis
 */

package sce.component;

import javax.swing.*;
import java.awt.*;

public class SCETextHighlight {
  // the start and end position of the highlight
  private SCEPosition startPosition;
  private SCEPosition endPosition;
  private Color color;

  private JComponent actionComponent;
  private boolean actionComponentAtEnd;

  /**
   * Creates a text highlight.
   *
   * @param pane          the text pane
   * @param startPosition the start position
   * @param endPosition   the end position
   * @param color         the color
   */
  public SCETextHighlight(SCEPane pane, SCEPosition startPosition, SCEPosition endPosition, Color color) {
    this(pane, startPosition, endPosition, color, null, false);
  }

  public SCETextHighlight(SCEPane pane, SCEPosition startPosition, SCEPosition endPosition, Color color, JComponent actionComponent, boolean actionComponentAtEnd) {
    this.startPosition = startPosition;
    this.endPosition = endPosition;
    this.color = color;
    this.actionComponent = actionComponent;
    this.actionComponentAtEnd = actionComponentAtEnd;
  }

  /**
   * Returns the start position of this highlight.
   *
   * @return the start position
   */
  public SCEPosition getStartPosition() {
    return startPosition;
  }

  /**
   * Returns the end position of this highlight.
   *
   * @return the end position
   */
  public SCEPosition getEndPosition() {
    return endPosition;
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
   * Action component.
   */
  public JComponent getActionComponent() {
    return actionComponent;
  }

  public void setActionComponent(JComponent actionComponent) {
    this.actionComponent = actionComponent;
  }

  public boolean isActionComponentAtEnd() {
    return actionComponentAtEnd;
  }

  public void setActionComponentAtEnd(boolean actionComponentAtEnd) {
    this.actionComponentAtEnd = actionComponentAtEnd;
  }

  /**
   * Draws the highlight.
   *
   * @param g2D  the graphics object
   * @param pane the text pane
   */
  public void paint(Graphics2D g2D, SCEPane pane) {
    paint(g2D, pane, startPosition, endPosition);
  }

  /**
   * Draws the highlight.
   *
   * @param g2D   the graphics object
   * @param pane  the text pane
   * @param start the start position
   * @param end   the end position
   */
  public void paint(Graphics2D g2D, SCEPane pane, SCEPosition start, SCEPosition end) {
    g2D.setColor(getColor());
    Point startPos = pane.modelToView(start.getRow(), start.getColumn());
    Point endPos = pane.modelToView(end.getRow(), end.getColumn());

    if (start.getRow() == end.getRow()) {
      g2D.fillRect(startPos.x, startPos.y, endPos.x - startPos.x, pane.getLineHeight());
    } else {
      int marginLeft = pane.getMargin(SCEPane.MARGIN_LEFT);
      g2D.fillRect(startPos.x, startPos.y, pane.getWidth(), pane.getLineHeight());
      g2D.fillRect(marginLeft, startPos.y + pane.getLineHeight(), pane.getWidth(), endPos.y - startPos.y - pane.getLineHeight());
      g2D.fillRect(marginLeft, endPos.y, endPos.x - marginLeft, pane.getLineHeight());
    }
  }
}

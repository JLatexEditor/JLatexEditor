package sce.component;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;

/**
 * Cared in a document.
 *
 * @author Jörg Endrullis
 * @author Stefan Endrullis
 */
public class SCECaret implements SCEPosition, Comparable, ActionListener {
  private SCEPane pane = null;
  private SCEDocument document = null;

  // row and column of the caret
  private SCEDocumentPosition position = null;
  private SCEDocumentPosition selectionMark = null;
  // the column where we want to be
  private int virtualColumn = 0;

  // blink timer
  private boolean cursorVisible = true;
  private Timer timer = null;

  // directions
  public static final int DIRECTION_UP = 0;
  public static final int DIRECTION_DOWN = 1;
  public static final int DIRECTION_LEFT = 2;
  public static final int DIRECTION_RIGHT = 3;

  // when was the last cursor motion?
  long lastMotionTime = 0;

  // caret listener
  ArrayList<SCECaretListener> caretListeners = new ArrayList<SCECaretListener>();

  /**
   * Creates a caret for a sce.component.SCEPane.
   *
   * @param pane SCE pane
   */
  public SCECaret(SCEPane pane) {
    this.pane = pane;
    document = pane.getDocument();
    position = new SCEDocumentPosition(0, 0);

    // blink timer
    timer = new Timer(500, this);
    timer.start();
  }

  /**
   * Returns the row of the caret position.
   *
   * @return row
   */
  public int getRow() {
    return position.getRow();
  }

  /**
   * Returns the column of the caret position.
   *
   * @return column
   */
  public int getColumn() {
    return position.getColumn();
  }

  /**
   * Moves the carent in the specified direction.
   *
   * @param direction a direction constant
   * @param keepSelection keep selection?
   */
  public void move(int direction, boolean keepSelection) {
    SCEDocumentRows rowsModel = document.getRowsModel();

    if (direction == DIRECTION_UP) moveToVirt(getRow() - 1, virtualColumn, keepSelection, false);
    if (direction == DIRECTION_DOWN) moveToVirt(getRow() + 1, virtualColumn, keepSelection, false);
    if (direction == DIRECTION_LEFT) {
      if (getColumn() == 0 && getRow() > 0) {
        moveTo(getRow() - 1, rowsModel.getRowLength(getRow() - 1), keepSelection);
      } else {
        moveTo(getRow(), getColumn() - 1, keepSelection);
      }
    }
    if (direction == DIRECTION_RIGHT) {
      if (getColumn() == rowsModel.getRowLength(getRow()) && getRow() < rowsModel.getRowsCount() - 1) {
        moveTo(getRow() + 1, 0, keepSelection);
      } else {
        moveTo(getRow(), getColumn() + 1, keepSelection);
      }
    }
  }

  /**
   * Moves the caret to the specified position.
   *
   * @param row    the row
   * @param column the column
   * @param keepSelection keep selection?
   */
  public void moveTo(int row, int column, boolean keepSelection) {
	  moveToVirt(row, column, keepSelection, true);
  }

  /**
   * Moves the caret to the specified position.
   *
   * @param pos position
   * @param keepSelection keep selection
   */
  public void moveTo(SCEPosition pos, boolean keepSelection) {
    moveTo(pos.getRow(), pos.getColumn(), keepSelection);
  }

  /**
   * Moves the caret to the specified position.
   *
   * @param row                 row
   * @param column              column
   * @param keepSelection       keep selection?
   * @param updateVirtualColumn true, if the virtual column should be updated
   */
  private void moveToVirt(int row, int column, boolean keepSelection, boolean updateVirtualColumn) {
    // remember the last position
    int lastRow = getRow();
    int lastColumn = getColumn();

    showCursor(false);

	  // set new position
    int new_row = Math.max(Math.min(row, document.getRowsModel().getRowsCount() - 1), 0);
    int new_column = Math.max(Math.min(column, document.getRowsModel().getRowLength(new_row)), 0);
    position.setPosition(new_row, new_column);

	  // ensure we stay within edit range
	  if (document.hasEditRange()) {
		  if (this.compareTo(document.getEditRangeStart()) < 0) {
			  position.setPosition(document.getEditRangeStart());
		  } else
		  if (this.compareTo(document.getEditRangeEnd()) > 0) {
			  position.setPosition(document.getEditRangeEnd());
		  }
	  }
	  
    if (updateVirtualColumn) virtualColumn = getColumn();

    showCursor(true);

	  if (keepSelection) {
			// update the documents selection range
			document.setSelectionRange(position, selectionMark, true);
	  } else {
		  // remove selection
		  if (document.hasSelection()) {
				document.clearSelection();
				pane.repaint();
		  }
		  setSelectionMark();
	  }

    // inform listeners
    for (SCECaretListener caretListener : caretListeners) {
      caretListener.caretMoved(getRow(), getColumn(), lastRow, lastColumn);
    }

    // upate last Motion time
    lastMotionTime = System.currentTimeMillis();
  }

  /**
   * Sets the selection mark to the current caret position.
   */
  public void setSelectionMark() {
    selectionMark = new SCEDocumentPosition(getRow(), getColumn());
  }

  /**
   * Removes the selection mark.
   */
  public void removeSelectionMark() {
    selectionMark = null;
    document.setSelectionRange(null, null, true);
  }

  /**
   * Returns the selection mark of null, if there is none.
   *
   * @return the selection mark
   */
  public SCEDocumentPosition getSelectionMark() {
    return selectionMark;
  }

  /**
   * Show/ hide the cursor.
   *
   * @param show true, if the cursor should be showed
   */
  public void showCursor(boolean show) {
    cursorVisible = show;

    // repaint the caret
    Rectangle caretRect = getCaretRectangle();
    pane.repaint(caretRect);
  }

  /**
   * Calculates the caret visualization rectangle.
   *
   * @return the visualization rectangle
   */
  private Rectangle getCaretRectangle() {
    Point position = pane.modelToView(getRow(), getColumn());
    return new Rectangle(position.x, position.y, 2, pane.getLineHeight());
  }

  /**
   * Draws the caret.
   *
   * @param g the graphics object to draw on
   */
  public void paint(Graphics g) {
    if (!cursorVisible) return;

    // Draw the caret
    g.setColor(Color.BLACK);
    g.setXORMode(pane.getBackground());

    Rectangle caretRect = getCaretRectangle();
    g.fillRect(caretRect.x, caretRect.y, caretRect.width, caretRect.height);
  }

  // ActionListener methods

  public void actionPerformed(ActionEvent e) {
    if (lastMotionTime + 250 < System.currentTimeMillis()) {
      showCursor(!cursorVisible);
    }
  }

  /**
   * Adds a caret listener.
   *
   * @param listener the listener
   */
  public void addSCECaretListener(SCECaretListener listener) {
    caretListeners.add(listener);
  }

  /**
   * Removes a caret listener.
   *
   * @param listener the listener
   */
  public void removeSCECaretListener(SCECaretListener listener) {
    caretListeners.remove(listener);
  }

	// Comparable methods

	public int compareTo(Object o) {
	  if (!(o instanceof SCEDocumentPosition)) return -1;

	  SCEDocumentPosition oPos = (SCEDocumentPosition) o;

	  if (getRow() < oPos.getRow() || (getRow() == oPos.getRow() && getColumn() < oPos.getColumn())) return -1;
	  if (getRow() == oPos.getRow() && getColumn() == oPos.getColumn()) return 0;
	  return 1;
	}

	public boolean equals(Object obj) {
	  return compareTo(obj) == 0;
	}

	@Override
	public String toString() {
	  return getRow() + ", " + getColumn();
	}
}

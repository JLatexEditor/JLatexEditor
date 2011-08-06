/**
 * @author Jörg Endrullis
 */

package sce.component;

import java.util.Stack;

public class SCEUndoManager implements SCEDocumentListener {
  public static long timeDistance = 250;

  // the document
  private SCEPane pane = null;
  private SCEDocument document = null;

  // the captured events
  private Stack<SCEDocumentEvent> lastEvents = new Stack<SCEDocumentEvent>();
  // the redo events
  private Stack<SCEDocumentEvent> nextEvents = new Stack<SCEDocumentEvent>();

  /**
   * Creates an undo manager for the specific document.
   *
   * @param pane the sce pane
   */
  public SCEUndoManager(SCEPane pane) {
    this.pane = pane;
    this.document = pane.getDocument();

    document.addSCEDocumentListener(this);
  }

  /**
   * Undos the last event.
   */
  public void undo(boolean atomic) {
    if (lastEvents.isEmpty()) return;

    long lastTime = lastEvents.peek().getTimeMillis();
    while (!lastEvents.isEmpty()) {
      SCEDocumentEvent event = lastEvents.peek();
      if (Math.abs(event.getTimeMillis() - lastTime) > timeDistance) break;
      lastTime = event.getTimeMillis();
      lastEvents.pop();
      nextEvents.push(event);

      SCEDocumentPosition start = event.getRangeStart();
      SCEDocumentPosition end = event.getRangeEnd();
      String text = event.getText();

      if (event.isInsert()) {
        document.remove(start.getRow(), start.getColumn(), end.getRow(), end.getColumn(), SCEDocumentEvent.EVENT_UNDO, false);
      }
      if (event.isRemove()) {
        document.insert(text, start.getRow(), start.getColumn(), SCEDocumentEvent.EVENT_UNDO, false);
      }
	    if (event.isEditRange()) {
		    pane.removeEditRangeSilently();
	    }

      if (atomic) break;
    }

    document.setSelectionRange(null, null, true);
  }

  /**
   * Redos the last undo event.
   */
  public void redo(boolean atomic) {
    if (nextEvents.isEmpty()) return;

    long lastTime = nextEvents.peek().getTimeMillis();
    while (!nextEvents.isEmpty()) {
      SCEDocumentEvent event = nextEvents.peek();
      if (Math.abs(event.getTimeMillis() - lastTime) > timeDistance) break;
      lastTime = event.getTimeMillis();
      nextEvents.pop();
      lastEvents.push(event);

      SCEDocumentPosition start = event.getRangeStart();
      SCEDocumentPosition end = event.getRangeEnd();
      String text = event.getText();

      if (event.isInsert()) {
        document.insert(text, start.getRow(), start.getColumn(), SCEDocumentEvent.EVENT_REDO, false);
      }
      if (event.isRemove()) {
        document.remove(start.getRow(), start.getColumn(), end.getRow(), end.getColumn(), SCEDocumentEvent.EVENT_REDO, false);
      }

      if (atomic) break;
    }

    pane.clearSelection();
  }

  /**
   * Clear undo list.
   */
  public void clear() {
    lastEvents.clear();
    nextEvents.clear();
  }

  // add event to lastEvents vector

  public void documentChanged(SCEDocument sender, SCEDocumentEvent event) {
    // don't work with undo events
    if ((event.getEventType() & SCEDocumentEvent.EVENT_UNDO) != 0) return;
    if ((event.getEventType() & SCEDocumentEvent.EVENT_REDO) != 0) return;

    lastEvents.push(event);
    nextEvents.clear();
  }
}

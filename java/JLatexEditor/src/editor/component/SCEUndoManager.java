
/**
 * @author Jörg Endrullis
 */

package editor.component;

import java.util.Stack;

public class SCEUndoManager implements SCEDocumentListener{
  // the document
  private SCEDocument document = null;

  // the captured events
  private Stack lastEvents = new Stack();
  // the redo events
  private Stack nextEvents = new Stack();

  /**
   * Creates an undo manager for the specific document.
   *
   * @param document the document
   */
  public SCEUndoManager(SCEDocument document){
    this.document = document;

    document.addSCEDocumentListener(this);
  }

  /**
   * Undos the last event.
   */
  public void undo(){
    if(lastEvents.isEmpty()) return;

    SCEDocumentEvent event = (SCEDocumentEvent) lastEvents.pop();

    SCEDocumentPosition start = event.getRangeStart();
    SCEDocumentPosition end = event.getRangeEnd();

    if(event.isInsert()){
      document.remove(start.getRow(), start.getColumn(), end.getRow(), end.getColumn(), SCEDocumentEvent.EVENT_UNDO);
    }
    if(event.isRemove()){
      document.insert(event.getText(), start.getRow(), start.getColumn(), SCEDocumentEvent.EVENT_UNDO);
    }
  }

  /**
   * Redos the last undo event.
   */
  public void redo(){
	  // TODO
  }

  // add event to lastEvents vector
  public void documentChanged(SCEDocument sender, SCEDocumentEvent event){
    // don't work with undo events
    if((event.getEventType() & SCEDocumentEvent.EVENT_UNDO) != 0) return;

    lastEvents.push(event);
  }
}

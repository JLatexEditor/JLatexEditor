
/**
 * @author Jörg Endrullis
 */

package editor.component;

public class SCEDocumentEvent {
  // undoable events
  public static final int EVENT_INSERT    = 1 << 0;
  public static final int EVENT_REMOVE    = 1 << 1;

  // style events (not undoable)
  public static final int EVENT_STYLE     = 1 << 2;
  public static final int EVENT_EDITRANGE = 1 << 3;

  // update view (cursor pos)?
  public static final int UPDATE_VIEW     = 1 << 4;

  // undo event
  public static final int EVENT_UNDO      = 1 << 5;

  // the event properties
  private int eventType = 0;
  private SCEDocumentPosition start = null;
  private SCEDocumentPosition end = null;
  private String text = null;

  // the time when the event occurred
  private long time = 0;

  /**
   * Creates a document event.
   */
  public SCEDocumentEvent(){
    time = System.currentTimeMillis();
  }

  /**
   * Returns the time when the event occurred.
   *
   * @return the time
   */
  public long getTimeMillis(){
    return time;
  }

  /**
   * Returns the type of the event.
   *
   * @return event type
   */
  public int getEventType(){
    return eventType;
  }

  /**
   * Sets the type of the event.
   *
   * @param eventType event type
   */
  public void setEventType(int eventType){
    this.eventType = eventType;
  }

  /**
   * Check if the event is an insert event.
   *
   * @return true, if insert event
   */
  public boolean isInsert(){
    return (eventType & EVENT_INSERT) != 0;
  }

  /**
   * Sets the EVENT_INSERT flag of event type.
   */
  public void setInsert(){
    eventType |= EVENT_INSERT;
  }

  /**
   * Check if the event is an remove event.
   *
   * @return true, if remove event
   */
  public boolean isRemove(){
    return (eventType & EVENT_REMOVE) != 0;
  }

  /**
   * Sets the EVENT_REMOVE flag of event type.
   */
  public void setRemove(){
    eventType |= EVENT_REMOVE;
  }

  /**
   * Check if the event forces an update of cursor position.
   *
   * @return true, if the cursor position should be updated
   */
  public boolean updateView(){
    return (eventType & UPDATE_VIEW) != 0;
  }

  /**
   * Sets the UPDATE_VIEW flag of event type.
   */
  public void setUpdateView(){
    eventType |= UPDATE_VIEW;
  }

  /**
   * Returns the start of the damage range.
   *
   * @return start of the range
   */
  public SCEDocumentPosition getRangeStart(){
    return start;
  }

  /**
   * Returns the end of the damage range.
   *
   * @return end of the damage range
   */
  public SCEDocumentPosition getRangeEnd(){
    return end;
  }

  /**
   * Sets the range of the damage.
   *
   * @param startRow start row
   * @param startColumn start column
   * @param endRow end row
   * @param endColumn end column
   */
  public void setRange(int startRow, int startColumn, int endRow, int endColumn){
    start = new SCEDocumentPosition(startRow, startColumn);
    end = new SCEDocumentPosition(endRow, endColumn);
  }

  /**
   * Sets the range of the damage.
   *
   * @param start the start position.
   * @param end the end position.
   */
  public void setRange(SCEDocumentPosition start, SCEDocumentPosition end){
    if(start != null) this.start = new SCEDocumentPosition(start.getRow(), start.getColumn());
    if(end != null) this.end = new SCEDocumentPosition(end.getRow(), end.getColumn());
  }

  /**
   * Returns the text that was inserted/ removed or null otherwise.
   *
   * @return the text
   */
  public String getText(){
    return text;
  }

  /**
   * The text that was inserted/ removed.
   *
   * @param text the text
   */
  public void setText(String text){
    this.text = text;
  }
}

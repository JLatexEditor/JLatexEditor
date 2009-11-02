
/**
 * @author Jörg Endrullis
 */

package editor.component;

import java.awt.*;
import java.awt.font.TextAttribute;
import java.text.AttributedString;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

public class SCEDocument{
  // optimization
  private int capacityIncrement = 20;

  // text data
  private int rowsCount = 1;
  private SCEDocumentRow rows[] = null;

  // the avaliable attributes
  private Map stylesMap[] = null;

  // selection range
  private SCEDocumentPosition selectionStart = null;
  private SCEDocumentPosition selectionEnd = null;

  // limited edit range
  private SCEDocumentPosition editRangeStart = null;
  private SCEDocumentPosition editRangeEnd = null;

  // document listener
  private ArrayList documentListener = new ArrayList();

  /**
   * Creates a SCEDocument (the model SCEPane).
   */
  public SCEDocument(){
    // initialize text data
    rowsCount = 1;
    rows = new SCEDocumentRow[capacityIncrement];
    for(int row_nr = 0; row_nr < rows.length; row_nr++){
      rows[row_nr] = new SCEDocumentRow();
      rows[row_nr].row_nr = row_nr;
    }

    // create an array for avaliable text styles
    stylesMap = new Map[256];
  }

  /**
   * Clears this document.
   */
  public void clear(){
    rowsCount = 1;
    for(int row_nr = 0; row_nr < rows.length; row_nr++) rows[row_nr].length = 0;
  }

  /**
   * Returns the number of rows in this document.
   *
   * @return the number of rows
   */
  public int getRowsCount(){
    return rowsCount;
  }

  /**
   * Returns the rows of the document.
   * Be carefull, this is internal representation!
   *
   * @return the rows
   */
  public SCEDocumentRow[] getRows(){
    return rows;
  }

  /**
   * Returns the length of a certain row.
   *
   * @param row_nr the row number
   * @return the length of the row
   */
  public int getRowLength(int row_nr){
    if(row_nr >= rowsCount) return 0;
    return rows[row_nr].length;
  }

  /**
   * Creates a character input stream for this document.
   *
   * @return the character input stream
   */
  public SCECharInputStream getSCECharInputStream(){
    return new SCECharInputStream.FromDocument(this);
  }

  /**
   * Returns the row as string.
   *
   * @param row_nr the row number
   * @return the string
   */
  public String getRow(int row_nr){
    if(row_nr >= rowsCount) return "";
    return rows[row_nr].toString();
  }

  /**
   * Sets the text of this document.
   *
   * @param text the text
   */
  public void setText(String text){
    clear();
    insert(text, 0, 0);
  }

  /**
   * Returns the text in this document.
   */
  public String getText(){
    StringBuffer textBuffer = new StringBuffer(getRow(0));
    for(int row_nr = 1; row_nr < rowsCount; row_nr++){
      textBuffer.append("\n");
      textBuffer.append(getRow(row_nr));
    }
    return textBuffer.toString();
  }

  /**
   * Returns the text between start and end.
   *
   * @param start the start position
   * @param end the end position
   * @return the text between start and end
   */
  public String getText(SCEDocumentPosition start, SCEDocumentPosition end){
    String text = "";

    int startRow = start.getRow();
    int endRow = end.getRow();
    if(startRow == endRow){
      text = getRow(startRow).substring(start.getColumn(), end.getColumn());
    } else{
      text = getRow(startRow).substring(start.getColumn());
      for(int row_nr = startRow + 1; row_nr < endRow; row_nr++){
        text += "\n" + getRow(row_nr);
      }
      text += "\n" + getRow(endRow).substring(0, end.getColumn());
    }

    return text;
  }

  /**
   * Returns a document position.
   *
   * @param row_nr the row
   * @param column_nr the column
   * @return document position
   */
  public SCEDocumentPosition createDocumentPosition(int row_nr, int column_nr){
    // if the document is empty
    if(rowsCount <= 1 && rows[0].length == 0) return new SCEDocumentPosition(0,0);

    // wrong position?
    if(row_nr >= rowsCount) row_nr = rowsCount - 1;
    if(column_nr <= 0) return new SCEDocumentPosition(rows[row_nr], column_nr);
    if(column_nr >= rows[row_nr].length){
      return new SCEDocumentPosition(rows[row_nr], column_nr - rows[row_nr].length + 1);
    }

    return new SCEDocumentPosition(rows[row_nr].chars[column_nr], 0, 0);
  }

  /**
   * Sets the document position to a new value.
   *
   * @param position the position to change
   * @param row_nr the row
   * @param column_nr the column
   */
  public void setDocuentPosition(SCEDocumentPosition position, int row_nr, int column_nr){
    position.setPosition(rows[row_nr].chars[column_nr]);
  }

  /**
   * Updates the selection range of the document.
   *
   * @param start the selection start
   * @param end the selection end
   */
  public void setSelectionRange(SCEDocumentPosition start, SCEDocumentPosition end){
    if(start == null || end == null){
      selectionStart = null;
      selectionEnd = null;
      return;
    }

    if(start.getRow() < end.getRow() || (start.getRow() == end.getRow() && start.getColumn() < end.getColumn())){
      selectionStart = new SCEDocumentPosition(start.getRow(), start.getColumn());
      selectionEnd = new SCEDocumentPosition(end.getRow(), end.getColumn());
    } else{
      selectionStart = new SCEDocumentPosition(end.getRow(), end.getColumn());
      selectionEnd = new SCEDocumentPosition(start.getRow(), start.getColumn());
    }
  }

  /**
   * Checks, if there is a selection.
   *
   * @return true, if the document has a selection
   */
  public boolean hasSelection(){
    return selectionStart != null && selectionEnd != null;
  }

  /**
   * Returns the start of the selection.
   *
   * @return selection start
   */
  public SCEDocumentPosition getSelectionStart(){
    return selectionStart;
  }

  /**
   * Returns the end of the selection.
   *
   * @return selection end
   */
  public SCEDocumentPosition getSelectionEnd(){
    return selectionEnd;
  }

  /**
   * Returns the selected text as string.
   *
   * @return the selected text as string
   */
  public String getSelectedText(){
    if(!hasSelection()) return null;
    return getText(selectionStart, selectionEnd);
  }

  /**
   * Sets a limited edit range for the document.
   *
   * @param start the edit range start
   * @param end the edit range end
   */
  public void setEditRange(SCEDocumentPosition start, SCEDocumentPosition end){
    SCEDocumentEvent event = new SCEDocumentEvent();
    event.setEventType(SCEDocumentEvent.EVENT_EDITRANGE);

    if(start == null || end == null){
      editRangeStart = null;
      editRangeEnd = null;

      // Inform the listeners about the change (range is null)
      documentChanged(event);

      return;
    }

    editRangeStart = createDocumentPosition(start.getRow(), start.getColumn()-1);
    editRangeEnd = createDocumentPosition(end.getRow(), end.getColumn()+1);

    // Inform the listeners about the change
    event.setRange(start.getRow(), start.getColumn(), end.getRow(), end.getColumn());
    documentChanged(event);
  }

  /**
   * Checks, if the edit range is limited.
   *
   * @return true, if the document has a limited edit range
   */
  public boolean hasEditRange(){
    return editRangeStart != null && editRangeEnd != null;
  }

  /**
   * Returns the start of the edit range.
   *
   * @return selection start
   */
  public SCEDocumentPosition getEditRangeStart(){
    return new SCEDocumentPosition(editRangeStart.getRow(), editRangeStart.getColumn() + 1);
  }

  /**
   * Returns the end of the edit range.
   *
   * @return selection end
   */
  public SCEDocumentPosition getEditRangeEnd(){
    return new SCEDocumentPosition(editRangeEnd.getRow(), editRangeEnd.getColumn() - 1);
  }

  /**
   * Returns the text within the edit range.
   *
   * @return the text within the edit range
   */
  public String getEditRangeText(){
    if(!hasEditRange()) return null;
    return getText(getEditRangeStart(), getEditRangeEnd());
  }

  /**
   * Adds a style to the document.
   *
   * @param id the identifiert for this style
   * @param parentStyle the parent style of this style
   * @return a style map
   */
  public HashMap addStyle(byte id, Map parentStyle){
    HashMap style = new HashMap();
    if(parentStyle != null) style.putAll(parentStyle);

    stylesMap[id] = style;

    return style;
  }

  /**
   * Returns the row as attributed string.
   *
   * @param row_nr the row number
   * @return the attributed test
   */
  public synchronized AttributedString getRowAttributed(int row_nr){
    if(row_nr >= rowsCount) return null;

    // create the attributedString
    String string = getRow(row_nr);
    if(string == null || string.length() == 0) return null;
    AttributedString attributedString = new AttributedString(string);

    // add the attributes
    SCEDocumentRow row = rows[row_nr];

    for(int column_nr = 0; column_nr < row.length; column_nr++){
      int begin_index = column_nr;
      while(column_nr < row.length - 1 && row.chars[column_nr].style == row.chars[column_nr + 1].style) column_nr++;
      attributedString.addAttributes(stylesMap[row.chars[begin_index].style], begin_index, column_nr + 1);
    }

    // pay attention to selection
    if(hasSelection() && row_nr >= selectionStart.getRow() && row_nr <= selectionEnd.getRow()){
      for(int column_nr = 0; column_nr < row.length; column_nr++){
        if(row_nr == selectionStart.getRow() && column_nr < selectionStart.getColumn()) continue;
        if(row_nr == selectionEnd.getRow() && column_nr >= selectionEnd.getColumn()) continue;

        attributedString.addAttribute(TextAttribute.FOREGROUND, Color.WHITE, column_nr, column_nr + 1);
      }
    }

    return attributedString;
  }

  /**
   * Inserts text at the given position.
   *
   * @param text the text
   * @param row_nr the row
   * @param column_nr the column
   */
  public synchronized void insert(String text, int row_nr, int column_nr){
    // is this position within the edit range?
    if(hasEditRange()){
      SCEDocumentPosition position = new SCEDocumentPosition(row_nr, column_nr);
      if(position.compareTo(editRangeStart) <= 0 || position.compareTo(editRangeEnd) >= 0){
        // this is not allowed
        return;
      }
    }

    insert(text, row_nr, column_nr, SCEDocumentEvent.UPDATE_VIEW);
  }

  /**
   * Inserts text at the given position.
   *
   * @param text the text
   * @param row_nr the row
   * @param column_nr the column
   * @param eventID the event to tell the listeners (-1 if not to tell)
   */
  public synchronized void insert(String text, int row_nr, int column_nr, int eventID){
    if(row_nr >= rowsCount) return;

    // remember row and column
    SCEDocumentPosition position = new SCEDocumentPosition(row_nr, column_nr);

    // convert the text into a character array
    char charText[] = text.toCharArray();

    // is there a rowbreak within the text?
    if(text.indexOf('\n') == -1){
      insert(charText, 0, charText.length, row_nr, column_nr);
      column_nr += charText.length;
    } else{
      // count the newrow characters
      int newlineCount = 0;
      for(int char_nr = 0; char_nr < charText.length; char_nr++){
        if(charText[char_nr] == '\n') newlineCount++;
      }
      // insert some new rows
      insertRows(row_nr + 1, newlineCount);

      // special handling of first and last row
      insert(rows[row_nr].chars, column_nr, rows[row_nr].length - column_nr, row_nr + newlineCount, 0);
      rows[row_nr].length = column_nr;

      // insert row by row
      int textRowStart = 0;
      int textRowEnd = 0;
      while((textRowEnd = text.indexOf('\n', textRowStart)) != -1){
        insert(charText, textRowStart, textRowEnd - textRowStart, row_nr, column_nr);

        // go to next row
        textRowStart = textRowEnd + 1;
        row_nr++;
        column_nr = 0;
      }
      insert(charText, textRowStart, text.length() - textRowStart, row_nr, column_nr);
      column_nr += text.length() - textRowStart;
    }

    // Inform the listeners about the change
    if(eventID != -1){
      SCEDocumentEvent event = new SCEDocumentEvent();
      event.setEventType(eventID | SCEDocumentEvent.EVENT_INSERT);
      event.setRange(position.getRow(), position.getColumn(), row_nr, column_nr);
      event.setText(text);
      documentChanged(event);
    }
  }

  /**
   * Inserts the text (without '\n' character) at the given position.
   *
   * @param chars the text as document characters
   * @param offset the offset within the array
   * @param length the length the length of the data
   * @param row_nr the row
   * @param column_nr the column
   */
  private void insert(SCEDocumentChar[] chars, int offset, int length, int row_nr, int column_nr){
    if(chars == null || length <= 0) return;

    prepereInsert(row_nr, column_nr, length);
    // insert new data
    rows[row_nr].setCharacters(chars, offset, length, column_nr);

    // the row has been modified
    setModified(row_nr);
  }

  /**
   * Inserts the text (without '\n' character) at the given position.
   *
   * @param charText the text as char array
   * @param offset the offset within the array
   * @param length the length the length of the data
   * @param row_nr the row
   * @param column_nr the column
   */
  private void insert(char[] charText, int offset, int length, int row_nr, int column_nr){
    if(charText == null || length <= 0) return;

    prepereInsert(row_nr, column_nr, length);
    // insert new data
    rows[row_nr].setCharacters(charText, offset, length, column_nr);

    // the row has been modified
    setModified(row_nr);
  }

  /**
   * Prepers a insert by shifting the data.
   *
   * @param row_nr the row
   * @param column_nr the column
   * @param length the length
   */
  private void prepereInsert(int row_nr, int column_nr, int length){
    // do we have enough space left?
    if(rows[row_nr].length + length >= rows[row_nr].chars.length){
      rows[row_nr].increaseMaxCharacters(length + capacityIncrement);
    }

    // shift the data
    rows[row_nr].moveCharacters(column_nr, column_nr + length);
  }

  /**
   * Removes text from the document.
   *
   * @param startRow the start row
   * @param startColumn the start column
   * @param endRow the end row
   * @param endColumn the end column (behind the last character to remove)
   */
  public void remove(int startRow, int startColumn, int endRow, int endColumn){
    // is this position within the edit range?
    if(hasEditRange()){
      SCEDocumentPosition startPosition = new SCEDocumentPosition(startRow, startColumn);
      SCEDocumentPosition endPosition = new SCEDocumentPosition(endRow, endColumn);

      if(startPosition.compareTo(editRangeStart) <= 0 || endPosition.compareTo(editRangeEnd) >= 0){
        // this is not allowed
        return;
      }
    }

    remove(startRow, startColumn, endRow, endColumn, SCEDocumentEvent.UPDATE_VIEW);
  }

  /**
   * Removes text from the document.
   *
   * @param startRow the start row
   * @param startColumn the start column
   * @param endRow the end row
   * @param endColumn the end column (behind the last character to remove)
   * @param eventID the event to tell the listeners (-1 if not to tell)
   */
  public void remove(int startRow, int startColumn, int endRow, int endColumn, int eventID){
    // error handling = do nothing
    if(startRow < 0 || startColumn < 0) return;
    if(startRow > endRow || (startRow == endRow && startColumn > endColumn)) return;
    if(endRow >= rowsCount) return;
    if(startColumn > rows[startRow].length || endColumn > rows[endRow].length) return;

    // copy the text that will be removed
    SCEDocumentPosition start = new SCEDocumentPosition(startRow, startColumn);
    SCEDocumentPosition end = new SCEDocumentPosition(endRow, endColumn);
    String text = getText(start, end);

    // do we have enough space left?
    int length = rows[endRow].length - endColumn;
    if(startColumn + length >= rows[startRow].chars.length){
      rows[startRow].increaseMaxCharacters(length + capacityIncrement);
    }

    // insert the text of the last row into the first row
    rows[startRow].setCharacters(rows[endRow].chars, endColumn, length ,startColumn);
    rows[startRow].length = startColumn + rows[endRow].length - endColumn;
    // the row has been modified
    setModified(startRow);

    // remove the rows between
    int deleteCount = endRow - startRow;
    System.arraycopy(rows, endRow + 1, rows, startRow + 1, rowsCount - endRow - 1);
    rowsCount -= deleteCount;
    // recreate the doublicated rows
    for(int row_nr = rowsCount; row_nr < rowsCount + deleteCount; row_nr ++) rows[row_nr] = new SCEDocumentRow();
    // set the new row numbers
    for(int row_nr = startRow; row_nr < rowsCount + deleteCount; row_nr ++) rows[row_nr].row_nr = row_nr;

    // Inform the listeners about the change
    if(eventID != -1){
      SCEDocumentEvent event = new SCEDocumentEvent();
      event.setEventType(eventID | SCEDocumentEvent.EVENT_REMOVE);
      event.setRange(startRow, startColumn, endRow, endColumn);
      event.setText(text);
      documentChanged(event);
    }
  }

  /**
   * Sets the row propertie modiefied to true.
   *
   * @param row_nr the row
   */
  public void setModified(int row_nr){
    rows[row_nr].modified = true;
  }

  /**
   * Inserts a new row at the given positoin.
   *
   * @param row the row number
   */
  private void insertRows(int row, int count){
    // do we have enough space left?
    if(rowsCount + count >= rows.length) increaseMaxRows(count + capacityIncrement);

    // shift text data
    System.arraycopy(rows, row, rows, row + count, rowsCount - row);

    // init new rows with null
    for(int row_nr = row; row_nr < row + count; row_nr++) rows[row_nr] = new SCEDocumentRow();
    // increase rows count
    rowsCount += count;
    // new line numbers
    for(int row_nr = row; row_nr < rowsCount; row_nr++) rows[row_nr].row_nr = row_nr;
  }

  /**
   * Sets the style of the text.
   *
   * @param style the style id
   * @param row the row
   * @param column the column
   * @param length the number of characters to set
   */
  public void setStyle(byte style, int row, int column, int length){
    if(row >= rowsCount || column + length > rows[row].length) return;

    for(int column_nr = column; column_nr < column + length; column_nr++){
      rows[row].chars[column_nr].style = style;
    }
  }

  /**
   * Sets the style of the text.
   *
   * @param style the style id
   * @param startPosition the start position
   * @param endPosition the end position
   */
  public void setStyle(byte style, SCEDocumentPosition startPosition, SCEDocumentPosition endPosition){
    int rowStart = startPosition.getRow();
    int columnStart = startPosition.getColumn();
    int rowEnd = endPosition.getRow();
    int columnEnd = endPosition.getColumn();

    while(rowStart < rowEnd){
      setStyle(style, rowStart, columnStart, rows[rowStart].length - columnStart);
      rowStart++;
      columnStart = 0;
    }
    setStyle(style, rowStart, columnStart, columnEnd - columnStart);
  }

  /**
   * Allocate more rows.
   *
   * @param count the number of rows to add
   */
  private void increaseMaxRows(int count){
    SCEDocumentRow newRows[] = new SCEDocumentRow[rows.length + count];
    System.arraycopy(rows, 0, newRows, 0, rows.length);
    for(int row_nr = rows.length; row_nr < newRows.length; row_nr++){
      newRows[row_nr] = new SCEDocumentRow();
      newRows[row_nr].row_nr = row_nr;
    }
    rows = newRows;
  }

  /**
   * Informs the listeners about the document change.
   *
   * @param event the event
   */
  private void documentChanged(SCEDocumentEvent event){
    Iterator listenerIterator = documentListener.iterator();
    while(listenerIterator.hasNext()){
      SCEDocumentListener listener = (SCEDocumentListener) listenerIterator.next();
      listener.documentChanged(this, event);
    }
  }

  /**
   * Adds a document listener.
   *
   * @param listener the listener
   */
  public void addSCEDocumentListener(SCEDocumentListener listener){
    documentListener.add(listener);
  }

  /**
   * Removes a document listener.
   *
   * @param listener the listener
   */
  public void removeSCEDocumentListener(SCEDocumentListener listener){
    documentListener.remove(listener);
  }
}
package sce.component;

import sce.codehelper.WordWithPos;

import java.awt.font.TextAttribute;
import java.text.AttributedString;
import java.util.Map;

public class SCEDocumentRows {
  // optimization
  private int capacityIncrement = 20;

  private int rowsCount = 1;
  private SCEDocumentRow[] rows = null;

  // limited edit range
  private SCEDocumentPosition editRangeStart = null;
  private SCEDocumentPosition editRangeEnd = null;

  public SCEDocumentRows() {
    // initialize text data
    rowsCount = 1;
    rows = new SCEDocumentRow[capacityIncrement];
    for (int row_nr = 0; row_nr < rows.length; row_nr++) {
      rows[row_nr] = new SCEDocumentRow();
      rows[row_nr].row_nr = row_nr;
    }
  }

  /**
   * Clears this document.
   */
  public synchronized void clear() {
    rowsCount = 1;
    for (SCEDocumentRow row : rows) row.length = 0;
  }

  /**
   * Returns the number of rows.
   *
   * @return the number of rows
   */
  public int getRowsCount() {
    return rowsCount;
  }

  /**
   * Returns the actual rows.
   * Be carefull, this is internal representation!
   *
   * @return the rows
   */
  public synchronized SCEDocumentRow[] getRows() {
    SCEDocumentRow[] usedRows = new SCEDocumentRow[rowsCount];
    System.arraycopy(rows, 0, usedRows, 0, rowsCount);
    return usedRows;
  }

  /**
   * Returns the row.
   *
   * @param row_nr the row number
   * @return the row
   */
  public synchronized SCEDocumentRow getRow(int row_nr) {
    if (row_nr >= rowsCount) return null;
    return rows[row_nr];
  }

  /**
   * Returns the length of a certain row.
   *
   * @param row_nr the row number
   * @return the length of the row
   */
  public synchronized int getRowLength(int row_nr) {
    if (row_nr >= rowsCount) return 0;
    return rows[row_nr].length;
  }

  /**
   * Returns the row as string.
   *
   * @param row_nr the row number
   * @return the string
   */
  public synchronized String getRowAsString(int row_nr) {
    if (row_nr >= rowsCount) return "";
    return rows[row_nr].toString();
  }

  public synchronized String getRowAsString(int row_nr, int col_start, int col_end) {
    if (row_nr >= rowsCount) return "";
    return rows[row_nr].toString(col_start, col_end);
  }

  /**
   * Returns the text.
   *
   * @return text
   */
  public String getText() {
    SCEDocumentRow[] usedRows = getRows();

    StringBuffer textBuffer = new StringBuffer(getRowAsString(0));
    for (int row_nr = 1; row_nr < usedRows.length; row_nr++) {
      textBuffer.append("\n");
      textBuffer.append(usedRows[row_nr].toString());
    }
    return textBuffer.toString();
  }

  /**
   * Returns the text between start and end.
   *
   * @param start the start position
   * @param end   the end position
   * @return the text between start and end
   */
  public String getText(SCEPosition start, SCEPosition end) {
    SCEDocumentRow[] usedRows = getRows();

    int startRow = Math.min(start.getRow(), usedRows.length);
    int endRow = Math.min(end.getRow(), usedRows.length);

    StringBuffer textBuffer = new StringBuffer();

    if (startRow == endRow) {
      textBuffer.append(usedRows[startRow].toString(start.getColumn(), end.getColumn()));
    } else {
      textBuffer.append(usedRows[startRow].toString(start.getColumn(), usedRows[startRow].length));
      for (int row_nr = startRow + 1; row_nr < endRow; row_nr++) {
        textBuffer.append('\n');
        textBuffer.append(usedRows[row_nr].toString());
      }
      textBuffer.append('\n');
      textBuffer.append(usedRows[endRow].toString(0, end.getColumn()));
    }

    return textBuffer.toString();
  }

  /**
   * Returns a document position.
   *
   * @param row_nr    the row
   * @param column_nr the column
   * @return document position
   */
  public SCEDocumentPosition createDocumentPosition(int row_nr, int column_nr) {
    return createDocumentPosition(row_nr, column_nr, 0);
  }

  /**
   * Returns a document position.
   *
   * @param row_nr    the row
   * @param column_nr the column
   * @param rel_column relative column
   * @return document position
   */
  public synchronized SCEDocumentPosition createDocumentPosition(int row_nr, int column_nr, int rel_column) {
    // if the document is empty
    if (rowsCount <= 1 && rows[0].length == 0) return new SCEDocumentPosition(0, 0);

    // wrong position?
    if (row_nr >= rowsCount) row_nr = rowsCount - 1;

    SCEDocumentRow row = rows[row_nr];
    synchronized (row) {
      if (column_nr <= 0) return new SCEDocumentPosition(row, column_nr);
      if (column_nr >= row.length) {
        return new SCEDocumentPosition(row.chars[Math.max(0,row.length-1)], 0, 1);
      }

      return new SCEDocumentPosition(row.chars[column_nr], 0, rel_column);
    }
  }

  /**
   * Sets the document position to a new value.
   *
   * @param position  the position to change
   * @param row_nr    the row
   * @param column_nr the column
   */
  public synchronized void setDocumentPosition(SCEDocumentPosition position, int row_nr, int column_nr) {
    row_nr = Math.min(row_nr, rowsCount-1);
    position.setPosition(rows[row_nr].chars[column_nr]);
  }

  /**
   * Sets a limited edit range for the document.
   *
   * @param start the edit range start
   * @param end   the edit range end
   */
  public SCEDocumentEvent setEditRange(SCEPosition start, SCEPosition end, boolean undo) {
    SCEDocumentEvent event = new SCEDocumentEvent();
    event.setEventType(SCEDocumentEvent.EVENT_EDITRANGE | (undo ? SCEDocumentEvent.EVENT_UNDO : 0));

    if (start == null || end == null) {
      editRangeStart = null;
      editRangeEnd = null;

      return event;
    }

    editRangeStart = createDocumentPosition(start.getRow(), start.getColumn() - 1);
    editRangeEnd = createDocumentPosition(end.getRow(), end.getColumn() + 1);

    // Inform the listeners about the change
    event.setRange(start.getRow(), start.getColumn(), end.getRow(), end.getColumn());
    return event;
  }

  /**
   * Checks, if the edit range is limited.
   *
   * @return true, if the document has a limited edit range
   */
  public boolean hasEditRange() {
    return editRangeStart != null && editRangeEnd != null;
  }

  /**
   * Returns the start of the edit range.
   *
   * @return selection start
   */
  public SCEDocumentPosition getEditRangeStart() {
    return new SCEDocumentPosition(editRangeStart.getRow(), editRangeStart.getColumn() + 1);
  }

  /**
   * Returns the end of the edit range.
   *
   * @return selection end
   */
  public SCEDocumentPosition getEditRangeEnd() {
    return new SCEDocumentPosition(editRangeEnd.getRow(), editRangeEnd.getColumn() - 1);
  }

  /**
   * Returns the text within the edit range.
   *
   * @return the text within the edit range
   */
  public String getEditRangeText() {
    if (!hasEditRange()) return null;
    return getText(getEditRangeStart(), getEditRangeEnd());
  }

  /**
   * Returns the row as attributed string.
   *
   * @param row_nr the row number
   * @return the attributed test
   */
  public AttributedString getRowAttributed(
          int row_nr,
          SCEDocumentPosition selectionStart, SCEDocumentPosition selectionEnd,
          Map<? extends TextAttribute, ?>[] stylesMap)
  {
    SCEDocumentRow row;
    synchronized (this) {
      if (row_nr >= rowsCount) return null;
      row = rows[row_nr];
    }
    return row.getRowAttributed(selectionStart, selectionEnd, stylesMap);
  }

  public AttributedString getRowAttributed(
          int row_nr, int col_start, int col_end,
          SCEDocumentPosition selectionStart, SCEDocumentPosition selectionEnd,
          Map<? extends TextAttribute, ?>[] stylesMap)
  {
    SCEDocumentRow row;
    synchronized (this) {
      if (row_nr >= rowsCount) return null;
      row = rows[row_nr];
    }
    return row.getRowAttributed(col_start, col_end, selectionStart, selectionEnd, stylesMap);
  }

  /**
   * Sets the row property modified to true.
   *
   * @param row_nr the row
   */
  public synchronized void setModified(int row_nr) {
    if(row_nr > rowsCount) return;
    rows[row_nr].modified = true;
  }

  /**
   * Inserts text at the given position.
   *
   * @param text      the text
   * @param row_nr    the row
   * @param column_nr the column
   */
  public synchronized SCEDocumentPosition insert(String text, int row_nr, int column_nr, boolean checkEditRange) {
    if (row_nr >= rowsCount) return null;

    // remember row and column
    SCEDocumentPosition position = new SCEDocumentPosition(row_nr, column_nr);

    // is this position within the edit range?
    if (checkEditRange && hasEditRange()) {
      if (position.compareTo(editRangeStart) <= 0 || position.compareTo(editRangeEnd) >= 0) {
        // this is not allowed
        return null;
      }
    }

    // convert the text into a character array
    char charText[] = text.toCharArray();

    // is there a rowbreak within the text?
    if (text.indexOf('\n') == -1) {
      insert(charText, 0, charText.length, row_nr, column_nr);
      column_nr += charText.length;
    } else {
      // count the newrow characters
      int newlineCount = 0;
      for (char aCharText : charText) {
        if (aCharText == '\n') newlineCount++;
      }
      // insert some new rows
      insertRows(row_nr + 1, newlineCount);

      // special handling of first and last row
      insert(rows[row_nr].chars, column_nr, rows[row_nr].length - column_nr, row_nr + newlineCount, 0);
      rows[row_nr].length = column_nr;

      // insert row by row
      int textRowStart = 0;
      int textRowEnd = 0;
      while ((textRowEnd = text.indexOf('\n', textRowStart)) != -1) {
        insert(charText, textRowStart, textRowEnd - textRowStart, row_nr, column_nr);

        // go to next row
        textRowStart = textRowEnd + 1;
        row_nr++;
        column_nr = 0;
      }
      insert(charText, textRowStart, text.length() - textRowStart, row_nr, column_nr);
      column_nr += text.length() - textRowStart;
    }

    return new SCEDocumentPosition(row_nr, column_nr);
  }

  /**
   * Inserts the text (without '\n' character) at the given position.
   *
   * @param chars     the text as document characters
   * @param offset    the offset within the array
   * @param length    the length the length of the data
   * @param row_nr    the row
   * @param column_nr the column
   */
  private void insert(SCEDocumentChar[] chars, int offset, int length, int row_nr, int column_nr) {
    if (chars == null || length <= 0) return;

    prepereInsert(row_nr, column_nr, length);
    // insert new data
    rows[row_nr].setCharacters(chars, offset, length, column_nr);

    // the row has been modified
    setModified(row_nr);
  }

  /**
   * Inserts the text (without '\n' character) at the given position.
   *
   * @param charText  the text as char array
   * @param offset    the offset within the array
   * @param length    the length the length of the data
   * @param row_nr    the row
   * @param column_nr the column
   */
  private void insert(char[] charText, int offset, int length, int row_nr, int column_nr) {
    if (charText == null || length <= 0) return;

    prepereInsert(row_nr, column_nr, length);
    // insert new data
    rows[row_nr].setCharacters(charText, offset, length, column_nr);

    // the row has been modified
    setModified(row_nr);
  }

  /**
   * Allocate more rows.
   *
   * @param count the number of rows to add
   */
  private void increaseMaxRows(int count) {
    SCEDocumentRow newRows[] = new SCEDocumentRow[rows.length + count];
    System.arraycopy(rows, 0, newRows, 0, rows.length);
    for (int row_nr = rows.length; row_nr < newRows.length; row_nr++) {
      newRows[row_nr] = new SCEDocumentRow();
      newRows[row_nr].row_nr = row_nr;
    }
    rows = newRows;
  }

  /**
   * Inserts a new row at the given position.
   *
   * @param row   the row number
   * @param count number of rows to insert
   */
  private void insertRows(int row, int count) {
    // do we have enough space left?
    if (rowsCount + count >= rows.length) increaseMaxRows(count + capacityIncrement);

    // shift text data
    System.arraycopy(rows, row, rows, row + count, rowsCount - row);

    // init new rows with null
    for (int row_nr = row; row_nr < row + count; row_nr++) rows[row_nr] = new SCEDocumentRow();
    // increase rows count
    rowsCount += count;
    // new line numbers
    for (int row_nr = row; row_nr < rowsCount; row_nr++) rows[row_nr].row_nr = row_nr;
  }

  /**
   * Prepers a insert by shifting the data.
   *
   * @param row_nr    the row
   * @param column_nr the column
   * @param length    the length
   */
  private void prepereInsert(int row_nr, int column_nr, int length) {
    // do we have enough space left?
    if (rows[row_nr].length + length >= rows[row_nr].chars.length) {
      rows[row_nr].increaseMaxCharacters(length + capacityIncrement);
    }

    // shift the data
    rows[row_nr].moveCharacters(column_nr, column_nr + length);
  }

  /**
   * Removes text from the document.
   *
   * @param startRow    the start row
   * @param startColumn the start column
   * @param endRow      the end row
   * @param endColumn   the end column (behind the last character to remove)
   */
  public synchronized boolean remove(int startRow, int startColumn, int endRow, int endColumn, boolean checkEditRange) {
    SCEDocumentPosition start = new SCEDocumentPosition(startRow, startColumn);
    SCEDocumentPosition end = new SCEDocumentPosition(endRow, endColumn);

    // is this position within the edit range?
    if (checkEditRange && hasEditRange()) {
      if (start.compareTo(editRangeStart) <= 0 || end.compareTo(editRangeEnd) >= 0) {
        // this is not allowed
        return false;
      }
    }

    // error handling = do nothing
    if (startRow < 0 || startColumn < 0) return false;
    if (startRow > endRow || (startRow == endRow && startColumn > endColumn)) return false;
    if (endRow >= rowsCount) return false;
    if (startColumn > rows[startRow].length || endColumn > rows[endRow].length) return false;

    // do we have enough space left?
    int length = rows[endRow].length - endColumn;
    if (startColumn + length >= rows[startRow].chars.length) {
      rows[startRow].increaseMaxCharacters(length + capacityIncrement);
    }

    // insert the text of the last row into the first row
    rows[startRow].setCharacters(rows[endRow].chars, endColumn, length, startColumn);
    rows[startRow].length = startColumn + rows[endRow].length - endColumn;
    // the row has been modified
    setModified(startRow);

    // remove the rows between
    int deleteCount = endRow - startRow;
    System.arraycopy(rows, endRow + 1, rows, startRow + 1, rowsCount - endRow - 1);
    rowsCount -= deleteCount;
    // recreate the doublicated rows
    for (int row_nr = rowsCount; row_nr < rowsCount + deleteCount; row_nr++) rows[row_nr] = new SCEDocumentRow();
    // set the new row numbers
    for (int row_nr = startRow; row_nr < rowsCount + deleteCount; row_nr++) rows[row_nr].row_nr = row_nr;

    return true;
  }

  /**
   * Sets the style of the text.
   *
   * @param style  the style id
   * @param row    the row
   * @param column the column
   * @param length the number of characters to set
   */
  public void setStyle(byte style, int row, int column, int length) {
    if (row >= rowsCount || column + length > rows[row].length) return;

    for (int column_nr = column; column_nr < column + length; column_nr++) {
      rows[row].chars[column_nr].style = style;
    }
  }

	/**
	 * Sets the style for a certain word.
	 *
	 * @param style style
	 * @param wordWithPos word with position information
	 */
	public void setStyle(byte style, WordWithPos wordWithPos) {
		setStyle(style, wordWithPos.getStartPos(), wordWithPos.getEndPos());
	}

  /**
   * Sets the style of the text.
   *
   * @param style         the style id
   * @param startPosition the start position
   * @param endPosition   the end position
   */
  public void setStyle(byte style, SCEPosition startPosition, SCEPosition endPosition) {
    int rowStart = startPosition.getRow();
    int columnStart = startPosition.getColumn();
    int rowEnd = endPosition.getRow();
    int columnEnd = endPosition.getColumn();

    while (rowStart < rowEnd) {
      setStyle(style, rowStart, columnStart, rows[rowStart].length - columnStart);
      rowStart++;
      columnStart = 0;
    }
    setStyle(style, rowStart, columnStart, columnEnd - columnStart);
  }
}

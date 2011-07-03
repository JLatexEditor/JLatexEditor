/**
 * @author Jörg Endrullis
 */

package sce.component;

import jlatexeditor.gproperties.GProperties;
import org.jetbrains.annotations.Nullable;

import java.awt.*;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.StringSelection;
import java.awt.font.TextAttribute;
import java.text.AttributedString;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;

public class SCEDocument {
  // text data
  private SCEDocumentRows rows;

  // the available attributes
  private Map<? extends TextAttribute, ?>[] stylesMap = null;

  // selection range
  private SCEDocumentPosition selectionStart = null;
  private SCEDocumentPosition selectionEnd = null;

  // editable?
  private boolean editable = true;

  // document listeners
  private ArrayList<SCEDocumentListener> documentListeners = new ArrayList<SCEDocumentListener>();
  // modification state listeners
  private ArrayList<SCEModificationStateListener> modificationStateListeners = new ArrayList<SCEModificationStateListener>();
  // selection listeners
  private ArrayList<SCESelectionListener> selectionListeners = new ArrayList<SCESelectionListener>();

  // Has the document been modified since the last save?
  private boolean modified = false;

	private Clipboard selectionClipboard = Toolkit.getDefaultToolkit().getSystemSelection();

  /**
   * Creates a SCEDocument (the model SCEPane).
   */
  public SCEDocument() {
    // initialize text data
    rows = new SCEDocumentRows();

    // create an array for available text styles and add default styles
    stylesMap = new Map[256];
    addDefaultStyles();
  }

  private void addDefaultStyles() {
    Font font = GProperties.getEditorFont();

    // Text
    Map<TextAttribute, Object> styleText = addStyle((byte) 0, null);
    styleText.put(TextAttribute.FONT, font);
    styleText.put(TextAttribute.FOREGROUND, Color.BLACK);
  }

  /**
   * Clears this document.
   */
  public void clear() {
    rows.clear();
  }

  /**
   * Returns the rows in this document.
   *
   * @return the rows
   */
  public SCEDocumentRows getRowsModel() {
    return rows;
  }

  /**
   * Creates a character input stream for this document.
   *
   * @return the character input stream
   */
  public SCECharInputStream getSCECharInputStream() {
    return new SCECharInputStream.FromDocument(this);
  }

  /**
   * Sets the text of this document.
   *
   * @param text the text
   */
  public void setText(String text) {
    // treat Windows line breaks
    if (text.indexOf((char) 0x0D) != -1) {
      StringBuilder builder = new StringBuilder(text.length());
      int lastIndex = 0, index = -1;
      while ((index = text.indexOf((char) 0x0D, index + 1)) != -1) {
        builder.append(text.substring(lastIndex, index));
        lastIndex = index + 1;
      }
      builder.append(text.substring(lastIndex));
      text = builder.toString();
    }

    clear();
    boolean editable_ = editable;
    editable = true;
    insert(text, 0, 0);
    editable = editable_;
  }

  /**
   * Returns the text in this document.
   *
   * @return text of this document
   */
  public String getText() {
    return rows.getText();
  }

  /**
   * Returns the text between start and end.
   *
   * @param start the start position
   * @param end   the end position
   * @return the text between start and end
   */
  public String getText(SCEPosition start, SCEPosition end) {
    return rows.getText(start, end);
  }

	/**
	 * Returns a document position.
	 *
	 * @param row_nr    the row
	 * @param column_nr the column
	 * @return document position
	 */
	public SCEDocumentPosition createDocumentPosition(int row_nr, int column_nr) {
		return rows.createDocumentPosition(row_nr, column_nr);
	}

  /**
   * Returns a document position.
   *
   * @param row_nr    the row
   * @param column_nr the column
   * @param rel_column relative column
   * @return document position
   */
  public SCEDocumentPosition createDocumentPosition(int row_nr, int column_nr, int rel_column) {
    return rows.createDocumentPosition(row_nr, column_nr, rel_column);
  }

  /**
   * Sets the document position to a new value.
   *
   * @param position  the position to change
   * @param row_nr    the row
   * @param column_nr the column
   */
  public void setDocumentPosition(SCEDocumentPosition position, int row_nr, int column_nr) {
    rows.setDocumentPosition(position, row_nr, column_nr);
  }

  /**
   * Updates the selection range of the document.
   *
   * @param range selection range
   */
  public void setSelectionRange(SCERange range) {
    setSelectionRange(range.getStartPos(), range.getEndPos());
  }

  /**
   * Updates the selection range of the document.
   *
   * @param start the selection start
   * @param end   the selection end
   */
  public void setSelectionRange(@Nullable SCEDocumentPosition start, @Nullable SCEDocumentPosition end) {
    if (start == null || end == null) {
      selectionStart = null;
      selectionEnd = null;
      selectionChanged();
      return;
    }

    if (start.getRow() < end.getRow() || (start.getRow() == end.getRow() && start.getColumn() < end.getColumn())) {
      selectionStart = new SCEDocumentPosition(start.getRow(), start.getColumn());
      selectionEnd = new SCEDocumentPosition(end.getRow(), end.getColumn());
    } else {
      selectionStart = new SCEDocumentPosition(end.getRow(), end.getColumn());
      selectionEnd = new SCEDocumentPosition(start.getRow(), start.getColumn());
    }
    selectionChanged();
  }

  /**
   * Clears the selection.
   */
  public void clearSelection() {
    setSelectionRange(null, null);
  }

  /**
   * Checks, if there is a selection.
   *
   * @return true, if the document has a selection
   */
  public boolean hasSelection() {
    return selectionStart != null && selectionEnd != null;
  }

  /**
   * Returns the start of the selection.
   *
   * @return selection start
   */
  public SCEDocumentPosition getSelectionStart() {
    return selectionStart;
  }

  /**
   * Returns the end of the selection.
   *
   * @return selection end
   */
  public SCEDocumentPosition getSelectionEnd() {
    return selectionEnd;
  }

  /**
   * Returns the selected text as string.
   *
   * @return the selected text as string
   */
  public String getSelectedText() {
    if (!hasSelection()) return null;
    return getText(selectionStart, selectionEnd);
  }

  /**
   * Sets a limited edit range for the document.
   *
   * @param start the edit range start
   * @param end   the edit range end
   */
  public void setEditRange(SCEDocumentPosition start, SCEDocumentPosition end, boolean undo) {
    documentChanged(rows.setEditRange(start, end, undo));
  }

  /**
   * Checks, if the edit range is limited.
   *
   * @return true, if the document has a limited edit range
   */
  public boolean hasEditRange() {
    return rows.hasEditRange();
  }

  /**
   * Returns the start of the edit range.
   *
   * @return selection start
   */
  public SCEDocumentPosition getEditRangeStart() {
    return rows.getEditRangeStart();
  }

  /**
   * Returns the end of the edit range.
   *
   * @return selection end
   */
  public SCEDocumentPosition getEditRangeEnd() {
    return rows.getEditRangeEnd();
  }

  /**
   * Returns the text within the edit range.
   *
   * @return the text within the edit range
   */
  public String getEditRangeText() {
    return rows.getEditRangeText();
  }

  /**
   * Adds a style to the document.
   *
   * @param id          the identifier for this style
   * @param parentStyle the parent style of this style
   * @return a style map
   */
  public HashMap<TextAttribute, Object> addStyle(byte id, Map<TextAttribute, Object> parentStyle) {
    HashMap<TextAttribute, Object> style = new HashMap<TextAttribute, Object>();
    if (parentStyle != null) style.putAll(parentStyle);

    stylesMap[id] = style;

    return style;
  }

  /**
   * Returns the row as attributed string.
   *
   * @param row_nr the row number
   * @return the attributed test
   */
  public AttributedString getRowAttributed(int row_nr) {
    return rows.getRowAttributed(row_nr, selectionStart, selectionEnd, stylesMap);
  }

  public AttributedString getRowAttributed(int row_nr, int col_start, int col_end) {
    return rows.getRowAttributed(row_nr, col_start, col_end, selectionStart, selectionEnd, stylesMap);
  }

  /**
   * Inserts text at the given position.
   *
   * @param text      the text
   * @param row_nr    the row
   * @param column_nr the column
   */
  public synchronized void insert(String text, int row_nr, int column_nr) {
    insert(text, row_nr, column_nr, SCEDocumentEvent.UPDATE_VIEW, true);
  }

  /**
   * Inserts text at the given position.
   *
   * @param text      the text
   * @param row_nr    the row
   * @param column_nr the column
   * @param eventID   the event to tell the listeners (-1 if not to tell)
   */
  public synchronized void insert(String text, int row_nr, int column_nr, int eventID, boolean checkEditRange) {
    if (!editable) return;

    // remember row and column
    SCEDocumentPosition position = new SCEDocumentPosition(row_nr, column_nr);

    SCEDocumentPosition endPosition = rows.insert(text, row_nr, column_nr, checkEditRange);
    if(endPosition == null) return;

    // Inform the listeners about the change
    if (eventID != -1) {
      SCEDocumentEvent event = new SCEDocumentEvent();
      event.setEventType(eventID | SCEDocumentEvent.EVENT_INSERT);
      event.setRange(position.getRow(), position.getColumn(), endPosition.getRow(), endPosition.getColumn());
      event.setText(text);
      documentChanged(event);
    }
  }

  public void remove(SCEPosition start, SCEPosition end) {
    remove(start.getRow(), start.getColumn(), end.getRow(), end.getColumn());
  }

  /**
   * Removes text from the document.
   *
   * @param startRow    the start row
   * @param startColumn the start column
   * @param endRow      the end row
   * @param endColumn   the end column (behind the last character to remove)
   */
  public void remove(int startRow, int startColumn, int endRow, int endColumn) {
    remove(startRow, startColumn, endRow, endColumn, SCEDocumentEvent.UPDATE_VIEW, true);
  }

  /**
   * Removes text from the document.
   *
   * @param startRow    the start row
   * @param startColumn the start column
   * @param endRow      the end row
   * @param endColumn   the end column (behind the last character to remove)
   * @param eventID     the event to tell the listeners (-1 if not to tell)
   */
  public void remove(int startRow, int startColumn, int endRow, int endColumn, int eventID, boolean checkEditRange) {
    if (!editable) return;

    // copy the text that will be removed
    SCEDocumentPosition start = new SCEDocumentPosition(startRow, startColumn);
    SCEDocumentPosition end = new SCEDocumentPosition(endRow, endColumn);
    String text = getText(start, end);

    if(!rows.remove(startRow, startColumn, endRow, endColumn, checkEditRange)) return;

    // Inform the listeners about the change
    if (eventID != -1) {
      SCEDocumentEvent event = new SCEDocumentEvent();
      event.setEventType(eventID | SCEDocumentEvent.EVENT_REMOVE);
      event.setRange(startRow, startColumn, endRow, endColumn);
      event.setText(text);
      documentChanged(event);
    }
  }

	/**
	 * Removes the selected text from the document.
	 *
	 * @return true if there was a selection to remove
	 */
	public boolean removeSelection() {
	  if (selectionStart == null || selectionEnd == null) return false;

	  remove(selectionStart, selectionEnd);
		return true;
	}

  /**
   * Replaces the text between the given positions with the new text.
   *
   * @param startRow    the start row
   * @param startColumn the start column
   * @param endRow      the end row
   * @param endColumn   the end column (behind the last character to remove)
   * @param text        new text
   */
  public void replace(int startRow, int startColumn, int endRow, int endColumn, String text) {
    remove(startRow, startColumn, endRow, endColumn);
    insert(text, startRow, startColumn);
  }

  public void replace(SCEPosition start, SCEPosition end, String text) {
    int startRow = start.getRow();
    int startColumn = start.getColumn();
    remove(start, end);
    insert(text, startRow, startColumn);
  }

  public void remove(SCERange range) {
    remove(range.getStartRow(), range.getStartCol(), range.getEndRow(), range.getEndCol());
  }

  /**
   * Sets if this document should be editable.
   *
   * @param editable true, if editable
   */
  public void setEditable(boolean editable) {
    this.editable = editable;
  }

  /**
   * Returns if the text document is editable.
   *
   * @return true, if editable
   */
  public boolean isEditable() {
    return editable;
  }

  public boolean isModified() {
    return modified;
  }

  public void setModified(boolean modified) {
    if (this.modified != modified) {
      this.modified = modified;
      for (SCEModificationStateListener modificationStateListener : modificationStateListeners) {
        modificationStateListener.modificationStateChanged(modified);
      }
    }
  }

  /**
   * Informs the listeners about the document change.
   *
   * @param event the event
   */
  private void documentChanged(SCEDocumentEvent event) {
    setModified(true);
    for (SCEDocumentListener documentListener : documentListeners) {
      documentListener.documentChanged(this, event);
    }
  }

  /**
   * Adds a document listener.
   *
   * @param listener the listener
   */
  public void addSCEDocumentListener(SCEDocumentListener listener) {
    documentListeners.add(listener);
  }

  /**
   * Removes a document listener.
   *
   * @param listener the listener
   */
  public void removeSCEDocumentListener(SCEDocumentListener listener) {
    documentListeners.remove(listener);
  }

  /**
   * Informs the listeners about the selection change.
   */
  private void selectionChanged() {
	  // update selection clipboard
	  if (selectionStart != null && selectionEnd != null && !selectionStart.equals(selectionEnd) && selectionClipboard != null) {
		  StringSelection data = new StringSelection(getSelectedText());
		  selectionClipboard.setContents(data, data);
	  }

	  // inform listeners
    for (SCESelectionListener selectionListener : selectionListeners) {
      selectionListener.selectionChanged(this, selectionStart, selectionEnd);
    }
  }

  /**
   * Adds a selection listener.
   *
   * @param listener the listener
   */
  public void addSCESelectionListener(SCESelectionListener listener) {
    selectionListeners.add(listener);
  }

  /**
   * Removes a selection listener.
   *
   * @param listener the listener
   */
  public void removeSCESelectionListener(SCESelectionListener listener) {
    selectionListeners.remove(listener);
  }

  /**
   * Adds a modification state listener.
   *
   * @param listener the listener
   */
  public void addSCEModificationStateListener(SCEModificationStateListener listener) {
    modificationStateListeners.add(listener);
  }

  /**
   * Removes a modification state listener.
   *
   * @param listener the listener
   */
  public void removeSCEModificationStateListener(SCEModificationStateListener listener) {
    modificationStateListeners.remove(listener);
	}

	public void replaceInAllRows(String from, String to) {
		Pattern fromPattern = Pattern.compile(from);

		// replace all occurrences in all rows
    SCEDocumentRow[] rows = this.rows.getRows();
		for (int i = 0; i < rows.length; i++) {
			String rowString = rows[i].toString();
			if (fromPattern.matcher(rowString).find()) {
				String newRowString = rowString.replaceAll(from, to);
				replace(i, 0, i, rowString.length(), newRowString);
			}
		}
	}

  public synchronized void replaceAll(String fromRegex, String toRegex) {
    setText(getText().replaceAll(fromRegex, toRegex));
  }
}
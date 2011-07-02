/**
 * @author Jörg Endrullis
 */

package sce.component;

import sce.syntaxhighlighting.ParserStateStack;

import java.awt.*;
import java.awt.font.TextAttribute;
import java.text.AttributedString;
import java.util.Map;

public class SCEDocumentRow {
  // the characters and length of the row
  public SCEDocumentChar chars[] = new SCEDocumentChar[0];
  public int length = 0;
  // the row_nr (position in the document)
  public int row_nr = 0;

  // the tokens (result of lexer parsing)
  public ParserStateStack parserStateStack = null;
  // was the line modified (since last parsing)
  public boolean modified = true;

  /**
   * Allocate more characters for a row.
   *
   * @param count the number of characters
   */
  public void increaseMaxCharacters(int count) {
    SCEDocumentChar new_chars[] = new SCEDocumentChar[length + count];
    System.arraycopy(chars, 0, new_chars, 0, chars.length);
    for (int column_nr = chars.length; column_nr < new_chars.length; column_nr++) {
      new_chars[column_nr] = new SCEDocumentChar();
      new_chars[column_nr].column_nr = column_nr;
      new_chars[column_nr].row = this;
    }
    chars = new_chars;
  }

  /**
   * Moves the characters behind src position to dest position.
   *
   * @param src  the source offset
   * @param dest the destination offset
   */
  public void moveCharacters(int src, int dest) {
    int move_length = length - src;

    System.arraycopy(chars, src, chars, dest, move_length);
    if (src < dest) {
      for (int column_nr = src; column_nr < dest; column_nr++) {
        chars[column_nr] = new SCEDocumentChar();
        chars[column_nr].row = this;
      }
      for (int column_nr = src; column_nr < dest + move_length; column_nr++) chars[column_nr].column_nr = column_nr;
    } else {
      for (int column_nr = dest + move_length; column_nr < src + move_length; column_nr++) {
        chars[column_nr] = new SCEDocumentChar();
        chars[column_nr].row = this;
      }
      for (int column_nr = dest; column_nr < src + move_length; column_nr++) chars[column_nr].column_nr = column_nr;
    }

    length = dest + move_length;
  }

  /**
   * Sets some characters within the row.
   *
   * @param text        the array to take from
   * @param src         the offset within the array
   * @param move_length the number of chars to copy
   * @param dest        the column number (where to put the chars)
   */
  public void setCharacters(SCEDocumentChar text[], int src, int move_length, int dest) {
    for (int char_nr = 0; char_nr < move_length; char_nr++) {
      chars[dest + char_nr] = text[src + char_nr];
      chars[dest + char_nr].row = this;
      chars[dest + char_nr].column_nr = dest + char_nr;
    }
  }

  /**
   * Sets some characters within the row.
   *
   * @param charText    the array to take from
   * @param src         the offset within the array
   * @param move_length the number of chars to copy
   * @param dest        the column number (where to put the chars)
   */
  public void setCharacters(char charText[], int src, int move_length, int dest) {
    for (int char_nr = 0; char_nr < move_length; char_nr++) {
      chars[dest + char_nr].character = charText[src + char_nr];
      chars[dest + char_nr].style = 0;
    }
  }

  /**
   * Converts the row into a char array.
   *
   * @return char array
   */
  public char[] toCharArray() {
    return toCharArray(0, length);
  }

  public synchronized char[] toCharArray(int col_start, int col_end) {
    col_end = Math.min(col_end, length);
    if (col_end <= col_start) return new char[0];

    char textChars[] = new char[col_end - col_start];
    for (int column_nr = col_start; column_nr < col_end; column_nr++) {
      textChars[column_nr - col_start] = chars[column_nr].character;
    }
    return textChars;
  }

  /**
   * Returns the row as attributed string.
   *
   * @return the attributed test
   */
  public synchronized AttributedString getRowAttributed(
          SCEDocumentPosition selectionStart, SCEDocumentPosition selectionEnd,
          Map<? extends TextAttribute, ?>[] stylesMap)
  {
    return getRowAttributed(0, length, selectionStart, selectionEnd, stylesMap);
  }

  public synchronized AttributedString getRowAttributed(
          int col_start, int col_end,
          SCEDocumentPosition selectionStart, SCEDocumentPosition selectionEnd,
          Map<? extends TextAttribute, ?>[] stylesMap)
  {
    col_end = Math.min(col_end, length);
    if (col_start >= col_end) return null;

    // create the attributedString
    String string = toString(col_start, col_end);
    if (string == null || string.length() == 0) return null;
    AttributedString attributedString = new AttributedString(string);

    // add the attributes
    for (int column_nr = col_start; column_nr < col_end; column_nr++) {
      int begin_index = column_nr - col_start;
      while (column_nr < col_end - 1 &&
              chars[column_nr].style == chars[column_nr + 1].style &&
              chars[column_nr].overlayStyle == chars[column_nr + 1].overlayStyle) column_nr++;
      int end_index = column_nr + 1 - col_start;
      attributedString.addAttributes(stylesMap[chars[begin_index + col_start].style], begin_index, end_index);
      attributedString.addAttributes(stylesMap[chars[begin_index + col_start].overlayStyle], begin_index, end_index);
    }

    // pay attention to selection
    boolean hasSelection = selectionStart != null && selectionEnd != null;
    if (hasSelection && row_nr >= selectionStart.getRow() && row_nr <= selectionEnd.getRow()) {
      for (int column_nr = col_start; column_nr < col_end; column_nr++) {
        if (row_nr == selectionStart.getRow() && column_nr < selectionStart.getColumn()) continue;
        if (row_nr == selectionEnd.getRow() && column_nr >= selectionEnd.getColumn()) continue;

        // background must be set here too to override other background colors
        attributedString.addAttribute(TextAttribute.BACKGROUND, SCEPane.selectionHighlightColor, column_nr - col_start, column_nr + 1 - col_start);
        attributedString.addAttribute(TextAttribute.FOREGROUND, Color.WHITE, column_nr - col_start, column_nr + 1 - col_start);
      }
    }

    return attributedString;
  }

  /**
   * Converts the row into a string.
   *
   * @return the string
   */
  public String toString() {
    return new String(toCharArray());
  }

  public String toString(int col_start, int col_end) {
    return new String(toCharArray(col_start, col_end));
  }
}

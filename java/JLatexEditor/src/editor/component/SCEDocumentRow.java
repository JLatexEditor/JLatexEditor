
/**
 * @author Jörg Endrullis
 */

package editor.component;

import editor.syntaxhighlighting.ParserStateStack;

public class SCEDocumentRow{
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
  public void increaseMaxCharacters(int count){
    SCEDocumentChar new_chars[] = new SCEDocumentChar[length + count];
    System.arraycopy(chars, 0, new_chars, 0, chars.length);
    for(int column_nr = chars.length; column_nr < new_chars.length; column_nr++){
      new_chars[column_nr] = new SCEDocumentChar();
      new_chars[column_nr].column_nr = column_nr;
      new_chars[column_nr].row = this;
    }
    chars = new_chars;
  }

  /**
   * Moves the characters behind src position to dest position.
   *
   * @param src the source offset
   * @param dest the destination offset
   */
  public void moveCharacters(int src, int dest){
    int move_length = length - src;

    System.arraycopy(chars, src, chars, dest, move_length);
    if(src < dest){
      for(int column_nr = src; column_nr < dest; column_nr++){
        chars[column_nr] = new SCEDocumentChar();
        chars[column_nr].row = this;
      }
      for(int column_nr = src; column_nr < dest + move_length; column_nr++) chars[column_nr].column_nr = column_nr;
    }else{
      for(int column_nr = dest + move_length; column_nr < src + move_length; column_nr++){
        chars[column_nr] = new SCEDocumentChar();
        chars[column_nr].row = this;
      }
      for(int column_nr = dest; column_nr < src + move_length; column_nr++) chars[column_nr].column_nr = column_nr;
    }

    length = dest + move_length;
  }

  /**
   * Sets some characters within the row.
   *
   * @param text the array to take from
   * @param src the offset within the array
   * @param move_length the number of chars to copy
   * @param dest the column number (where to put the chars)
   */
  public void setCharacters(SCEDocumentChar text[], int src, int move_length, int dest){
    for(int char_nr = 0; char_nr <  move_length; char_nr ++){
      chars[dest + char_nr] = text[src + char_nr];
      chars[dest + char_nr].row = this;
      chars[dest + char_nr].column_nr = dest + char_nr;
    }
  }

  /**
   * Sets some characters within the row.
   *
   * @param charText the array to take from
   * @param src the offset within the array
   * @param move_length the number of chars to copy
   * @param dest the column number (where to put the chars)
   */
  public void setCharacters(char charText[], int src, int move_length, int dest){
    for(int char_nr = 0; char_nr <  move_length; char_nr ++){
      chars[dest + char_nr].character = charText[src + char_nr];
      chars[dest + char_nr].style = 0;
    }
  }

  /**
   * Converts the row into a char array.
   *
   * @return char array
   */
  public char[] toCharArray(){
    char textChars[] = new char[length];
    for(int column_nr = 0; column_nr < length; column_nr++){
      textChars[column_nr] = chars[column_nr].character;
    }
    return textChars;
  }

  /**
   * Converts the row into a string.
   *
   * @return the string
   */
  public String toString(){
    return new String(toCharArray());
  }
}

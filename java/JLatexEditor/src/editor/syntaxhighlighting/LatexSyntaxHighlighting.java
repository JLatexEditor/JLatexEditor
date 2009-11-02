
/**
 * @author Jörg Endrullis
 */

package editor.syntaxhighlighting;

import editor.component.*;

public class LatexSyntaxHighlighting extends SyntaxHighlighting implements SCEDocumentListener{
  // text pane and document
  private SCEPane pane = null;
  private SCEDocument document = null;

  // do we need to parse
  private boolean parseNeeded = false;
  private boolean currentlyChanging = false;

  public LatexSyntaxHighlighting(SCEPane pane){
    this.pane = pane;
    document = pane.getDocument();
    document.addSCEDocumentListener(this);

    // initialize
    reset();

    // set the thread priority to min
    setPriority(Thread.MIN_PRIORITY);
  }

  /**
   * Resets the parser/ syntax highlighting to initial state.
   */
  public void reset(){
    // get the actual document rows
    int rowsCount = document.getRowsCount();
    SCEDocumentRow rows[] = document.getRows();

    // reset all parser states and mark rows as modified
    for(int row_nr = 0; row_nr < rowsCount; row_nr++){
      rows[row_nr].modified = true;
      rows[row_nr].parserStateStack = null;
    }

    // initialize the first row with parser state
    rows[0].parserStateStack = new ParserStateStack();
    rows[0].parserStateStack.push(new ParserState());
  }

  public void run(){
    while(!isInterrupted()){
      // sleep a short time
      try{
        sleep(100);
      } catch(InterruptedException e){
        continue;
      }

      // only parse, if the user does not edit the text
      if(!parseNeeded || currentlyChanging){
        currentlyChanging = false;
        continue;
      }
      parseNeeded = false;

      // parse the document
      try{
        parse();
      }catch(RuntimeException e){
        // internal parser error (should never happen)
        e.printStackTrace();
      }

      pane.repaint();
    }
  }

  /**
   * Parse and highlight the document.
   */
  private void parse(){
    // get the actual document rows
    int rowsCount = document.getRowsCount();
    SCEDocumentRow rows[] = document.getRows();

    // find the rows that were modified since last parse
    for(int row_nr = 0; row_nr < rowsCount; row_nr++){
      SCEDocumentRow row = rows[row_nr];
      if(!row.modified) continue;

      // has this row a known parser state?
      if(row.parserStateStack != null){
        parseRow(row_nr, rowsCount, rows);
      }else{
        parseRow(row_nr - 1, rowsCount, rows);
      }
    }
  }

  /**
   * Parse one row of the document (and following, if the state changed).
   *
   * @param row_nr the row
   * @param rowsCount the total number of rows
   * @param rows the array with all rows
   */
  private void parseRow(int row_nr, int rowsCount, SCEDocumentRow rows[]){
    boolean ready = false;

    while(!ready && row_nr < rowsCount){
      SCEDocumentRow row = rows[row_nr];
      // this may never be
      if(row.parserStateStack == null) throw new RuntimeException("Internel parser error occured.");

      // the current parser state (at the beginning of the row)
      ParserStateStack stateStack = row.parserStateStack.copy();
      ParserState state = stateStack.peek();

      // reset the modified value of the row
      row.modified = false;

      // parse the row
      SCEDocumentChar chars[] = row.chars;
      for(int char_nr = 0; char_nr < row.length; char_nr++){
        // search for a backslash '\'
        if(chars[char_nr].character == '\\'){
          String command = getCommandString(row, char_nr + 1);

          if(command == null){
            // if there is no command -> '\' escapes the next character -> set style text
            if(char_nr < row.length - 1){
              chars[char_nr].style = LatexStyles.TEXT;
              chars[char_nr + 1].style = LatexStyles.TEXT;
              char_nr += 1;
            }else{
              chars[char_nr].style = LatexStyles.ERROR;
            }
          }else{
            // highlight the command
            for(int i = 0; i <= command.length(); i++){
              chars[char_nr + i].style = LatexStyles.COMMAND;
            }
            char_nr += command.length();
          }

          continue;
        }

        // search for '$' and "$$"
        if(chars[char_nr].character == '$'){

          continue;
        }

        // search for '{' and '}'
        if(chars[char_nr].character == '{'){
          chars[char_nr].style = LatexStyles.BRACKET;
          continue;
        }
        if(chars[char_nr].character == '}'){
          chars[char_nr].style = LatexStyles.BRACKET;
          continue;
        }

        // search for '%' (comment)
        if(chars[char_nr].character == '%'){
          while(char_nr < row.length) chars[char_nr++].style = LatexStyles.COMMENT;
          continue;
        }

        // default style is text or number
        if(chars[char_nr].character >= '0' && chars[char_nr].character <= '9'){
          chars[char_nr].style = LatexStyles.NUMBER;
        }else{
          chars[char_nr].style = LatexStyles.TEXT;
        }
      }

      // go to the next row
      row_nr++;

      // set the parser state for the next row
      if(row_nr < rowsCount){
        if(stateStack.equals(rows[row_nr].parserStateStack)){
          ready = true;
        }else{
          rows[row_nr].parserStateStack = stateStack;
        }
      }
    }
  }

  /**
   * Returns the command name found at the given position (after '\').
   *
   * @param row the row
   * @param offset the offset
   * @return the command string
   */
  private String getCommandString(SCEDocumentRow row, int offset){
    String command = null;

    SCEDocumentChar chars[] = row.chars;
    int end_offset = offset;
    for(; end_offset < row.length; end_offset++){
      char character = chars[end_offset].character;

      // only letters are allowed
      if(character >= 'a' && character <= 'z') continue;
      if(character >= 'A' && character <= 'Z') continue;

      // we found the end of the command
      break;
    }

    // did we find a command?
    if(offset != end_offset){
      SCEString sceCommand = new SCEString(chars, offset, end_offset - offset);
      command = sceCommand.toString();
    }

    return command;
  }

  // SCEDocumentListener methods
  public void documentChanged(SCEDocument sender, SCEDocumentEvent event){
    parseNeeded = true;
    currentlyChanging = true;
  }
}

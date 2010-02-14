
/**
 * @author Jörg Endrullis
 */

package jlatexeditor.syntaxhighlighting;

import jlatexeditor.gproperties.GProperties;
import jlatexeditor.syntaxhighlighting.states.MathMode;
import jlatexeditor.syntaxhighlighting.states.RootState;
import sce.component.*;
import sce.syntaxhighlighting.ParserState;
import sce.syntaxhighlighting.ParserStateStack;
import sce.syntaxhighlighting.SyntaxHighlighting;
import util.Aspell;

import java.io.IOException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class LatexSyntaxHighlighting extends SyntaxHighlighting implements SCEDocumentListener{
	private static final Pattern TERM_PATTERN = Pattern.compile("(\\\\?[\\wäöüÄÖÜß_\\-\\^]+)");
	private static final Pattern BAD_TERM_CHARS = Pattern.compile("[\\\\\\d_\\-\\^]");
	private static final Pattern TODO_PATTERN = Pattern.compile("\\btodo\\b");

  // text pane and document
  private SCEPane pane = null;
  private SCEDocument document = null;

  // do we need to parse
  private boolean parseNeeded = false;
  private boolean currentlyChanging = false;

	private static Aspell aspell;
	static {
  	aspell = Aspell.getInstance(GProperties.getAspellLang());
	}

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
   * Resets the states/ syntax highlighting to initial state.
   */
  public void reset(){
    // get the actual document rows
    int rowsCount = document.getRowsCount();
    SCEDocumentRow rows[] = document.getRows();

    // reset all states states and mark rows as modified
    for(int row_nr = 0; row_nr < rowsCount; row_nr++){
      rows[row_nr].modified = true;
      rows[row_nr].parserStateStack = null;
    }

    // initialize the first row with states state
    rows[0].parserStateStack = new ParserStateStack();
    rows[0].parserStateStack.push(new RootState());
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
        // internal states error (should never happen)
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

      // has this row a known states state?
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
	  LatexStyles.CommandStyle lastCommandStyle = null;

    while(!ready && row_nr < rowsCount){
      SCEDocumentRow row = rows[row_nr];
      // this may never be
      if(row.parserStateStack == null) throw new RuntimeException("Internal parser error occured.");

      // the current states state (at the beginning of the row)
      ParserStateStack stateStack = row.parserStateStack.copy();
      ParserState state = stateStack.peek();

      // reset the modified value of the row
      row.modified = false;

      // parse the row
      SCEDocumentChar chars[] = row.chars;
      for(int char_nr = 0; char_nr < row.length; char_nr++){
        SCEDocumentChar sce_char = chars[char_nr];
        char c = sce_char.character;

        byte[] stateStyles = state.getStyles();

        // search for a backslash '\'
        if(c == '\\'){
          String command = getCommandString(row, char_nr + 1);

          if(command == null){
            byte styleText = stateStyles[LatexStyles.TEXT];
            // if there is no command -> '\' escapes the next character -> set style text
            if(char_nr < row.length - 1){
              sce_char.style = styleText;
              chars[char_nr + 1].style = styleText;
              char_nr += 1;
            }else{
              sce_char.style = stateStyles[LatexStyles.ERROR];
            }
	          lastCommandStyle = null;
          }else{
            lastCommandStyle = LatexStyles.getCommandStyle(command);
            // highlight the command
            byte commandStyle = stateStyles[lastCommandStyle.commandStyle];
            for(int i = 0; i <= command.length(); i++){
              chars[char_nr + i].style = commandStyle;
            }
            char_nr += command.length();
          }

          continue;
        }

        // search for '$' and "$$"
        if(c == '$'){
          sce_char.style = stateStyles[LatexStyles.MATH];

	        boolean doubleMath = chars[char_nr + 1].character == '$';
	        if (doubleMath) {
            char_nr++;
            chars[char_nr].style = stateStyles[LatexStyles.MATH];
          }

	        // if active math mode -> close; otherwise open
	        if (state instanceof MathMode) {
		        stateStack.pop();
		        state = stateStack.peek();
	        } else {
		        stateStack.push(state = new MathMode(doubleMath));
	        }

          continue;
        }

        // search for '{' and '}'
        if(c == '{'){
          sce_char.style = stateStyles[LatexStyles.BRACKET];
          continue;
        }
        if(c == '}'){
          sce_char.style = stateStyles[LatexStyles.BRACKET];
          continue;
        }

        // search for '%' (comment)
        if(c == '%'){
          byte commentStyle = stateStyles[LatexStyles.COMMENT];
	        Matcher matcher = TODO_PATTERN.matcher(row.toString().substring(char_nr).toLowerCase());
	        if (matcher.find()) {
		        int todoIndex = char_nr + matcher.start();
		        while(char_nr < todoIndex) chars[char_nr++].style = commentStyle;
		        byte todoStyle = stateStyles[LatexStyles.TODO];
		        while(char_nr < row.length) chars[char_nr++].style = todoStyle;
	        } else {
		        while(char_nr < row.length) chars[char_nr++].style = commentStyle;
	        }
          continue;
        }

        // default style is text or number
        if(c >= '0' && c <= '9'){
          sce_char.style = stateStyles[LatexStyles.NUMBER];
        }else{
          sce_char.style = stateStyles[LatexStyles.TEXT];
        }
      }

	    // extract word from row that shell be checked for misspellings
	    String rowString = document.getRow(row_nr);

	    // for each term in this row
	    Matcher matcher = TERM_PATTERN.matcher(rowString);
	    while(matcher.find()) {
		    // check if it's not a tex command and does not contain formula specific characters ("_" and numbers)
		    String termString = matcher.group(1);
		    StyleableTerm term = null;
		    if (BAD_TERM_CHARS.matcher(termString).find()) {
			    term = new StyleableTerm(termString, chars, matcher.start(1), LatexStyles.U_NORMAL);
		    } else {
			    // spell check
			    try {
            if(aspell != null) {
              Aspell.Result aspellResult = aspell.check(termString);
              term = new StyleableTerm(termString, chars, matcher.start(1), aspellResult.isCorrect() ? LatexStyles.U_NORMAL : LatexStyles.U_MISSPELLED);
            }
			    } catch (IOException e) {
				    e.printStackTrace();
			    }

          if(term == null) term = new StyleableTerm(termString, chars, matcher.start(1), LatexStyles.U_NORMAL);
		    }

		    term.applyStyleToDoc();
	    }

      // go to the next row
      row_nr++;

      // set the states state for the next row
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
	  // command consists of at least 1 char
	  if (end_offset == offset) end_offset++;

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

package jlatexeditor.bib;

import jlatexeditor.syntaxhighlighting.LatexStyles;
import jlatexeditor.syntaxhighlighting.LatexSyntaxHighlighting;
import jlatexeditor.syntaxhighlighting.states.RootState;
import sce.component.*;
import sce.syntaxhighlighting.ParserStateStack;
import sce.syntaxhighlighting.SyntaxHighlighting;

import java.util.Arrays;

/**
 * Syntax highlighting for global.properties.
 */
public class BibSyntaxHighlighting extends SyntaxHighlighting implements SCEDocumentListener {
  private static char[] COMMA_OR_BRACKET = new char[] {'}', ',', ' '};
  static { Arrays.sort(COMMA_OR_BRACKET); }

  // text pane and document
  private SCEPane pane = null;
  private SCEDocument document = null;

  // do we need to parse
  private boolean parseNeeded = false;
  private boolean currentlyChanging = false;

  public BibSyntaxHighlighting(SCEPane pane) {
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
  public void reset() {
    // get the actual document rows
    int rowsCount = document.getRowsCount();
    SCEDocumentRow rows[] = document.getRows();

    // reset all states states and mark rows as modified
    for (int row_nr = 0; row_nr < rowsCount; row_nr++) {
      rows[row_nr].modified = true;
      rows[row_nr].parserStateStack = null;
    }

    // initialize the first row with states state
    rows[0].parserStateStack = new ParserStateStack();
    rows[0].parserStateStack.push(new RootState());
  }

  public void run() {
    while (!isInterrupted()) {
      // sleep a short time
      try {
        sleep(100);
      } catch (InterruptedException e) {
        continue;
      }

      // only parse, if the user does not edit the text
      if (!parseNeeded || currentlyChanging) {
        currentlyChanging = false;
        continue;
      }
      parseNeeded = false;

      // parse the document
      try {
        parse();
      } catch (RuntimeException e) {
        // internal states error (should never happen)
        e.printStackTrace();
      }

      pane.repaint();
    }
  }

  /**
   * Parse and highlight the document.
   */
  private void parse() {
    // get the actual document rows
    int rowsCount = document.getRowsCount();
    SCEDocumentRow rows[] = document.getRows();

    // find the rows that were modified since last parse
    for (int row_nr = 0; row_nr < rowsCount; row_nr++) {
      SCEDocumentRow row = rows[row_nr];
      if (!row.modified) continue;

      // has this row a known states state?
      if (row.parserStateStack != null) {
        parseRow(row_nr, rowsCount, rows);
      } else {
        parseRow(row_nr - 1, rowsCount, rows);
      }
    }
  }

  /**
   * Parse one row of the document (and following, if the state changed).
   *
   * @param row_nr    the row
   * @param rowsCount the total number of rows
   * @param rows      the array with all rows
   */
  private void parseRow(int row_nr, int rowsCount, SCEDocumentRow rows[]) {
    boolean ready = false;

    while (!ready && row_nr < rowsCount) {
      SCEDocumentRow row = rows[row_nr];
      // this may never be
      if (row.parserStateStack == null) throw new RuntimeException("Internal parser error occured.");

      // the current states state (at the beginning of the row)
      ParserStateStack stateStack = row.parserStateStack.copy();
      BibParserState state = (BibParserState) stateStack.peek();

      // reset the modified value of the row
      row.modified = false;

      // parse the row
      SCEDocumentChar chars[] = row.chars;
      for (int char_nr = 0; char_nr < row.length; char_nr++) {
        SCEDocumentChar sce_char = chars[char_nr];
        char c = sce_char.character;

        byte[] stateStyles = state.getStyles();
        chars[char_nr].style = stateStyles[LatexStyles.COMMENT];

        // @name
        if(state.getState() == BibParserState.STATE_NOTHING && c == '@') {
          String entryType = LatexSyntaxHighlighting.getWord(row, char_nr + 1, false);
          if(entryType == null) continue;

          byte entryStyle = stateStyles[LatexStyles.COMMAND];
          for (int i = 0; i <= entryType.length(); i++) {
            chars[char_nr + i].style = entryStyle;
          }
          char_nr += entryType.length();

          stateStack.pop();
          stateStack.push(new BibParserState(BibParserState.STATE_EXPECT_OPEN, 0));
          continue;
        }

        // open entry {
        if(state.getState() == BibParserState.STATE_EXPECT_OPEN && c == '{') {
          chars[char_nr].style = stateStyles[LatexStyles.BRACKET];

          stateStack.pop();
          stateStack.push(new BibParserState(BibParserState.STATE_EXPECT_NAME, 0));
          continue;
        }

        // closing bracket
        if(c == '}' && state.getState() != BibParserState.STATE_VALUE_QUOTED) {
          sce_char.style = stateStyles[LatexStyles.BRACKET];

          stateStack.pop();

          if(state.getBracketLevel() > 1) {
            stateStack.push(new BibParserState(state.getState(), state.getBracketLevel()-1));
          } else
          if(state.getBracketLevel() == 1) {
            // exit value
            stateStack.push(new BibParserState(BibParserState.STATE_EXPECT_COMMA, 0));
          } else {
            // exit block
            stateStack.push(new BibParserState(BibParserState.STATE_NOTHING, 0));
          }

          continue;
        }

        // entry name
        if(state.getState() == BibParserState.STATE_EXPECT_NAME && !Character.isWhitespace(c)) {
          String entryName = LatexSyntaxHighlighting.getUntil(row, char_nr + 1, COMMA_OR_BRACKET);
          if(entryName == null) {
            sce_char.style = stateStyles[LatexStyles.ERROR];
            continue;
          }

          byte entryStyle = stateStyles[LatexStyles.COMMAND];
          for (int i = 0; i <= entryName.length(); i++) {
            chars[char_nr + i].style = entryStyle;
          }
          char_nr += entryName.length();

          stateStack.pop();
          stateStack.push(new BibParserState(BibParserState.STATE_EXPECT_COMMA, 0));
          continue;
        }
      }

      // go to the next row
      row_nr++;

      // set the states state for the next row
      if (row_nr < rowsCount) {
        if (stateStack.equals(rows[row_nr].parserStateStack)) {
          ready = true;
        } else {
          rows[row_nr].parserStateStack = stateStack;
        }
      }
    }
  }

  private void markError(SCEDocumentRow row, int startColumn, int length) {
    SCEDocumentChar[] chars = row.chars;
    int endColumn = startColumn + length;
    for (int i = startColumn; i < endColumn; i++) {
      chars[i].style = LatexStyles.ERROR;
    }
  }

  // SCEDocumentListener methods

  public void documentChanged(SCEDocument sender, SCEDocumentEvent event) {
    parseNeeded = true;
    currentlyChanging = true;
  }

}
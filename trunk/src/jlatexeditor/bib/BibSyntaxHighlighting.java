package jlatexeditor.bib;

import jlatexeditor.JLatexEditorJFrame;
import jlatexeditor.codehelper.BackgroundParser;
import jlatexeditor.codehelper.BibParser;
import jlatexeditor.syntaxhighlighting.LatexStyles;
import jlatexeditor.syntaxhighlighting.LatexSyntaxHighlighting;
import sce.component.*;
import sce.syntaxhighlighting.ParserStateStack;
import sce.syntaxhighlighting.SyntaxHighlighting;
import util.TrieSet;

import java.util.ArrayList;
import java.util.Arrays;

/**
 * Syntax highlighting for global.properties.
 */
public class BibSyntaxHighlighting extends SyntaxHighlighting implements SCEDocumentListener {
  private static char[] COMMA_OR_BRACKET = new char[] {'}', ',', ' '};
  private static char[] EQ_COMMA_OR_BRACKET = new char[] {'=', '}', ',', ' '};
  private static char[] EQ = new char[] {'=', ' '};
  static {
    Arrays.sort(COMMA_OR_BRACKET);
    Arrays.sort(EQ_COMMA_OR_BRACKET);
  }

  // text pane and document
  private SCEPane pane = null;
  private SCEDocument document = null;
  private static BackgroundParser backgroundParser = null;

  // do we need to parse
  private boolean parseNeeded = false;
  private boolean currentlyChanging = false;

  public BibSyntaxHighlighting(SCEPane pane, BackgroundParser parser) {
	  super("BibTexSyntaxHighlighting");
    this.pane = pane;
    document = pane.getDocument();
    document.addSCEDocumentListener(this);

    backgroundParser = parser;

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
    rows[0].parserStateStack.push(new BibParserState(BibParserState.STATE_NOTHING));
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
      ParserStateStack stateStack = parseRow(row, row.length);

      // go to the next row
      row_nr++;

      // set the states state for the next row
      if (row_nr < rowsCount) {
        if (stateStack.equals(rows[row_nr].parserStateStack)) {
          ready = true;
        } else {
          rows[row_nr].parserStateStack = stateStack.copy();
        }
      }
    }
  }

  public static ParserStateStack parseRow(SCEDocumentRow row, int length) {
    // this may never be
    if (row.parserStateStack == null) throw new RuntimeException("Internal parser error occurred.");

    // the current states state (at the beginning of the row)
    ParserStateStack stateStack = row.parserStateStack.copy();
    BibParserState state = (BibParserState) stateStack.peek();

    // reset the modified value of the row
    row.modified = false;

    // parse the row
    SCEDocumentChar chars[] = row.chars;
    for (int char_nr = 0; char_nr < length; char_nr++) {
      SCEDocumentChar sce_char = chars[char_nr];
      char c = sce_char.character;

      byte[] stateStyles = state.getStyles();
      chars[char_nr].style = stateStyles[Character.isWhitespace(c) ? LatexStyles.TEXT : LatexStyles.COMMENT];

      // @name
      if(state.getState() == BibParserState.STATE_NOTHING && c == '@') {
        String entryType = LatexSyntaxHighlighting.getWord(row, char_nr + 1, false);
        if(entryType == null) {
          sce_char.style = stateStyles[LatexStyles.ERROR];
          continue;
        }

        byte entryStyle = stateStyles[LatexStyles.COMMAND];
        for (int i = 0; i <= entryType.length(); i++) {
          chars[char_nr + i].style = entryStyle;
        }
        char_nr += entryType.length();

        state.setState(BibParserState.STATE_EXPECT_OPEN);
        state.setEntryType(entryType);
        state.setKeys(new ArrayList<String>());
        state.setAllKeys(new ArrayList<String>());
        continue;
      }

      // open entry {
      if(state.getState() == BibParserState.STATE_EXPECT_OPEN && !Character.isWhitespace(c)) {
        sce_char.style = stateStyles[LatexStyles.BRACKET];
        if(c != '{') sce_char.style = LatexStyles.ERROR;

        state.setState(BibParserState.STATE_EXPECT_NAME);
        continue;
      }

      // close string entry }
      if(state.getState() == BibParserState.STATE_EXPECT_CLOSE && !Character.isWhitespace(c)) {
        sce_char.style = stateStyles[LatexStyles.BRACKET];
        if(c != '}') {
          sce_char.style = LatexStyles.ERROR;
          continue;
        }
      }

      // closing bracket
      if(c == '}' && state.getState() != BibParserState.STATE_VALUE_QUOTED) {
        sce_char.style = stateStyles[LatexStyles.BRACKET];

        if(state.getBracketLevel() > 1) {
          state.setBracketLevel(state.getBracketLevel()-1);
        } else
        if(state.getBracketLevel() == 1) {
          // exit value
          state.setState(BibParserState.STATE_EXPECT_COMMA);
          state.setBracketLevel(0);
        } else {
          // exit block
          state.getAllKeys().clear();
          state.getAllKeys().addAll(state.getKeys());
          state.setKeys(new ArrayList<String>());

          state.setState(BibParserState.STATE_NOTHING);
          state.setEntryType(null);
        }

        continue;
      }

      // entry name
      if(state.getState() == BibParserState.STATE_EXPECT_NAME && !Character.isWhitespace(c)) {
        boolean isString = state.getEntryType().toLowerCase().equals("string");
        char delimiter[] = !isString ? COMMA_OR_BRACKET: EQ;
        String entryName = LatexSyntaxHighlighting.getUntil(row, char_nr, delimiter);
        if(c == ',' || entryName == null) {
          sce_char.style = stateStyles[LatexStyles.TEXT];
          if(c == ',') state.setState(BibParserState.STATE_EXPECT_KEY);
          continue;
        }

        byte entryStyle = stateStyles[LatexStyles.getStyle("begin")];
        // is the bib item used?
        if(backgroundParser != null) {
          TrieSet<BackgroundParser.FilePos> bibRefs = backgroundParser.getBibRefs();
          if(!bibRefs.contains(entryName)) {
            entryStyle = stateStyles[LatexStyles.COMMENT];
          }
        }

        for (int i = 0; i <= entryName.length(); i++) {
          chars[char_nr + i].style = entryStyle;
        }
        char_nr += entryName.length()-1;

        state.setState(!isString ? BibParserState.STATE_EXPECT_COMMA : BibParserState.STATE_EXPECT_EQ);
        continue;
      }

      // comma
      if(state.getState() == BibParserState.STATE_EXPECT_COMMA && !Character.isWhitespace(c)) {
        sce_char.style = stateStyles[LatexStyles.TEXT];
        if(c != ',') sce_char.style = LatexStyles.ERROR;

        state.setState(BibParserState.STATE_EXPECT_KEY);
        continue;
      }

      // key
      if(state.getState() == BibParserState.STATE_EXPECT_KEY && !Character.isWhitespace(c)) {
        String key = LatexSyntaxHighlighting.getUntil(row, char_nr, EQ_COMMA_OR_BRACKET);
        String keyLower = key.toLowerCase();
        if(key == null) {
          sce_char.style = stateStyles[LatexStyles.ERROR];
          continue;
        }

        byte entryStyle = stateStyles[LatexStyles.MATH_COMMAND];

        BibEntry entry = BibEntry.getEntry("@" + state.getEntryType());
        if(entry != null) {
          // non-existing key
          if(!entry.getAll().contains(keyLower)) {
            entryStyle = stateStyles[LatexStyles.ERROR];
          }
        }

        for (int i = 0; i <= key.length(); i++) {
          chars[char_nr + i].style = entryStyle;
        }
        char_nr += key.length()-1;

        state.getKeys().add(key);
        state.setState(BibParserState.STATE_EXPECT_EQ);
        continue;
      }

      // eq
      if(state.getState() == BibParserState.STATE_EXPECT_EQ && !Character.isWhitespace(c)) {
        sce_char.style = stateStyles[LatexStyles.TEXT];
        if(c != '=') sce_char.style = LatexStyles.ERROR;

        state.setState(BibParserState.STATE_EXPECT_VALUE);
        continue;
      }

      // value
      if(state.getState() == BibParserState.STATE_EXPECT_VALUE && !Character.isWhitespace(c)) {
        sce_char.style = stateStyles[LatexStyles.TEXT];

        if(c == '"') {
          state.setState(BibParserState.STATE_VALUE_QUOTED);
          continue;
        } else {
          state.setState(BibParserState.STATE_VALUE_BRACED);
        }
      }

      // quoted value
      if(state.getState() == BibParserState.STATE_VALUE_QUOTED) {
        sce_char.style = stateStyles[LatexStyles.TEXT];

        if(c == '\\') {
          if(char_nr + 1 < row.length) chars[char_nr+1].style = stateStyles[LatexStyles.TEXT];
          char_nr++;
          continue;
        } else
        if(c == '"') {
          boolean isString = state.getEntryType().toLowerCase().equals("string");
          state.setState(!isString ? BibParserState.STATE_EXPECT_COMMA : BibParserState.STATE_EXPECT_CLOSE);
        }

        continue;
      }

      // braced value
      if(state.getState() == BibParserState.STATE_VALUE_BRACED) {
        sce_char.style = stateStyles[LatexStyles.TEXT];

        if(c == '\\') {
          if(char_nr + 1 < row.length) chars[char_nr+1].style = stateStyles[LatexStyles.TEXT];
          char_nr++;
          continue;
        } else
        if(c == '{') {
          sce_char.style = stateStyles[LatexStyles.BRACKET];

          state.setBracketLevel(state.getBracketLevel()+1);
        } else
        if(state.getBracketLevel() == 0 && c == ',') {
          boolean isString = state.getEntryType().toLowerCase().equals("string");
          state.setState(!isString ? BibParserState.STATE_EXPECT_COMMA : BibParserState.STATE_EXPECT_CLOSE);
          char_nr--;
        }

        continue;
      }
    }

    return stateStack;
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
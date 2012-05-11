package jlatexeditor.bib;

import jlatexeditor.codehelper.BackgroundParser;
import jlatexeditor.syntaxhighlighting.LatexStyles;
import jlatexeditor.syntaxhighlighting.LatexSyntaxHighlighting;
import sce.codehelper.WordWithPos;
import sce.component.*;
import sce.syntaxhighlighting.ParserStateStack;
import sce.syntaxhighlighting.SyntaxHighlighting;
import util.SetTrie;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;

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

  public static HashMap<String,String[]> stringMap = new HashMap<String, String[]>();

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
    SCEDocumentRow rows[] = document.getRowsModel().getRows();

    // reset all states states and mark rows as modified
    for (SCEDocumentRow row : rows) {
      row.modified = true;
      row.parserStateStack = null;
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
    SCEDocumentRow rows[] = document.getRowsModel().getRows();

    // find the rows that were modified since last parse
    for (int row_nr = 0; row_nr < rows.length; row_nr++) {
      SCEDocumentRow row = rows[row_nr];
      if (!row.modified) continue;

      // has this row a known state?
      if (row.parserStateStack != null) {
        parseRow(row_nr, rows.length, rows);
      } else {
        parseRow(row_nr - 1, rows.length, rows);
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
      ParserStateStack stateStack = parseRow(row, row.length, document, rows, false);

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

  public static ParserStateStack parseRow(SCEDocumentRow row, int length, SCEDocument document, SCEDocumentRow rows[], boolean simulate) {
    // this may never be
    if (row.parserStateStack == null) throw new RuntimeException("Internal parser error occurred.");

    // the current states state (at the beginning of the row)
    ParserStateStack stateStack = row.parserStateStack.copy();
    BibParserState state = (BibParserState) stateStack.peek();
    if(simulate) state = (BibParserState) state.copy();

    // reset the modified value of the row
    row.modified = false;

    // parse the row
    SCEDocumentChar chars[] = row.chars;
    for (int char_nr = 0; char_nr < length; char_nr++) {
      SCEDocumentChar sce_char = chars[char_nr];
      char c = sce_char.character;

      byte[] stateStyles = state.getStyles();
      chars[char_nr].style = stateStyles[Character.isWhitespace(c) ? LatexStyles.TEXT : LatexStyles.COMMENT];

      // @type
      if(c == '@' && (state.getState() == BibParserState.STATE_NOTHING || char_nr==0)) {
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

        BibEntry entry = new BibEntry();
        if(!simulate) state.setEntry(entry);
        entry.setStartPos(new SCEDocumentPosition(sce_char));
        entry.setType(entryType);

        state.setValue(new BibKeyValuePair());

        state.setBracketLevel(0);
        continue;
      }

      // open entry {
      if(state.getState() == BibParserState.STATE_EXPECT_OPEN && !Character.isWhitespace(c)) {
        sce_char.style = stateStyles[LatexStyles.BRACKET];
        if(c != '{') sce_char.style = LatexStyles.ERROR;

        boolean isString = state.getEntry().getType(false).toLowerCase().equals("string");
        state.setState(!isString ? BibParserState.STATE_EXPECT_NAME : BibParserState.STATE_EXPECT_KEY);
        state.setBracketLevel(1);
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
      if(c == '}'
              && state.getState() != BibParserState.STATE_VALUE_QUOTED
              && state.getState() !=  BibParserState.STATE_VALUE_BASIC) {
        sce_char.style = stateStyles[LatexStyles.BRACKET];

        int bracketLevel = state.getBracketLevel();
        if(bracketLevel > 0) {
          state.setBracketLevel(bracketLevel-1);
        }

        if(bracketLevel > 2) {
        } else
        if(bracketLevel == 2) {
          // exit value
          SCEDocumentPosition start = state.getValueOpening();
          SCEDocumentPosition end = new SCEDocumentPosition(sce_char,0,1);
          String text = document != null ? document.getText(start, end) : "";
          state.getValue().addValue(new WordWithPos(text, start, end));

          if(state.getValue().getKey().word.equalsIgnoreCase("pages")) {
            if(text.matches(".*([^-])-([^-]).*")) {
              mark(rows, stateStyles[LatexStyles.getStyle("baderror")], start, end);
            }
          }

          state.setState(BibParserState.STATE_EXPECT_COMMA);
        } else
        if(bracketLevel == 1) {
          // exit block
          BibEntry entry = state.getEntry();

          BibKeyValuePair value = state.getValue();
          if(value != null && value.getKey() != null) {
            entry.getParameters().put(value.getKey().word.toLowerCase(), value);
          }

          entry.setEndPos(new SCEDocumentPosition(sce_char));
          entry.getAllParameters().clear();
          entry.getAllParameters().putAll(entry.getParameters());

          boolean isString = entry.getType(false).toLowerCase().equals("string");
          if(isString) {
            BibKeyValuePair keyValue = entry.getAllParameters().get(entry.getName());
            String[] values = BibAssistant.getValues(keyValue);
            stringMap.put(entry.getName().toLowerCase(), values);
          } else {
            String canonicalEntryName = BibAssistant.getCanonicalEntryName(entry);

            if(!entry.getName().startsWith(canonicalEntryName)) {
              WordWithPos name = entry.getNameWithPos();
              mark(rows, stateStyles[LatexStyles.getStyle("suggestion")], name.getStartPos(), name.getEndPos());
            }

            // lexicographic order
            if(state.getEntryNr() > 1) {
              BibEntry previousEntry = state.getEntryByNr().get(state.getEntryNr()-2);
              String previousEntryName = BibAssistant.getCanonicalEntryName(previousEntry);

              if(previousEntryName.compareTo(canonicalEntryName) > 0) {
                SCEDocumentPosition end = new SCEDocumentPosition(entry.getStartPos().getRow(), entry.getStartPos().getColumn() + entry.getType(false).length()+1);
                mark(rows, stateStyles[LatexStyles.getStyle("suggestion")], entry.getStartPos(), end);
              }
            }

          }

          state.setState(BibParserState.STATE_NOTHING);
          state.resetEntry();
        } else {
          sce_char.style = stateStyles[LatexStyles.COMMENT];
        }

        continue;
      }

      // entry name
      if(state.getState() == BibParserState.STATE_EXPECT_NAME && !Character.isWhitespace(c)) {
        char delimiter[] = COMMA_OR_BRACKET;
        String entryName = LatexSyntaxHighlighting.getUntil(row, char_nr, delimiter);
        state.getEntry().setName(new WordWithPos(entryName, new SCEDocumentPosition(sce_char)));
        if(c == ',' || entryName == null) {
          sce_char.style = stateStyles[LatexStyles.TEXT];
          if(c == ',') state.setState(BibParserState.STATE_EXPECT_KEY);
          continue;
        }

        byte entryStyle = stateStyles[LatexStyles.getStyle("begin")];
        // is the bib item used?
        if(backgroundParser != null) {
          SetTrie<BackgroundParser.FilePos> bibRefs = backgroundParser.getBibRefs();
          if(!bibRefs.contains(entryName)) {
            entryStyle = stateStyles[LatexStyles.COMMENT];
          }
        }

        // check whether entry name is in use
        for(int entryNr = 0; entryNr < state.getEntryNr()-1; entryNr++) {
          BibEntry previousEntry = state.getEntryByNr().get(entryNr);
          if(previousEntry != null) {
            if(entryName.equalsIgnoreCase(previousEntry.getName())) entryStyle = stateStyles[LatexStyles.getStyle("baderror")];
          }
        }

        for (int i = 0; i <= entryName.length(); i++) {
          chars[char_nr + i].style = entryStyle;
        }
        char_nr += entryName.length()-1;

        state.setState(BibParserState.STATE_EXPECT_COMMA);
        continue;
      }

      // comma
      if(state.getState() == BibParserState.STATE_EXPECT_COMMA && !Character.isWhitespace(c)) {
        sce_char.style = stateStyles[LatexStyles.TEXT];

        if(c == '#') {
          state.setState(BibParserState.STATE_EXPECT_VALUE);
          continue;
        } else
        if(c != ',') { sce_char.style = LatexStyles.ERROR; }

        BibKeyValuePair value = state.getValue();
        if(value != null && value.getKey() != null) {
          state.getEntry().getParameters().put(value.getKey().word.toLowerCase(), value);
        }

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

        BibEntryPattern entry = BibEntryPattern.getEntry("@" + state.getEntry().getType(false));
        if(entry != null) {
          // non-existing key
          if(!entry.getAll().contains(keyLower)) {
            entryStyle = stateStyles[LatexStyles.COMMENT];
          }
        }

        boolean isString = state.getEntry().getType(false).toLowerCase().equals("string");
        if(isString) {
          state.getEntry().setName(new WordWithPos(key, new SCEDocumentPosition(sce_char)));

          // check whether entry name is in use
          for(int entryNr = 0; entryNr < state.getEntryNr()-1; entryNr++) {
            BibEntry previousEntry = state.getEntryByNr().get(entryNr);
            if(previousEntry != null) {
              if(key.equalsIgnoreCase(previousEntry.getName())) entryStyle = stateStyles[LatexStyles.getStyle("baderror")];
            }
          }
        }

        for (int i = 0; i <= key.length(); i++) {
          chars[char_nr + i].style = entryStyle;
        }

        BibKeyValuePair value = new BibKeyValuePair();
        value.setKey(new WordWithPos(key, new SCEDocumentPosition(sce_char)));
        state.getEntry().getParameters().put(keyLower, value);
        state.setValue(value);

        state.setState(BibParserState.STATE_EXPECT_EQ);

        char_nr += key.length()-1;
        continue;
      }

      // eq
      if(state.getState() == BibParserState.STATE_EXPECT_EQ && !Character.isWhitespace(c)) {
        sce_char.style = stateStyles[LatexStyles.TEXT];
        if(c != '=') sce_char.style = LatexStyles.ERROR;

        state.setState(BibParserState.STATE_EXPECT_VALUE);

        state.getValue().setEq(new WordWithPos(c +"", new SCEDocumentPosition(sce_char)));

        continue;
      }

      // value
      if(state.getState() == BibParserState.STATE_EXPECT_VALUE && !Character.isWhitespace(c)) {
        sce_char.style = stateStyles[LatexStyles.TEXT];

        state.setValueOpening(new SCEDocumentPosition(sce_char));

        if(c == '"') {
          state.setState(BibParserState.STATE_VALUE_QUOTED);
          continue;
        } else
        if(c == '{') {
          state.setState(BibParserState.STATE_VALUE_BRACED);
        } else {
          state.setState(BibParserState.STATE_VALUE_BASIC);
        }
      }

      // basic value
      if(state.getState() == BibParserState.STATE_VALUE_BASIC) {
        sce_char.style = stateStyles[LatexStyles.TEXT];

        if(c == ',' || c == ' ' || c == '}') {
          SCEDocumentPosition start = state.getValueOpening();
          SCEDocumentPosition end = new SCEDocumentPosition(sce_char);
          String text = document != null ? document.getText(start, end) : "";
          state.getValue().addValue(new WordWithPos(text.trim(), start, end));

          if(c == ',' || c == '}') char_nr--;
          state.setState(BibParserState.STATE_EXPECT_COMMA);
          continue;
        }

        continue;
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
          SCEDocumentPosition start = state.getValueOpening();
          SCEDocumentPosition end = new SCEDocumentPosition(sce_char,0,1);
          String text = document != null ? document.getText(start, end) : "";
          state.getValue().addValue(new WordWithPos(text, start, end));

          if(state.getValue().getKey().word.equalsIgnoreCase("pages")) {
            if(text.matches(".*([^-])-([^-]).*")) {
              mark(rows, stateStyles[LatexStyles.getStyle("baderror")], start, end);
            }
          }

          state.setState(BibParserState.STATE_EXPECT_COMMA);
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
        if(state.getBracketLevel() == 1 && c == ',') {
          state.setState(BibParserState.STATE_EXPECT_COMMA);
          char_nr--;
        }

        continue;
      }
    }

    return stateStack;
  }

  private static void mark(SCEDocumentRow rows[], byte style, SCEPosition start, SCEPosition end) {
    int startRow = start.getRow();
    int endRow = end.getRow();
    for(int rowNr = startRow; rowNr <= endRow; rowNr++) {
      SCEDocumentRow row = rows[rowNr];

      int min = rowNr == startRow ? start.getColumn() : 0;
      int max = Math.min(row.length, rowNr == endRow ? end.getColumn() : row.length);
      SCEDocumentChar[] chars = row.chars;
      for (int i = min; i < max; i++) {
        chars[i].style = style;
      }
    }
  }

  // SCEDocumentListener methods

  public void documentChanged(SCEDocument sender, SCEDocumentEvent event) {
    parseNeeded = true;
    currentlyChanging = true;
  }

}
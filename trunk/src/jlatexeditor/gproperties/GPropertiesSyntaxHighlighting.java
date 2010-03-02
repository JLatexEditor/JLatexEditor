package jlatexeditor.gproperties;

import de.endrullis.utils.BetterProperties2.*;
import jlatexeditor.gproperties.GProperties;
import jlatexeditor.syntaxhighlighting.LatexStyles;
import jlatexeditor.syntaxhighlighting.states.RootState;
import sce.component.*;
import sce.syntaxhighlighting.ParserState;
import sce.syntaxhighlighting.ParserStateStack;
import sce.syntaxhighlighting.SyntaxHighlighting;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Syntax highlighting for global.properties.
 */
public class GPropertiesSyntaxHighlighting extends SyntaxHighlighting implements SCEDocumentListener {
  private static final Pattern PATTERN = Pattern.compile("^([^#=]+)=([^#]*)");

  // text pane and document
  private SCEPane pane = null;
  private SCEDocument document = null;

  // do we need to parse
  private boolean parseNeeded = false;
  private boolean currentlyChanging = false;

  public GPropertiesSyntaxHighlighting(SCEPane pane) {
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
      ParserState state = stateStack.peek();

      // reset the modified value of the row
      row.modified = false;

      // parse the row
      boolean parsingKey = true;
      SCEDocumentChar chars[] = row.chars;
      for (int char_nr = 0; char_nr < row.length; char_nr++) {
        SCEDocumentChar sce_char = chars[char_nr];
        char c = sce_char.character;

        byte[] stateStyles = state.getStyles();

        // search for '#' (comment)
        if (c == '#') {
          byte commentStyle = stateStyles[LatexStyles.COMMENT];
          while (char_nr < row.length) chars[char_nr++].style = commentStyle;
          continue;
        }

        if (parsingKey) {
          // search for a backslash '\'
          if (c == '=') {
            sce_char.style = stateStyles[GPropertiesStyles.TEXT];
            parsingKey = false;
            continue;
          } else {
            sce_char.style = stateStyles[GPropertiesStyles.KEY];
            continue;
          }
        }

        // search for '{' and '}'
        if (c == '{') {
          sce_char.style = stateStyles[GPropertiesStyles.BRACKET];
          continue;
        }
        if (c == '}') {
          sce_char.style = stateStyles[GPropertiesStyles.BRACKET];
          continue;
        }

        // default style is text or number
        if (c >= '0' && c <= '9') {
          sce_char.style = stateStyles[GPropertiesStyles.NUMBER];
        } else {
          sce_char.style = stateStyles[GPropertiesStyles.TEXT];
        }
      }

      Matcher matcher = PATTERN.matcher(row.toString());
      if (matcher.find()) {
        String key = matcher.group(1);
        String value = matcher.group(2);

        Def def = GProperties.getDef(key.replaceAll("\\\\(.)", "$1"));
        if (def == null) {
          // mark invalid key
          markError(row, matcher.start(1), key.length());
        } else {
          // check value
          if (!def.getRange().isValid(value.replaceAll("\\\\(.)", "$1"))) {
            markError(row, matcher.start(2), value.length());
          }
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
      chars[i].style = GPropertiesStyles.ERROR;
    }
  }

  // SCEDocumentListener methods

  public void documentChanged(SCEDocument sender, SCEDocumentEvent event) {
    parseNeeded = true;
    currentlyChanging = true;
  }
}

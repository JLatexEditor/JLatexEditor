/**
 * @author Jörg Endrullis
 */

package jlatexeditor.syntaxhighlighting;

import jlatexeditor.codehelper.BackgroundParser;
import jlatexeditor.syntaxhighlighting.states.MathMode;
import jlatexeditor.syntaxhighlighting.states.RootState;
import sce.codehelper.CHCommand;
import sce.codehelper.CHCommandArgument;
import sce.component.*;
import sce.syntaxhighlighting.ParserState;
import sce.syntaxhighlighting.ParserStateStack;
import sce.syntaxhighlighting.SyntaxHighlighting;
import util.SpellChecker;
import util.Trie;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.Iterator;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class LatexSyntaxHighlighting extends SyntaxHighlighting implements SCEDocumentListener {
  private static final Pattern TERM_PATTERN = Pattern.compile("(\\\\?[\\wäöüÄÖÜß_\\-\\^]+)");
  private static final Pattern BAD_TERM_CHARS = Pattern.compile("[\\\\\\d_\\-\\^]");
  private static final Pattern TODO_PATTERN = Pattern.compile("\\btodo\\b");

  // text pane and document
  private SCEPane pane = null;
  private SCEDocument document = null;
	private Trie<CHCommand> commands;
	private BackgroundParser backgroundParser;

  // do we need to parse
  private boolean parseNeeded = false;
  private boolean currentlyChanging = false;

	private SpellChecker spellChecker;

  public LatexSyntaxHighlighting(SCEPane pane, SpellChecker spellChecker, Trie<CHCommand> commands, BackgroundParser backgroundParser) {
	  super("LatexSyntaxHighlighting");
    this.pane = pane;
	  this.spellChecker = spellChecker;
	  this.commands = commands;
	  this.backgroundParser = backgroundParser;
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
	  Iterator<CHCommandArgument> argumentsIterator = null;

    while (!ready && row_nr < rowsCount) {
      SCEDocumentRow row = rows[row_nr];
      // this may never be
      if (row.parserStateStack == null) throw new RuntimeException("Internal parser error occurred.");

      // the current states state (at the beginning of the row)
      ParserStateStack stateStack = row.parserStateStack.copy();
      ParserState state = stateStack.peek();

      // reset the modified value of the row
      row.modified = false;

      // parse the row
      SCEDocumentChar chars[] = row.chars;
      for (int char_nr = 0; char_nr < row.length; char_nr++) {
        SCEDocumentChar sce_char = chars[char_nr];
        char c = sce_char.character;

        byte[] stateStyles = state.getStyles();

        // search for a backslash '\'
        if (c == '\\') {
          String command = getWord(row, char_nr + 1, true);

	        // todo: style of command
	        CHCommand chCommand = commands.get("\\" + command);
	        if (chCommand != null) {
		        argumentsIterator = chCommand.getArguments().iterator();
	        }

	        // highlight the command
          byte commandStyle = stateStyles[getStyle(chCommand == null ? null : chCommand.getStyle(), LatexStyles.COMMAND)];
	        char_nr = setStyle(command, commandStyle, chars, char_nr);

          continue;
        }

        // search for '$' and "$$"
        if (c == '$') {
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

	        argumentsIterator = null;
          continue;
        }

        // search for '{' and '}'
        if (c == '{') {
	        String argumentType = getArgumentType(argumentsIterator);

	        if (argumentType != null) {
		        String param = getStringUpToClosingBracket(row, char_nr + 1);

		        if (argumentType.equals("title") || argumentType.equals("italic") || argumentType.equals("bold")) {

							// highlight the command
							byte style = stateStyles[getStyle(argumentType, LatexStyles.TEXT)];
			        char_nr = setStyle(param, style, chars, char_nr);
		        } else
		        if (argumentType.equals("file")) {
			        boolean fileExists = false;
			        if (param.startsWith("/")) {
				        fileExists = new File(param).exists();
			        } else {
				        File docFile = pane.getSourceCodeEditor().getFile();
				        if (docFile.exists()) {
					        String pathname = docFile.getParentFile().getAbsolutePath() + "/" + param;
					        fileExists = new File(pathname).exists();
				        }
			        }

			        // highlight the command
							byte style = stateStyles[getStyle(fileExists ? "file_exists" : "file_not_found", LatexStyles.TEXT)];
			        char_nr = setStyle(param, style, chars, char_nr);
		        } else
		        if (argumentType.equals("label")) {
			        boolean labelExists = backgroundParser.getLabels().contains(param);
			        byte style = stateStyles[getStyle(labelExists ? "label_exists" : "label_not_found", LatexStyles.TEXT)];
			        char_nr = setStyle(param, style, chars, char_nr);
		        } else
		        if (argumentType.equals("cite")) {
			        boolean citeExists = backgroundParser.getCites().contains(param);
			        byte style = stateStyles[getStyle(citeExists ? "cite_exists" : "cite_not_found", LatexStyles.TEXT)];
		        }
	        }

          sce_char.style = stateStyles[LatexStyles.BRACKET];
          continue;
        }
        if (c == '}') {
          sce_char.style = stateStyles[LatexStyles.BRACKET];
          continue;
        }

        // search for '%' (comment)
        if (c == '%') {
          byte commentStyle = stateStyles[LatexStyles.COMMENT];
          Matcher matcher = TODO_PATTERN.matcher(row.toString().substring(char_nr).toLowerCase());
          if (matcher.find()) {
            int todoIndex = char_nr + matcher.start();
            while (char_nr < todoIndex) chars[char_nr++].style = commentStyle;
            byte todoStyle = stateStyles[LatexStyles.TODO];
            while (char_nr < row.length) chars[char_nr++].style = todoStyle;
          } else {
            while (char_nr < row.length) chars[char_nr++].style = commentStyle;
          }
          continue;
        }

        // default style is text or number
        if (c >= '0' && c <= '9') {
          sce_char.style = stateStyles[LatexStyles.NUMBER];
        } else {
          sce_char.style = stateStyles[LatexStyles.TEXT];
        }
      }

      // extract word from row that shell be checked for misspellings
      String rowString = document.getRow(row_nr);

      // for each term in this row
      Matcher matcher = TERM_PATTERN.matcher(rowString);
      while (matcher.find()) {
        // check if it's not a tex command and does not contain formula specific characters ("_" and numbers)
        String termString = matcher.group(1);
        StyleableTerm term = null;
        if (BAD_TERM_CHARS.matcher(termString).find()) {
          term = new StyleableTerm(termString, chars, matcher.start(1), LatexStyles.U_NORMAL);
        } else {
          // spell check
          try {
            if (spellChecker != null) {
              SpellChecker.Result spellCheckResult = spellChecker.check(termString);
              term = new StyleableTerm(termString, chars, matcher.start(1), spellCheckResult.isCorrect() ? LatexStyles.U_NORMAL : LatexStyles.U_MISSPELLED);
            }
          } catch (IOException e) {
            e.printStackTrace();
          }

          if (term == null) term = new StyleableTerm(termString, chars, matcher.start(1), LatexStyles.U_NORMAL);
        }

        term.applyStyleToDoc();
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

	private String getArgumentType(Iterator<CHCommandArgument> argumentsIterator) {
		if (argumentsIterator != null && argumentsIterator.hasNext()) {
			CHCommandArgument argument = argumentsIterator.next();
			return argument.getType();
		}
		return null;
	}

	private int setStyle(String text, byte style, SCEDocumentChar[] chars, int offset) {
		for (int i = 0; i <= text.length(); i++) {
		  chars[offset + i].style = style;
		}
		return offset + text.length();
	}

	private byte getStyle(String st, byte default_) {
		Byte style = LatexStyles.getStyle(st);
		return style != null ? style : default_;
	}

	/**
   * Returns the word found at the given position (after '\').
   *
   * @param row    the row
   * @param offset the offset
   * @return the command string
   */
  public static String getWord(SCEDocumentRow row, int offset, boolean nonEmpty) {
    String word = null;

    SCEDocumentChar chars[] = row.chars;
    int end_offset = offset;
    for (; end_offset < row.length; end_offset++) {
      char character = chars[end_offset].character;

      // only letters are allowed
      if (Character.isLetter(character)) continue;

      // we found the end of the command
      break;
    }
    // command consists of at least 1 char
    if (nonEmpty && end_offset == offset) end_offset++;

    // did we find a command?
    if (offset != end_offset) {
      SCEString sceCommand = new SCEString(chars, offset, end_offset - offset);
      word = sceCommand.toString();
    }

    return word;
  }

  /**
   * Returns the word found at the given position (after '\').
   *
   * @param row    the row
   * @param offset the offset
   * @return the command string
   */
  public static String getUntil(SCEDocumentRow row, int offset, char[] until) {
    String word = null;

    SCEDocumentChar chars[] = row.chars;
    int end_offset = offset;
    for (; end_offset < row.length; end_offset++) {
      char character = chars[end_offset].character;

      if (Arrays.binarySearch(until, character) < 0) continue;

      break;
    }
    // command consists of at least 1 char
    if (end_offset == offset) end_offset++;

    // did we find a command?
    if (offset != end_offset) {
      SCEString sceCommand = new SCEString(chars, offset, end_offset - offset);
      word = sceCommand.toString();
    }

    return word;
  }

	private String getStringUpToClosingBracket(SCEDocumentRow row, int offset) {
		SCEDocumentChar chars[] = row.chars;
		int end_offset = offset;
		int level = 1;
		for (; end_offset < row.length; end_offset++) {
		  char character = chars[end_offset].character;
			if (character == '{') {
				level++;
			} else
			if (character == '}') {
				level--;
				if (level == 0)
					break;
			}
		}

		SCEString sceCommand = new SCEString(chars, offset, end_offset - offset);
		return sceCommand.toString();
	}

  // SCEDocumentListener methods

  public void documentChanged(SCEDocument sender, SCEDocumentEvent event) {
    parseNeeded = true;
    currentlyChanging = true;
  }
}

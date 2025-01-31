package jlatexeditor.syntaxhighlighting;

import jlatexeditor.SCEManager;
import jlatexeditor.codehelper.BackgroundParser;
import jlatexeditor.codehelper.ScriptingSupport;
import jlatexeditor.syntaxhighlighting.states.DontParse;
import jlatexeditor.syntaxhighlighting.states.Env;
import jlatexeditor.syntaxhighlighting.states.MathMode;
import jlatexeditor.syntaxhighlighting.states.ScriptMode;
import sce.codehelper.CHArgumentType;
import sce.codehelper.CHCommand;
import sce.codehelper.CHCommandArgument;
import sce.component.*;
import sce.syntaxhighlighting.ParserState;
import sce.syntaxhighlighting.ParserStateStack;
import sce.syntaxhighlighting.SyntaxHighlighting;
import util.Function1;
import util.SimpleTrie;
import util.SpellChecker;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.Iterator;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Latex syntax highlighter.
 *
 * @author Jörg Endrullis
 * @author Stefan Endrullis
 */
public class LatexSyntaxHighlighting extends SyntaxHighlighting implements SCEDocumentListener {
  private static final Pattern TERM_PATTERN = Pattern.compile("(\\\\?[\\wäöüÄÖÜß_\\^]+)");
  private static final Pattern BAD_TERM_CHARS = Pattern.compile("[\\\\\\d_\\^]");
  private static final Pattern TODO_PATTERN = Pattern.compile("\\btodo\\b");
  private static final Pattern LIST_PATTERN = Pattern.compile("[^, ]+[^,]*");

  // text pane and document
  private SCEPane pane = null;
  private SCEDocument document = null;
	private SimpleTrie<CHCommand> commands;
	private BackgroundParser backgroundParser;

  private static final String scriptBegin = ScriptingSupport.genericBegin.substring(1);

  // do we need to parse
  private boolean parseNeeded = false;
  private boolean currentlyChanging = false;

	private SpellChecker spellChecker;

  public LatexSyntaxHighlighting(SCEPane pane, SpellChecker spellChecker, SimpleTrie<CHCommand> commands, BackgroundParser backgroundParser) {
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
	  document.invalidateSyntaxHighlighting();
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

      // has this row a known states state?
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

	    argumentsIterator = parseRow(row_nr, argumentsIterator, row, stateStack, state);

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

	protected Iterator<CHCommandArgument> parseRow(int row_nr, Iterator<CHCommandArgument> argumentsIterator, SCEDocumentRow row, ParserStateStack stateStack, ParserState state) {
		final SCEDocumentChar chars[] = row.chars;

		boolean rowWasParsed = false;

		// test for "%###" row (stop parsing)
		if (state instanceof DontParse) {
			if (row.length < 4 || !row.toString().startsWith("%###")) {
				byte style = state.getStyles()[LatexStyles.TEXT];
				for (int i=0; i < row.length; i++) chars[i].style = style;
				rowWasParsed = true;
			}
		}

    if (state instanceof ScriptMode) {
      byte styleText = state.getStyles()[LatexStyles.TEXT];
      byte styleComment = state.getStyles()[LatexStyles.COMMENT];

      for(int nr = 0; nr < row.length; nr++) {
        SCEDocumentChar c = chars[nr];

        if(c.character == ' ') { c.style = styleText; continue; }
        if(c.character == '%') { c.style = styleComment;
          for(nr++; nr < row.length; nr++) chars[nr].style = styleText;
          rowWasParsed = true;
          break;
        }

        stateStack.pop();
        state = stateStack.peek();
        break;
      }
    }

		if (!rowWasParsed) {
			// check if row has been added during an svn merge
			if (row.length >= 7) {
				char c = chars[0].character;
				switch (c) {
					case '<':
					case '>':
					case '=':
						rowWasParsed = true;
						for (int i = 1; i < 7; i++) {
							if (chars[i].character != c) rowWasParsed = false;
						}
						if (rowWasParsed) {
							if (c == '=') {
								rowWasParsed = row.length == 7;
							}
							else
								rowWasParsed = row.length >= 8 && chars[7].character == ' ';
						}
						if (rowWasParsed) {
							for (SCEDocumentChar aChar : chars) {
								aChar.style = state.getStyles()[LatexStyles.ERROR];
								//aChar.overlayStyle = 0;
							}
						}
				}
			}
		}

		if (!rowWasParsed) {
			// parse the latex row
			for (int char_nr = 0; char_nr < row.length; char_nr++) {
				SCEDocumentChar sce_char = chars[char_nr];
				char c = sce_char.character;

				final byte[] stateStyles = state.getStyles();

				// search for a backslash '\'
				if (c == '\\') {
					// check if next char is any a kind of brace
					if (char_nr + 1 < row.length) {
						switch (chars[char_nr+1].character) {
							case '(':
								MathMode mathMode = new MathMode(MathMode.Type.openingParenthesis);
								char_nr = processMathMode(stateStack, state, chars, char_nr, 2, mathMode);
								state = stateStack.peek();
								continue;
							case ')':
								mathMode = new MathMode(MathMode.Type.closingParenthesis);
								char_nr = processMathMode(stateStack, state, chars, char_nr, 2, mathMode);
								state = stateStack.peek();
								continue;
							case '[':
								mathMode = new MathMode(MathMode.Type.openingBracket);
								char_nr = processMathMode(stateStack, state, chars, char_nr, 2, mathMode);
								state = stateStack.peek();
								continue;
							case ']':
								mathMode = new MathMode(MathMode.Type.closingBracket);
								char_nr = processMathMode(stateStack, state, chars, char_nr, 2, mathMode);
								state = stateStack.peek();
								continue;
						}
					}

					String command = getWord(row, char_nr + 1, true);

					CHCommand chCommand = commands.get(command);
					if (chCommand != null) {
						argumentsIterator = chCommand.getArguments().iterator();
					}

					// highlight the command
					byte commandStyle = stateStyles[getStyle(chCommand == null ? null : chCommand.getStyle(), LatexStyles.COMMAND)];
					char_nr = setStyle(command.length() + 1, commandStyle, chars, char_nr);

					continue;
				}

				// search for '$' and "$$"
				if (c == '$') {
					int charCount = 1;

					MathMode.Type type = row.length > char_nr+1 && chars[char_nr+1].character == '$' ? MathMode.Type.doubled : MathMode.Type.simple;
					if (type == MathMode.Type.doubled) {
						charCount = 2;
					}

					MathMode mathMode = new MathMode(type);
					char_nr = processMathMode(stateStack, state, chars, char_nr, charCount, mathMode);
					state = stateStack.peek();

					argumentsIterator = null;
					continue;
				}

				// search for '{' and '}'
				if (c == '{') {
					CHArgumentType argumentType = getArgumentType(argumentsIterator);

					if (argumentType != null) {
						String argumentTypeName = argumentType.getName();
						final String param = getStringUpToClosingBracket(row, char_nr + 1);

						if (argumentTypeName.equals("title") || argumentTypeName.equals("italic") || argumentTypeName.equals("bold")) {
							// highlight the command
							byte style = stateStyles[getStyle(argumentTypeName, LatexStyles.TEXT)];
							char_nr = setStyle(param, style, chars, char_nr + 1);
						} else
						if (argumentTypeName.equals("file")) {
							String defaultExtension = argumentType.getProperty("defaultExtension");
							boolean fileExists = false;
							if (param.startsWith("/")) {
								fileExists = new File(param).exists() || new File(param + "." + defaultExtension).exists();
							} else {
								File docFile = SCEManager.getInstance().getMainEditor().getFile();
								if (docFile.exists()) {
									String pathname = docFile.getParentFile().getAbsolutePath() + "/" + param;
									String extPathname = pathname + "." +  defaultExtension;
									fileExists = new File(pathname).exists() || new File(extPathname).exists();
								}
							}

							// highlight the command
							byte style = stateStyles[getStyle(fileExists ? "file_exists" : "file_not_found", LatexStyles.TEXT)];
							char_nr = setStyle(param, style, chars, char_nr + 1);
						} else
						if (argumentTypeName.equals("label_def")) {
							BackgroundParser.FilePos existingDef = backgroundParser.getLabelDefs().get(param);
							boolean alreadyDefined = existingDef != null && existingDef.getLineNr() != row_nr;
							boolean labelReferenced = backgroundParser.getLabelRefs().contains(param);
							byte style;
							if (alreadyDefined) {
								style = stateStyles[getStyle("label_duplicate", LatexStyles.TEXT)];
							} else {
								style = stateStyles[getStyle(labelReferenced ? "label_exists" : "label_not_referenced", LatexStyles.TEXT)];
							}
							char_nr = setStyle(param, style, chars, char_nr + 1);
						} else
						if (argumentTypeName.equals("label_ref")) {
							boolean labelExists = backgroundParser.getLabelDefs().contains(param);
							byte style = stateStyles[getStyle(labelExists ? "label_exists" : "label_not_found", LatexStyles.TEXT)];
							char_nr = setStyle(param, style, chars, char_nr + 1);
						} else
						if (argumentTypeName.equals("cite_key_list")) {
							matchAndStyle(char_nr + 1, chars, param, LIST_PATTERN, new Function1<String, Byte>(){
								public Byte apply(String a1) {
									boolean citeExists = backgroundParser.getBibKeys2bibEntries().contains(a1);
									return stateStyles[getStyle(citeExists ? "cite_exists" : "cite_not_found", LatexStyles.TEXT)];
								}
							});
							char_nr += param.length();
						} else
						if (argumentTypeName.equals("opening_env")) {
							stateStack.push(new Env(param, false, state));
							byte style = stateStyles[getStyle("env_name", LatexStyles.TEXT)];
							char_nr = setStyle(param, style, chars, char_nr + 1);
						} else
						if (argumentTypeName.equals("closing_env")) {
							if (state instanceof Env) {
								Env openingEnv = (Env) state;
								Env closingEnv = new Env(param, true, state);
								boolean validClosing = false;
								if (closingEnv.closes(openingEnv)) {
									stateStack.pop();
									state = stateStack.peek();
									validClosing = true;
								}
								byte style = stateStyles[getStyle(validClosing ? "env_name" : "error", LatexStyles.TEXT)];
								char_nr = setStyle(param, style, chars, char_nr + 1);
							}
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
					String restOfRow = row.toString().substring(char_nr+1);
					if (restOfRow.startsWith("###")) {
						chars[char_nr++].style = commentStyle;
						byte todoStyle = stateStyles[LatexStyles.PARSER_STOP];
						for (int i=0; i<3; i++) chars[char_nr++].style = todoStyle;
						while (char_nr < row.length) chars[char_nr++].style = commentStyle;
						if (state instanceof DontParse) {
							stateStack.pop();
							state = stateStack.peek();
						} else {
							stateStack.push(state = new DontParse(state));
						}
          } else
          if (restOfRow.startsWith(scriptBegin)) {
            int position = char_nr;
            while (char_nr < row.length) chars[char_nr++].style = commentStyle;

            int offset = scriptBegin.length();
            int closing = restOfRow.indexOf('}', offset);
            if(closing >= 0) {
              String language = restOfRow.substring(offset, closing);
              if(ScriptingSupport.languagesSet.contains(language)) {
                byte todoStyle = stateStyles[LatexStyles.TODO];
                for(int nr = position+offset+1; nr <= position+closing; nr++) chars[nr].style = todoStyle;
              }
            }

            stateStack.push(state = new ScriptMode());
					} else {
						Matcher matcher = TODO_PATTERN.matcher(restOfRow.toLowerCase());
						if (matcher.find()) {
							int todoIndex = char_nr + matcher.start()-1;
							while (char_nr < todoIndex) chars[char_nr++].style = commentStyle;
							byte todoStyle = stateStyles[LatexStyles.TODO];
							while (char_nr < row.length) chars[char_nr++].style = todoStyle;
						} else {
							while (char_nr < row.length) chars[char_nr++].style = commentStyle;
						}
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

			// extract word from row that shall be checked for misspellings
			String rowString = row.toString();

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

        try {
				  term.applyStyleToDoc();
        } catch (Throwable e) {
          System.err.println(rowString + "; " + termString + "; " + matcher.start(1) + "; " + chars.length);
          e.printStackTrace();
        }
			}
		}
		return argumentsIterator;
	}

	private int processMathMode(ParserStateStack stateStack, ParserState state, SCEDocumentChar[] chars, int char_nr, int charCount, MathMode mathMode) {
		// if active math mode -> close; otherwise open
		byte style = LatexStyles.MATH;
		if (state instanceof MathMode) {
			if (mathMode.closes((MathMode) state)) {
				stateStack.pop();
			} else {
				style = LatexStyles.ERROR;
			}
		} else {
			if (mathMode.mayBeOpening()) {
				stateStack.push(mathMode);
			} else {
				style = LatexStyles.ERROR;
			}
		}
		char_nr = setStyle(charCount, style, chars, char_nr);
		return char_nr;
	}

	private void matchAndStyle(int start, SCEDocumentChar[] chars, String param, Pattern pattern, Function1<String, Byte> styleFunc) {
		Matcher matcher = pattern.matcher(param);
		int i = 0;
		while (matcher.find()) {
			setStyle(matcher.start() - i, LatexStyles.TEXT, chars, start + i);
			i = matcher.start();
			String group = matcher.group();
			setStyle(group, styleFunc.apply(group), chars, start + i);
			i += group.length();
		}
	}

	private CHArgumentType getArgumentType(Iterator<CHCommandArgument> argumentsIterator) {
		if (argumentsIterator != null && argumentsIterator.hasNext()) {
			CHCommandArgument argument = argumentsIterator.next();
			if (argument.isOptional() && argumentsIterator.hasNext()) {
				argument = argumentsIterator.next();
			}
			return argument.getType();
		}
		return null;
	}

	private int setStyle(String text, byte style, SCEDocumentChar[] chars, int offset) {
		return setStyle(text.length(), style, chars, offset);
	}

	private int setStyle(int count, byte style, SCEDocumentChar[] chars, int offset) {
		for (int i = 0; i < count; i++) {
		  chars[offset + i].style = style;
		}
		return offset + count - 1;
	}

	protected byte getStyle(String st, byte default_) {
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

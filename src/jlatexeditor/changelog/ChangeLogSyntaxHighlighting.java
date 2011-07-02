package jlatexeditor.changelog;

import jlatexeditor.syntaxhighlighting.states.RootState;
import sce.codehelper.StandalonePattern;
import sce.codehelper.WordWithPos;
import sce.component.*;
import sce.syntaxhighlighting.ParserStateStack;
import sce.syntaxhighlighting.SyntaxHighlighting;

import java.util.List;

/**
 * Syntax highlighting for CHANGELOG.
 */
public class ChangeLogSyntaxHighlighting extends SyntaxHighlighting implements SCEDocumentListener {
	private static final StandalonePattern linePattern = new StandalonePattern("^(.*)$");
	private static final StandalonePattern header1Pattern = new StandalonePattern("^(\"+)$");
	private static final StandalonePattern header2Pattern = new StandalonePattern("^(== )(.*)( ==)$");
	private static final StandalonePattern item1Pattern = new StandalonePattern("^(\\* )(.*)$");
	private static final StandalonePattern item2Pattern = new StandalonePattern("^(  - )(.*)$");
	private static final StandalonePattern item3Pattern = new StandalonePattern( "^(    \\* )(.*)$");
	private static final StandalonePattern viaPattern = new StandalonePattern("via ([^ ]+)");
	private static final StandalonePattern stringPattern = new StandalonePattern("[^\\w]\"([^\"]+)\"");
	private static final StandalonePattern commentPattern = new StandalonePattern("^(#.*)");

  // text pane and document
  private SCEPane pane = null;
  private SCEDocument document = null;

  // do we need to parse
  private boolean parseNeeded = false;
  private boolean currentlyChanging = false;

  public ChangeLogSyntaxHighlighting(SCEPane pane) {
	  super("ChangeLogSyntaxHighlighting");
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
    SCEDocumentRow rows[] = document.getRowsModel().getRows();

    // reset all states states and mark rows as modified
    for (SCEDocumentRow row : rows) {
      row.modified = true;
      row.parserStateStack = null;
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
    SCEDocumentRows rowsModel = document.getRowsModel();
    SCEDocumentRow rows[] = rowsModel.getRows();

    // find the rows that were modified since last parse
    for (int row_nr = 0; row_nr < rows.length; row_nr++) {
      SCEDocumentRow row = rows[row_nr];
      if (!row.modified) continue;
	    row.modified = false;

	    if (row_nr == 1) {
		    rowsModel.setStyle(ChangeLogStyles.HEADLINE1, row_nr, 0, row.length);
		    continue;
	    }

	    String rowString = row.toString();
	    List<WordWithPos> words;

	    boolean item = false;

	    if ((words = commentPattern.findInRow(rowString, row_nr)) != null) {
		    rowsModel.setStyle(ChangeLogStyles.COMMENT, words.get(0));
	    } else
	    if ((words = header1Pattern.findInRow(rowString, row_nr)) != null) {
		    rowsModel.setStyle(ChangeLogStyles.UNIMPORTANT, words.get(0));
	    } else
	    if ((words = header2Pattern.findInRow(rowString, row_nr)) != null) {
		    rowsModel.setStyle(ChangeLogStyles.UNIMPORTANT, words.get(0));
		    rowsModel.setStyle(ChangeLogStyles.HEADLINE2, words.get(1));
		    rowsModel.setStyle(ChangeLogStyles.UNIMPORTANT, words.get(2));
	    } else
	    if ((words = item1Pattern.findInRow(rowString, row_nr)) != null) {
		    rowsModel.setStyle(ChangeLogStyles.TEXT, words.get(0));
		    rowsModel.setStyle(ChangeLogStyles.ITEM1, words.get(1));
		    item = true;
	    } else
	    if ((words = item2Pattern.findInRow(rowString, row_nr)) != null) {
		    rowsModel.setStyle(ChangeLogStyles.TEXT, words.get(0));
		    rowsModel.setStyle(ChangeLogStyles.ITEM2, words.get(1));
		    item = true;
	    } else
	    if ((words = item3Pattern.findInRow(rowString, row_nr)) != null) {
		    rowsModel.setStyle(ChangeLogStyles.TEXT, words.get(0));
		    rowsModel.setStyle(ChangeLogStyles.ITEM3, words.get(1));
		    item = true;
	    } else
	    if ((words = linePattern.findInRow(rowString, row_nr)) != null) {
		    rowsModel.setStyle(ChangeLogStyles.TEXT, words.get(0));
	    }

	    if (item) {
		    for (List<WordWithPos> words2 : viaPattern.findAllInRow(rowString, row_nr)) {
			    rowsModel.setStyle(ChangeLogStyles.SHORTCUT, words2.get(0));
			    if (words2.size() > 1) {
				    rowsModel.setStyle(ChangeLogStyles.MENU, words2.get(2));
			    }
		    }
		    for (List<WordWithPos> words2 : stringPattern.findAllInRow(rowString, row_nr)) {
			    rowsModel.setStyle(ChangeLogStyles.STRING, words2.get(0));
		    }
	    }
    }
  }

  // SCEDocumentListener methods

  public void documentChanged(SCEDocument sender, SCEDocumentEvent event) {
    parseNeeded = true;
    currentlyChanging = true;
  }
}

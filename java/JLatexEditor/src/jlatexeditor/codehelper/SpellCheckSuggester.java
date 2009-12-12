package jlatexeditor.codehelper;

import jlatexeditor.syntaxhighlighting.LatexStyles;
import sce.codehelper.CodeAssistant;
import sce.codehelper.SCEPopup;
import sce.component.SCECaret;
import sce.component.SCEDocument;
import sce.component.SCEPane;
import util.Aspell;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Spell checker for the SourceCodeEditor.
 *
 * @author Stefan Endrullis
 */
public class SpellCheckSuggester implements CodeAssistant, SCEPopup.ItemHandler {
	/** Command has to start with a backslash and may only contain letters. */
	static final Pattern wordStartPattern = Pattern.compile("(\\w*)$");
	/** End of a command may end with an arbitrary number of letters. */
	static final Pattern wordEndPattern = Pattern.compile("^(\\w*)");

	static final Action addToDictionary = new Action("<add to dictionary>");

	/** Aspell wrapper. */
	private Aspell aspell = null;

	/** Last misspelled word ant its position in the document. */
	private WordWithPos misspelledWord = null;
	private SCEDocument document = null;


	public SpellCheckSuggester() throws IOException {
		aspell = Aspell.getInstance();
	}

	public boolean assistAt (SCEPane pane) {
		document = pane.getDocument();
		SCECaret caret = pane.getCaret();

		int row = caret.getRow();
		int column = caret.getColumn();

		// test if caret stands over a misspelled word
		boolean misspelled = document.getRows()[row].chars[column].overlayStyle == LatexStyles.U_MISSPELLED;
		if (!misspelled && column > 0) {
			misspelled = document.getRows()[row].chars[column-1].overlayStyle == LatexStyles.U_MISSPELLED;
		}

		if (misspelled) {
			// get the word under the caret
			misspelledWord = findWord(document.getRow(row), row, column);

			if (misspelledWord == null) return true;

			try {
				Aspell.Result aspellResult = aspell.check(misspelledWord.word);
				if (aspellResult.isCorrect()) return false;

				List<Object> list = new ArrayList<Object>();
				list.add(addToDictionary);
				for (String suggestion : aspellResult.getSuggestions()) {
					list.add(new Suggestion(suggestion));
				}

				pane.getPopup().openPopup(list, this);
			} catch (IOException e) {
				e.printStackTrace();
				return true;
			}
		}

		return false;
	}

	/**
	 * Searches for a command at the given position.
	 *
	 * @param line row content
	 * @param row row
	 * @param column column
	 * @return word
	 */
	public WordWithPos findWord(String line, int row, int column){
		Matcher startMatcher = wordStartPattern.matcher(line.substring(0, column));
		Matcher endMatcher = wordEndPattern.matcher(line.substring(column, line.length()));

		if (startMatcher.find() && endMatcher.find()) {
			return new WordWithPos(startMatcher.group(1) + endMatcher.group(1), row, startMatcher.start(1));
		}

		return null;
	}

	public void perform(Object item) {
		if (item instanceof Action) {
			aspell.addToPersonalDict(misspelledWord.word);
			// add and remove for reparsing
			document.remove(misspelledWord.row, misspelledWord.startColumn, misspelledWord.row, misspelledWord.endColumn);
			document.insert(misspelledWord.word, misspelledWord.row, misspelledWord.startColumn);
		} else
		if (item instanceof Suggestion) {
			Suggestion suggestion = (Suggestion) item;
			document.remove(misspelledWord.row, misspelledWord.startColumn, misspelledWord.row, misspelledWord.endColumn);
			document.insert(suggestion.word, misspelledWord.row, misspelledWord.startColumn);
		}
	}


// inner classes

	static class Action {
		String name;

		Action(String name) {
			this.name = name;
		}

		@Override
		public String toString() {
			return name;
		}
	}

	class Suggestion {
		String word;

		Suggestion(String word) {
			this.word = word;
		}

		@Override
		public String toString() {
			return word;
		}
	}
}

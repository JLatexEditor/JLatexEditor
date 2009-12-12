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
	static final Action removeFromDictionary = new Action("<remove from dictionary>");

	/** Aspell wrapper. */
	private Aspell aspell = null;

	/** Last misspelled word ant its position in the document. */
	private WordWithPos wordUnderCaret = null;
	private SCEDocument document = null;


	public SpellCheckSuggester() throws IOException {
		aspell = Aspell.getInstance();
	}

	public boolean assistAt (SCEPane pane) {
		document = pane.getDocument();
		SCECaret caret = pane.getCaret();

		int row = caret.getRow();
		int column = caret.getColumn();

		// get the word under the caret
		wordUnderCaret = findWord(document.getRow(row), row, column);

		if (wordUnderCaret == null) return false;

		try {
			Aspell.Result aspellResult = aspell.check(wordUnderCaret.word);

			if (aspellResult.isCorrect()) {
				if (aspell.getPersonalWords().contains(wordUnderCaret.word)) {
					List<Object> list = new ArrayList<Object>();
					list.add(removeFromDictionary);

					pane.getPopup().openPopup(list, this);
					return true;
				}
			} else {
				List<Object> list = new ArrayList<Object>();
				list.add(addToDictionary);
				for (String suggestion : aspellResult.getSuggestions()) {
					list.add(new Suggestion(suggestion));
				}

				pane.getPopup().openPopup(list, this);
				return true;
			}
		} catch (IOException e) {
			e.printStackTrace();
			return true;
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
			if (item == addToDictionary) {
				aspell.addToPersonalDict(wordUnderCaret.word);
				// replace the word for reparsing
				document.replace(wordUnderCaret.row, wordUnderCaret.startColumn, wordUnderCaret.row, wordUnderCaret.endColumn, wordUnderCaret.word);
			}
			if (item == removeFromDictionary) {
				try {
					aspell.removeFromPersonalDict(wordUnderCaret.word);
				} catch (IOException e) {
					e.printStackTrace();
				}
				// replace the word for reparsing
				document.replace(wordUnderCaret.row, wordUnderCaret.startColumn, wordUnderCaret.row, wordUnderCaret.endColumn, wordUnderCaret.word);
			}
		} else
		if (item instanceof Suggestion) {
			Suggestion suggestion = (Suggestion) item;
			document.replace(wordUnderCaret.row, wordUnderCaret.startColumn, wordUnderCaret.row, wordUnderCaret.endColumn, suggestion.word);
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

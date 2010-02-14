package jlatexeditor.codehelper;

import jlatexeditor.gproperties.GProperties;
import sce.codehelper.CodeAssistant;
import sce.codehelper.PatternPair;
import sce.codehelper.SCEPopup;
import sce.codehelper.WordWithPos;
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
	public static final PatternPair wordPattern = new PatternPair("([a-zA-ZäöüÄÖÜß]*)", "([a-zA-ZäöüÄÖÜß]*)");

	static final Action addToDictionary = new Action("<add to dictionary>");
	static final Action removeFromDictionary = new Action("<remove from dictionary>");

	/** Aspell wrapper. */
	private Aspell aspell = null;

	/** Last misspelled word ant its position in the document. */
	private WordWithPos wordUnderCaret = null;
	private SCEDocument document = null;

	public SpellCheckSuggester() throws Exception {
		aspell = Aspell.getInstance(GProperties.getAspellLang());
    if(aspell == null) throw new Exception("Initialization of the spell check suggester failed!");
	}

	public boolean assistAt (SCEPane pane) {
		document = pane.getDocument();

		// get the word under the caret
		List<WordWithPos> wordList = wordPattern.find(pane);

		if (wordList == null) return false;

		wordUnderCaret = wordList.get(0);

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

package jlatexeditor.codehelper;

import jlatexeditor.syntaxhighlighting.LatexStyles;
import sce.codehelper.CodeAssistant;
import sce.component.SCECaret;
import sce.component.SCEDocument;
import sce.component.SCEPane;
import util.Aspell;

import java.io.IOException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Spell checker for the SourceCodeEditor.
 */
public class SpellCheckSuggester implements CodeAssistant {
	/** Command has to start with a backslash and may only contain letters. */
	static final Pattern wordStartPattern = Pattern.compile("(\\w*)$");
	/** End of a command may end with an arbitrary number of letters. */
	static final Pattern wordEndPattern = Pattern.compile("^(\\w*)");

	/** Aspell wrapper. */
	private Aspell aspell;

	public SpellCheckSuggester() throws IOException {
		aspell = new Aspell();
	}

	public boolean assistAt(SCEPane pane) {
		SCEDocument document = pane.getDocument();
		SCECaret caret = pane.getCaret();

		int row = caret.getRow();
		int column = caret.getColumn();

		// test if caret stands over a misspelled word
		if (document.getRows()[row].chars[column].overlayStyle == LatexStyles.U_MISSPELLED) {
			// get the word under the caret
			WordWithPos wordWithPos = findWord(document.getRow(row), column);

			if (wordWithPos == null) return false;
		}

		return false;
	}

	/**
	 * Searches for a command at the given position.
	 *
	 * @param line row content
	 * @param column column
	 * @return word
	 */
	public WordWithPos findWord(String line, int column){
		Matcher startMatcher = wordStartPattern.matcher(line.substring(0, column));
		Matcher endMatcher = wordEndPattern.matcher(line.substring(column, line.length()));

		if (startMatcher.find() && endMatcher.find()) {
			return new WordWithPos(startMatcher.group(1) + endMatcher.group(1), startMatcher.start(1));
		}

		return null;
	}

	class WordWithPos {
		String word;
		int startColumn;
		int endColumn;

		WordWithPos(String word, int startColumn) {
			this.word = word;
			this.startColumn = startColumn;
			this.endColumn = startColumn + word.length();
		}
	}
}

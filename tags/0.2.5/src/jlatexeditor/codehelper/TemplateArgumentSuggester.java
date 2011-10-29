package jlatexeditor.codehelper;

import jlatexeditor.gui.TemplateEditor;
import sce.codehelper.CodeAssistant;
import sce.codehelper.PatternPair;
import sce.codehelper.SCEPopup;
import sce.codehelper.WordWithPos;
import sce.component.SCEPane;

import java.util.ArrayList;
import java.util.List;

/**
 * Suggester to add and remove template arguments in template editor.
 *
 * @author Stefan Endrullis
 */
public class TemplateArgumentSuggester implements CodeAssistant, SCEPopup.ItemHandler {
	public static final PatternPair argPattern = new PatternPair("@([^@]*)", "([^@]*)@");
	private static final String addTemplateArgument    = "add template argument";
	private static final String removeTemplateArgument = "remove template argument";
	private static final String deriveTemplateArgument = "derive another argument from this argument";

	/** Last misspelled word ant its position in the document. */
	private WordWithPos wordUnderCaret = null;
	private TemplateEditor templateEditor;

	public TemplateArgumentSuggester(TemplateEditor templateEditor) {
		this.templateEditor = templateEditor;
	}

	public boolean assistAt(SCEPane pane) {
	  // get the word under the caret
	  List<WordWithPos> wordList = argPattern.find(pane);

		if (wordList == null) return false;

	  wordUnderCaret = wordList.get(0);
		String argument = wordUnderCaret.word;
	  if(argument.length() == 0) return false;

		ArrayList<Object> suggestionList = new ArrayList<Object>();
		if (templateEditor.hasTemplateArgument(argument)) {
			suggestionList.add(deriveTemplateArgument);
			suggestionList.add(removeTemplateArgument);
		} else {
			suggestionList.add(addTemplateArgument);
		}

		pane.getPopup().openPopup(suggestionList, this);

		return true;
	}

	public void perform(Object item) {
		if (item == addTemplateArgument) {
			templateEditor.addTemplateArgument(wordUnderCaret.word);
		} else
		if (item == removeTemplateArgument) {
			templateEditor.removeTemplateArgument(wordUnderCaret.word);
		} else
		if (item == deriveTemplateArgument) {
			templateEditor.askForDerivedArgumentValue(wordUnderCaret.word);
		}
	}
}

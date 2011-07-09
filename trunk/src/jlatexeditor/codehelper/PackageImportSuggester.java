package jlatexeditor.codehelper;

import sce.codehelper.CodeAssistant;
import sce.codehelper.PatternPair;
import sce.codehelper.WordWithPos;
import sce.component.SCEDocument;
import sce.component.SCEPane;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * @author Stefan Endrullis &lt;stefan@endrullis.de&gt;
 */
public class PackageImportSuggester implements CodeAssistant {
	public static final PatternPair wordPattern = new PatternPair("\\(\\p{L}*)", "(\\p{L}*)");

	static final Action addToDictionary = new Action("<add to dictionary>");
	static final Action removeFromDictionary = new Action("<remove from dictionary>");

	/** Last misspelled word ant its position in the document. */
	private WordWithPos wordUnderCaret = null;
	private SCEDocument document = null;

	public PackageImportSuggester() {
	}

	public boolean assistAt(SCEPane pane) {
	  document = pane.getDocument();

	  // get the word under the caret
	  List<WordWithPos> wordList = wordPattern.find(pane);

	  if (wordList == null) return false;

	  wordUnderCaret = wordList.get(0);
	  if(wordUnderCaret.word.length() == 0) return false;
	  if(!Character.isLetter(wordUnderCaret.word.charAt(0))) return false;

		/*
	  try {
	    SpellChecker.Result spellCheckResult = spellChecker.check(wordUnderCaret.word);

	    if (spellCheckResult.isCorrect()) {
	      if (spellChecker.getPersonalWords().contains(wordUnderCaret.word)) {
	        List<Object> list = new ArrayList<Object>();
	        list.add(removeFromDictionary);

	        pane.getPopup().openPopup(list, this);
	        return true;
	      }
	    } else {
	      List<Object> list = new ArrayList<Object>();
	      list.add(addToDictionary);
	      for (String suggestion : spellCheckResult.getSuggestions()) {
	        list.add(new Suggestion(suggestion));
	      }

	      pane.getPopup().openPopup(list, this);
	      return true;
	    }
	  } catch (IOException e) {
	    e.printStackTrace();
	    return true;
	  }
	  */

	  return false;
	}

	public void perform(Object item) {
	  if (item instanceof Action) {
	    if (item == addToDictionary) {
	    }
	    if (item == removeFromDictionary) {
	    }
	  } else if (item instanceof Suggestion) {
	    Suggestion suggestion = (Suggestion) item;
	    document.replace(wordUnderCaret.getStartPos(), wordUnderCaret.getEndPos(), suggestion.word);
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

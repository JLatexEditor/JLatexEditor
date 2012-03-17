package jlatexeditor.codehelper;

import de.endrullis.utils.LazyVal;
import jlatexeditor.PackagesExtractor;
import sce.codehelper.CHCommand;
import sce.codehelper.PatternPair;
import sce.codehelper.WordWithPos;
import util.Function1;
import util.SimpleTrie;

import java.util.HashMap;

/**
 * Static completion for 1st parameter of given command.
 *
 * @author Stefan Endrullis &lt;stefan@endrullis.de&gt;
 */
public class ValueListCompletion extends PatternCompletion {
  protected WordWithPos word;
	protected HashMap<String, LazyVal<SimpleTrie<String>>> command2trie = new HashMap<String, LazyVal<SimpleTrie<String>>>();
	protected LazyVal<SimpleTrie<String>> availableValues;

	public ValueListCompletion(HashMap<String, LazyVal<SimpleTrie<String>>> command2trie) {
		pattern = new PatternPair("\\\\(\\w+)(?:\\[.*\\])?\\{([^{},]+,)*([^{},]*)");
		this.command2trie = command2trie;
	}

	/*
	public ValueListCompletion(String cmdName, LazyVal<SimpleTrie<String>> availableValues) {
    pattern = new PatternPair("\\\\" + cmdName + "(?:\\[.*\\])?\\{([^{},]+,)*([^{},]*)");
		this.availableValues = availableValues;
	}
	*/

  @Override
  public boolean matches() {
    if (super.matches()) {
	    String command = params.get(0).word;
	    if (command2trie.keySet().contains(command)) {
		    availableValues = command2trie.get(command);
		    word = params.get(2);
		    return true;
	    }
    }
    return false;
  }

  @Override
  public WordWithPos getWordToReplace() {
    return word;
  }

  @Override
  public Iterable<? extends CHCommand> getCompletions(int level) {
    return getCompletions(word.word);
  }

  @Override
  public String getMaxCommonPrefix() {
    return getMaxCommonPrefix(word.word);
  }

  public Iterable<CHCommand> getCompletions(String search) {
    return availableValues.getValue().getObjectsIterable(search).map(new Function1<String, CHCommand>() {
	    @Override
	    public CHCommand apply(String a1) { return new ValueCompletion(a1); }
    });
  }

  public String getMaxCommonPrefix(String search) {
    String prefix = availableValues.getValue().getMaxCommonPrefix(search);
	  if (prefix.length() < search.length()) {
		  prefix = search;
	  }
	  return prefix;
  }
}

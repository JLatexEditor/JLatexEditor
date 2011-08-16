package jlatexeditor.codehelper;

import sce.codehelper.CHCommand;
import sce.codehelper.PatternPair;
import sce.codehelper.WordWithPos;
import util.Trie;
import util.Function0;

import java.util.ArrayList;
import java.util.List;

public class GenericCodeHelper extends PatternHelper {
  protected WordWithPos label;
	protected Function0<Trie<?>> getCompletionTrie;

  public GenericCodeHelper(String pattern, Function0<Trie<?>> getCompletionTrie) {
    this.pattern = new PatternPair(pattern);
	  this.getCompletionTrie = getCompletionTrie;
  }

  public boolean matches() {
    if (super.matches()) {
      label = params.get(0);
      return true;
    }
    return false;
  }

  public WordWithPos getWordToReplace() {
    return label;
  }

  public Iterable<? extends CHCommand> getCompletions(int level) {
    Trie<?> trie = getCompletionTrie.apply();

    ArrayList<CHCommand> commands = new ArrayList<CHCommand>();
    List<String> strings = trie.getStrings(label.word, 100);

    for (String string : strings) commands.add(new CHCommand(string));

    return commands;
  }

  public String getMaxCommonPrefix() {
	  Trie<?> trie = getCompletionTrie.apply();
    return trie.getMaxCommonPrefix(label.word);
  }
}

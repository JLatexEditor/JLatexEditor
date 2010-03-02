package jlatexeditor.codehelper;

import sce.codehelper.CHCommand;
import sce.codehelper.PatternPair;
import sce.codehelper.WordWithPos;
import util.Trie;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Stefan Endrullis &lt;stefan@endrullis.de&gt;
 */
public class WordCompletion extends PatternHelper {
  private BackgroundParser backgroundParser;
  private WordWithPos word;

  public WordCompletion(BackgroundParser backgroundParser) {
    this.backgroundParser = backgroundParser;
    pattern = SpellCheckSuggester.wordPattern;
  }

  @Override
  public boolean matches() {
    super.matches();
    if (params != null) {
      word = params.get(0);
      return true;
    }
    return false;
  }

  @Override
  public WordWithPos getWordToReplace() {
    return word;
  }

  @Override
  public Iterable<? extends CHCommand> getCompletions() {
    ArrayList<CHCommand> commands = new ArrayList<CHCommand>();
    List<String> strings = backgroundParser.getWords().getStrings(word.word, 100);
    if (strings == null) return commands;

    for (String string : strings) commands.add(new CHCommand(string));

    return commands;
  }

  @Override
  public String getMaxCommonPrefix() {
    return backgroundParser.getWords().getMaxCommonPrefix(word.word);
  }
}

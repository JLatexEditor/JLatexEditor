package jlatexeditor.codehelper;

import sce.codehelper.CHCommand;
import sce.codehelper.WordWithPos;

import java.util.ArrayList;
import java.util.List;

/**
 * Word completion.
 *
 * @author Stefan Endrullis &lt;stefan@endrullis.de&gt;
 */
public class WordCompletion extends PatternCompletion {
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
      return word.word.length() > 0;
    }
    return false;
  }

  @Override
  public WordWithPos getWordToReplace() {
    return word;
  }

  @Override
  public Iterable<? extends CHCommand> getCompletions(int level) {
    ArrayList<CHCommand> commands = new ArrayList<CHCommand>();
    List<String> strings = backgroundParser.getWords().getStrings(word.word, 100);

    for (String string : strings) commands.add(new CHCommand(string));

    return commands;
  }

  @Override
  public String getMaxCommonPrefix() {
    String prefix = backgroundParser.getWords().getMaxCommonPrefix(word.word);
	  if (prefix.length() < word.word.length()) {
		  prefix = word.word;
	  }
	  return prefix;
  }
}

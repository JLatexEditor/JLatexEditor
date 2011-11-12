package jlatexeditor.codehelper;

import sce.codehelper.CHCommand;
import sce.codehelper.PatternPair;
import sce.codehelper.WordWithPos;

public class CiteCompletion extends PatternCompletion {
  private BackgroundParser backgroundParser;
  protected WordWithPos word;

  public CiteCompletion(BackgroundParser backgroundParser) {
    this.backgroundParser = backgroundParser;
    pattern = new PatternPair("\\\\(?:no)?cite(?:\\[.*\\])?\\{([^{},]+,)*([^{},]*)");
  }

  @Override
  public boolean matches() {
    if (super.matches()) {
      word = params.get(1);
      return true;
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

  public Iterable<BibEntry> getCompletions(String search) {
    return backgroundParser.getBibEntries(search);
  }

  public String getMaxCommonPrefix(String search) {
    return search;
  }
}

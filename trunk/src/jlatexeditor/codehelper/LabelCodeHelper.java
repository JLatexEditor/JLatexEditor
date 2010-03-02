package jlatexeditor.codehelper;

import sce.codehelper.CHCommand;
import sce.codehelper.PatternPair;
import sce.codehelper.WordWithPos;
import util.Trie;

import java.util.ArrayList;
import java.util.List;

public class LabelCodeHelper extends PatternHelper {
  protected WordWithPos label;
  protected BackgroundParser backgroundParser;

  public LabelCodeHelper(BackgroundParser backgroundParser) {
    this.backgroundParser = backgroundParser;
    pattern = new PatternPair("\\\\(ref|eqref)\\{([^{}]*)");
  }

  public boolean matches() {
    if (super.matches()) {
      label = params.get(1);
      return true;
    }
    return false;
  }

  public WordWithPos getWordToReplace() {
    return label;
  }

  public Iterable<? extends CHCommand> getCompletions() {
    Trie labels = backgroundParser.getLabels();

    ArrayList<CHCommand> commands = new ArrayList<CHCommand>();
    List<String> strings = labels.getStrings(label.word, 100);
    if (strings == null) return commands;

    for (String string : strings) commands.add(new CHCommand(string));

    return commands;
  }

  public String getMaxCommonPrefix() {
    Trie labels = backgroundParser.getLabels();
    return labels.getMaxCommonPrefix(label.word);
  }
}
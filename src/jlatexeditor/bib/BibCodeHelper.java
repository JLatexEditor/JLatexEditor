package jlatexeditor.bib;

import jlatexeditor.codehelper.PatternHelper;
import sce.codehelper.CHCommand;
import sce.codehelper.PatternPair;
import sce.codehelper.WordWithPos;
import sce.syntaxhighlighting.ParserState;
import sce.syntaxhighlighting.ParserStateStack;

import java.util.ArrayList;

public class BibCodeHelper extends PatternHelper {
  protected WordWithPos name;

  public BibCodeHelper() {
    pattern = new PatternPair("^(@\\w*)");
  }

  public boolean matches() {
    if (super.matches()) {
      name = params.get(0);
      return true;
    }
    int row = pane.getCaret().getRow();
    int column = pane.getCaret().getColumn();
    if (column == 0) {
      ParserStateStack stateStack = BibSyntaxHighlighting.parseRow(pane.getDocument().getRows()[row], column);
      BibParserState state = (BibParserState) stateStack.peek();
      if(state.getState() == BibParserState.STATE_NOTHING) return true;
    }
    return false;
  }

  public WordWithPos getWordToReplace() {
    return name;
  }

  public Iterable<? extends CHCommand> getCompletions() {
    return getCompletions(name.word);
  }

  public String getMaxCommonPrefix() {
    return getMaxCommonPrefix(name.word);
  }

  public Iterable<? extends CHCommand> getCompletions(String name) {
    ArrayList<CHCommand> list = new ArrayList<CHCommand>();

    for(BibEntry entry : BibEntry.ENTRIES) {
      if(entry.getName().startsWith(name.toLowerCase())) list.add(entry);
    }

    return list;
  }

  /**
   * Searches for the best completion of the prefix.
   * @return the completion suggestion (without the prefix)
   */
  public String getMaxCommonPrefix(String name) {
    int prefixLength = name.length();
    String completion = null;

    for (CHCommand command : getCompletions(name)) {
      String commandName = command.getName();
      if (commandName.startsWith(name)) {
        if (completion == null) {
          completion = commandName;
        } else {
          // find the common characters
          int commonIndex = prefixLength;
          int commonLength = Math.min(completion.length(), commandName.length());
          while (commonIndex < commonLength) {
            if (completion.charAt(commonIndex) != commandName.charAt(commonIndex)) break;
            commonIndex++;
          }
          completion = completion.substring(0, commonIndex);
        }
      }
    }

    return completion;
  }
}
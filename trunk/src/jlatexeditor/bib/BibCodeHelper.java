package jlatexeditor.bib;

import jlatexeditor.codehelper.PatternHelper;
import sce.codehelper.CHCommand;
import sce.codehelper.PatternPair;
import sce.codehelper.WordWithPos;
import sce.component.SCEDocumentRow;
import sce.syntaxhighlighting.ParserStateStack;

import java.util.ArrayList;
import java.util.Arrays;

public class BibCodeHelper extends PatternHelper {
  protected WordWithPos name;
  protected ArrayList<CHCommand> missingParams = null;

  public BibCodeHelper() {
    pattern = new PatternPair("^(@\\w*)");
  }

  public boolean matches() {
    missingParams = null;

    if (super.matches()) {
      name = params.get(0);
      return true;
    }

    SCEDocumentRow[] rows = pane.getDocument().getRows();
    int row = pane.getCaret().getRow();
    int column = pane.getCaret().getColumn();

    ParserStateStack stateStack = BibSyntaxHighlighting.parseRow(rows[row], column);
    BibParserState state = (BibParserState) stateStack.peek();

    if(column == 0 && state.getState() == BibParserState.STATE_NOTHING) {
      name = new WordWithPos("", row, 0);
      return true;
    }

    if(state.getState() == BibParserState.STATE_EXPECT_KEY || state.getState() == BibParserState.STATE_EXPECT_EQ) {
      missingParams = new ArrayList<CHCommand>();

      ArrayList<String> keys = state.getAllKeys();
      BibEntry entry = BibEntry.getEntry("@" + state.getEntryType());
      if(entry == null) return false;

      ArrayList<String> params = new ArrayList<String>();
      params.addAll(Arrays.asList(entry.getRequired()));
      params.addAll(Arrays.asList(entry.getOptional()));
      for(String param : params) {
        if(!keys.contains(param)) {
          CHCommand command = new CHCommand(param);
          command.setUsage(param + " = {@|@},");
          missingParams.add(command);
        }
      }

      name = new WordWithPos("", row, column);
      return true;
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
    name = name.toLowerCase();

    ArrayList<CHCommand> list = new ArrayList<CHCommand>();

    if(missingParams == null) {
      // entry completion
      for(BibEntry entry : BibEntry.ENTRIES) {
        if(entry.getName().startsWith(name.toLowerCase())) list.add(entry);
      }
    } else {
      // param completion
      for(CHCommand param : missingParams) {
        if(param.getName().startsWith(name.toLowerCase())) list.add(param);
      }
    }

    return list;
  }

  /**
   * Searches for the best completion of the prefix.
   * @return the completion suggestion (without the prefix)
   */
  public String getMaxCommonPrefix(String name) {
    name = name.toLowerCase();

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
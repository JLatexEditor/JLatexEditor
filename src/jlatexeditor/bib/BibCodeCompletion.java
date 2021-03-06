package jlatexeditor.bib;

import jlatexeditor.codehelper.PatternCompletion;
import sce.codehelper.CHCommand;
import sce.codehelper.PatternPair;
import sce.codehelper.WordWithPos;
import sce.component.SCEDocumentRow;
import sce.syntaxhighlighting.ParserStateStack;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;

public class BibCodeCompletion extends PatternCompletion {
  protected WordWithPos name;
  protected ArrayList<CHCommand> missingParams = null;

  protected PatternPair keyPattern = new PatternPair("(\\w*)");
  public BibCodeCompletion() {
    pattern = new PatternPair("^(@\\w*)");
  }

  public boolean matches() {
    missingParams = null;

    if (super.matches()) {
      name = params.get(0);
      return true;
    }

    SCEDocumentRow[] rows = pane.getDocument().getRowsModel().getRows();
    int row = pane.getCaret().getRow();
    int column = pane.getCaret().getColumn();

    ParserStateStack stateStack = BibSyntaxHighlighting.parseRow(rows[row], column, document, rows, true);
    BibParserState state = (BibParserState) stateStack.peek();

    if(column == 0 && state.getState() == BibParserState.STATE_NOTHING) {
      name = new WordWithPos("", row, 0);
      return true;
    }

    if(state.getState() == BibParserState.STATE_EXPECT_KEY || state.getState() == BibParserState.STATE_EXPECT_EQ) {
      missingParams = new ArrayList<CHCommand>();

      params = keyPattern.find(pane);
      if(params != null) {
        name = params.get(0);
      } else {
        name = new WordWithPos("", row, column);
      }

      HashMap<String,BibKeyValuePair> keys = state.getEntry().getAllParameters();
      BibEntryPattern entry = BibEntryPattern.getEntry("@" + state.getEntry().getType(false));
      if(entry == null) return false;

      ArrayList<String> bibKeys = new ArrayList<String>();
      bibKeys.addAll(Arrays.asList(entry.getRequired()));
      bibKeys.addAll(Arrays.asList(entry.getOptional()));
      for(String bibKey : bibKeys) {
        if(!keys.containsKey(bibKey)) {
          CHCommand command = new CHCommand(bibKey);
          command.setUsage(bibKey + " = {@|@},");
          missingParams.add(command);
        }
      }

      return true;
    }

    return false;
  }

  public WordWithPos getWordToReplace() {
    return name;
  }

  public Iterable<? extends CHCommand> getCompletions(int level) {
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
      for(BibEntryPattern entry : BibEntryPattern.ENTRIES) {
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
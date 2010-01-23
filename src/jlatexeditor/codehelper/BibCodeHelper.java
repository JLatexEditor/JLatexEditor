package jlatexeditor.codehelper;

import sce.codehelper.CHCommand;
import sce.codehelper.CodeHelper;

public class BibCodeHelper extends CodeHelper {
  private BackgroundParser backgroundParser;

  public BibCodeHelper(BackgroundParser backgroundParser) {
    this.backgroundParser = backgroundParser;
  }

  public Iterable<BibEntry> getCommands(String search) {
    return backgroundParser.getBibEntries(search);
  }

  public String getCompletion(String search) {
    return null;
  }
}

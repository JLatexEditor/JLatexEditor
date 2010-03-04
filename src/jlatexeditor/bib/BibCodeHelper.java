package jlatexeditor.bib;

import de.endrullis.utils.BetterProperties2.PSet;
import de.endrullis.utils.BetterProperties2.Range;
import jlatexeditor.codehelper.PatternHelper;
import jlatexeditor.gproperties.GProperties;
import sce.codehelper.CHCommand;
import sce.codehelper.PatternPair;
import sce.codehelper.WordWithPos;

import java.util.ArrayList;

public class BibCodeHelper extends PatternHelper {
  protected String key;
  protected WordWithPos value;
  protected Range range;

  public BibCodeHelper() {
    pattern = new PatternPair("^([^#=]+)=([^#]*)");
  }

  @Override
  public boolean matches() {
    if (super.matches()) {
      key = params.get(0).word;
      value = params.get(1);

      range = GProperties.getRange(key);
      return true;
    }
    return false;
  }

  @Override
  public WordWithPos getWordToReplace() {
    return value;
  }

  @Override
  public Iterable<? extends CHCommand> getCompletions() {
    return getCompletions(key, value.word);
  }

  @Override
  public String getMaxCommonPrefix() {
    return getMaxCommonPrefix(key, value.word);
  }

  public Iterable<ValueCompletion> getCompletions(String key, final String prefix) {
    ArrayList<ValueCompletion> list = new ArrayList<ValueCompletion>();

    if (range instanceof PSet) {
      PSet set = (PSet) range;
      for (String value : set.content) {
        if (value.startsWith(prefix)) {
          list.add(new ValueCompletion(value));
        }
      }
    } else {
      list.add(new ValueCompletion("<" + range.description() + ">"));
    }

    return list;
  }

  /**
   * Searches for the best completion of the prefix.
   *
   * @param key    property name
   * @param prefix filename
   * @return the completion suggestion (without the prefix)
   */
  public String getMaxCommonPrefix(String key, String prefix) {
    if (range instanceof PSet) {
      int prefixLength = prefix.length();
      String completion = null;

      for (CHCommand command : getCompletions(key, prefix)) {
        String commandName = command.getName();
        if (commandName.startsWith(prefix)) {
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
    } else {
      return prefix;
    }
  }

  public static class ValueCompletion extends CHCommand {
    /**
     * Creates a command with the given name.
     *
     * @param name the name
     */
    public ValueCompletion(String name) {
      super(name);
    }
	}
}
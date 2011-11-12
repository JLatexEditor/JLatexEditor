package jlatexeditor.codehelper;

import jlatexeditor.PackagesExtractor;
import sce.codehelper.CHCommand;
import sce.codehelper.PatternPair;
import sce.codehelper.WordWithPos;
import util.Function1;

/**
 * Completion for \bibliographystyle.
 *
 * @author Stefan Endrullis &lt;stefan@endrullis.de&gt;
 */
public class BibStyleCompletion extends PatternCompletion {
  protected WordWithPos word;

  public BibStyleCompletion() {
    pattern = new PatternPair("\\\\bibliographystyle(?:\\[.*\\])?\\{([^{},]+,)*([^{},]*)");
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

  public Iterable<CHCommand> getCompletions(String search) {
    return PackagesExtractor.getBibStyles().getObjectsIterable(search).map(new Function1<String, CHCommand>() {
	    @Override
	    public CHCommand apply(String a1) { return new ValueCompletion(a1); }
    });
  }

  public String getMaxCommonPrefix(String search) {
    String prefix = PackagesExtractor.getBibStyles().getMaxCommonPrefix(search);
	  if (prefix.length() < search.length()) {
		  prefix = search;
	  }
	  return prefix;
  }
}

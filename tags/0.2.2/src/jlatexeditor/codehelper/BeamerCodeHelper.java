package jlatexeditor.codehelper;

import de.endrullis.utils.collections.CollectionUtils;
import jlatexeditor.PackagesExtractor;
import sce.codehelper.CHCommand;
import sce.codehelper.PatternPair;
import sce.codehelper.WordWithPos;
import util.Function1;

import java.util.ArrayList;
import java.util.List;

/**
 * CodeHelper for LaTeX beamer commands.
 *
 * @author Stefan Endrullis &lt;stefan@endrullis.de&gt;
 */
public class BeamerCodeHelper extends PatternHelper {
	protected String type;
	protected WordWithPos word;

	public BeamerCodeHelper() {
		pattern = new PatternPair("\\\\use(theme|colortheme|fonttheme|innertheme|outertheme)(?:\\[[^\\]]*\\])?\\{([^{}]*)");
	}

	@Override
	public boolean matches() {
	  if (super.matches()) {
		  type = params.get(0).word;
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
		List<String> values = PackagesExtractor.getPackageParser().getPackages().getStrings("beamer" + type + search, 20);
		if (values == null) values = new ArrayList<String>();

		return CollectionUtils.map(values, new Function1<String, CHCommand>() {
			public CHCommand apply(String value) {
				return new ValueCompletion(value.substring(("beamer" + type).length()));
			}
		});
	}

	public String getMaxCommonPrefix(String search) {
	  return search;
	}
}

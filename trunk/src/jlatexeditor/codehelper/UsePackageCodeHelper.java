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
 * CodeHelper for \\usepackage{...}.
 *
 * @author Stefan Endrullis &lt;stefan@endrullis.de&gt;
 */
public class UsePackageCodeHelper extends PatternHelper {
	protected WordWithPos word;

	public UsePackageCodeHelper() {
		pattern = new PatternPair("\\\\usepackage(?:\\[[^\\]]*\\])?\\{([^{},]+,)*([^{},]*)");
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
	public Iterable<? extends CHCommand> getCompletions() {
	  return getCompletions(word.word);
	}

	@Override
	public String getMaxCommonPrefix() {
	  return getMaxCommonPrefix(word.word);
	}

	public Iterable<CHCommand> getCompletions(String search) {
		List<String> packageNames = PackagesExtractor.getPackageParser().getPackages().getStrings(search, 20);
		if (packageNames == null) packageNames = new ArrayList<String>();

		return CollectionUtils.map(packageNames, new Function1<String, CHCommand>() {
			public CHCommand apply(String packageName) {
				return new ValueCompletion(packageName);
			}
		});
	}

	public String getMaxCommonPrefix(String search) {
	  return search;
	}
}

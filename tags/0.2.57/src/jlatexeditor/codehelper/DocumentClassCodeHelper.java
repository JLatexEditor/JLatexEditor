package jlatexeditor.codehelper;

import de.endrullis.utils.CollectionUtils;
import jlatexeditor.PackagesExtractor;
import jlatexeditor.gproperties.GPropertiesCodeHelper;
import sce.codehelper.CHCommand;
import sce.codehelper.PatternPair;
import sce.codehelper.WordWithPos;
import util.Function1;

import java.util.ArrayList;
import java.util.List;

/**
 * CodeHelper for \\documentclass{...}.
 *
 * @author Stefan Endrullis &lt;stefan@endrullis.de&gt;
 */
public class DocumentClassCodeHelper extends PatternHelper {
	protected WordWithPos word;

	public DocumentClassCodeHelper() {
		pattern = new PatternPair("\\\\documentclass(?:\\[[^\\]]*\\])?\\{([^{}]*)");
	}

	@Override
	public boolean matches() {
	  if (super.matches()) {
	    word = params.get(0);
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
		List<String> docClassNames = PackagesExtractor.getDocClassesParser().getPackages().getStrings(search, 20);
		if (docClassNames == null) docClassNames = new ArrayList<String>();

		return CollectionUtils.map(docClassNames, new Function1<String, CHCommand>() {
			public CHCommand apply(String docClassName) { return new GPropertiesCodeHelper.ValueCompletion(docClassName); }
		});
	}

	public String getMaxCommonPrefix(String search) {
	  return search;
	}
}

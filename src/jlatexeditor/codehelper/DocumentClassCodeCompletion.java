package jlatexeditor.codehelper;

import de.endrullis.utils.collections.CollectionUtils;
import jlatexeditor.PackagesExtractor;
import sce.codehelper.CHCommand;
import sce.codehelper.PatternPair;
import util.Function1;

import java.util.ArrayList;
import java.util.List;

/**
 * CodeCompletion for \\documentclass{...}.
 *
 * @author Stefan Endrullis &lt;stefan@endrullis.de&gt;
 */
public class DocumentClassCodeCompletion extends ExtPatternCompletion<PackagesExtractor.Package> {
	private static final Function1<PackagesExtractor.Package, String> PACKAGE_2_NAME_FUNCTION = new Function1<PackagesExtractor.Package, String>() {
		@Override
		public String apply(PackagesExtractor.Package pack) {
			return pack.getName();
		}
	};

	public DocumentClassCodeCompletion() {
		super("documentclasses");
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

	public Iterable<CHCommand> getCompletions(String search, Function1<PackagesExtractor.Package, Boolean> filterFun) {
		List<String> docClassNames = PackagesExtractor.getDocClassesParser().getPackages().getObjectsIterable(search).filter(filterFun).map(PACKAGE_2_NAME_FUNCTION).toList(20);
		if (docClassNames == null) docClassNames = new ArrayList<String>();

		return CollectionUtils.map(docClassNames, new Function1<String, CHCommand>() {
			public CHCommand apply(String docClassName) { return new ValueCompletion(docClassName); }
		});
	}

	protected Function1<PackagesExtractor.Package, Boolean> minUsage(final int minUsageCount) {
		return new Function1<PackagesExtractor.Package, Boolean>() {
			@Override
			public Boolean apply(PackagesExtractor.Package pack) {
				return pack.getUsageCount() >= minUsageCount;
			}
		};
	}

	public String getMaxCommonPrefix(String search) {
		String maxCommonPrefix = PackagesExtractor.getPackageParser().getPackages().getMaxCommonPrefix(search);
		if (maxCommonPrefix.length() > search.length()) {
			return maxCommonPrefix;
		}
		return search;
	}
}
